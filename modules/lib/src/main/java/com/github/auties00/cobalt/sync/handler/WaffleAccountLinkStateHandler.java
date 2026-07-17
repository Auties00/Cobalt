package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.send.id.MessageIdGenerator;
import com.github.auties00.cobalt.message.send.id.MessageIdVersion;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestType;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.WaffleAccountLinkStateAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wire.wam.event.NonMessagePeerDataRequestEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WaffleCompanionStateLifecycleEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.PeerDataRequestType;
import com.github.auties00.cobalt.wire.wam.type.WaffleLifecycleLinkStateType;
import com.github.auties00.cobalt.wire.wam.type.WaffleLifecycleTraceActionType;
import com.github.auties00.cobalt.wire.wam.type.WaffleLifecycleTraceSourceType;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mirrors the WAFFLE (WhatsApp - Meta Account) account-linking state and triggers a primary-device
 * nonce fetch when the link becomes active.
 *
 * <p>The sync dispatcher routes incoming {@code waffle_account_link_state} mutations here whenever
 * the user's Meta-account linking state changes on another linked device (typical trigger: linking
 * or unlinking the WhatsApp account to a Meta account from the phone). The handler persists the
 * {@link WaffleAccountLinkStateAction.AccountLinkState} on the store and, when the state becomes
 * {@link WaffleAccountLinkStateAction.AccountLinkState#ACTIVE}, sends a
 * {@link PeerDataOperationRequestType#WAFFLE_LINKING_NONCE_FETCH} peer message so the primary device
 * delivers a fresh linking nonce.
 */
@WhatsAppWebModule(moduleName = "WAWebWaffleAccountLinkStateSync")
public final class WaffleAccountLinkStateHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link WaffleAccountLinkStateHandler}.
     */
    private static final System.Logger LOGGER = Log.get(WaffleAccountLinkStateHandler.class);

    /**
     * The AB-props service consulted on every mutation to enforce the {@link ABProp#WEB_WAFFLE}
     * gate.
     */
    private final ABPropsService abPropsService;

    /**
     * The WAM service used to commit the peer-data-request telemetry event when triggering a
     * linking nonce fetch.
     */
    private final WamService wamService;

    /**
     * Constructs the handler with its injected dependencies.
     *
     * <p>The handler is stateful only through the injected {@link ABPropsService} and
     * {@link WamService}; Cobalt's sync registry holds a single instance per client.
     *
     * @param abPropsService the AB-props service consulted on every mutation
     * @param wamService     the WAM service used to commit the nonce-fetch event
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleAccountLinkStateSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public WaffleAccountLinkStateHandler(ABPropsService abPropsService, WamService wamService) {
        this.abPropsService = abPropsService;
        this.wamService = wamService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebWaffleAccountLinkStateSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return WaffleAccountLinkStateAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebWaffleAccountLinkStateSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return WaffleAccountLinkStateAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebWaffleAccountLinkStateSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return WaffleAccountLinkStateAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@link ABProp#WEB_WAFFLE} is disabled every mutation is
     * {@link MutationApplicationResult#unsupported()} without inspection. Otherwise non-{@link SyncdOperation#SET}
     * operations are unsupported, mutations whose decoded value is not a
     * {@link WaffleAccountLinkStateAction} or whose {@link WaffleAccountLinkStateAction#linkState()}
     * is empty are malformed, and the remaining {@code SET} mutations contribute to the running
     * latest-timestamp tracker. After the per-mutation pass the latest mutation's link state and
     * timestamp are persisted via
     * {@link LinkedWhatsAppAccountStore#setLinkedMetaAccountState(WaffleAccountLinkStateAction.AccountLinkState)}
     * and
     * {@link LinkedWhatsAppAccountStore#setLinkedMetaAccountStateTimestamp(java.time.Instant)}.
     * A resolved {@link WaffleAccountLinkStateAction.AccountLinkState#ACTIVE} state emits the
     * syncd-received WAFFLE lifecycle event via {@link #emitSyncdReceived(Optional)} and, unless the
     * account was already {@link WaffleAccountLinkStateAction.AccountLinkState#ACTIVE}, triggers
     * {@link #requestNonceFromPrimary(LinkedWhatsAppClient)}; the link state recorded before the store
     * overwrite is captured first so the emitted trace action can distinguish a first link, a state
     * transition, and an already-active re-sync.
     *
     * @implNote
     * This implementation drops WA Web's per-mutation warning counters (unsupported and malformed
     * mutation counts); Cobalt's store layer absorbs WA Web's account-linking-state record. Unlike WA
     * Web the store is written for every resolved state (including paused and unlinked), whereas the
     * lifecycle telemetry and the nonce fetch stay confined to the active resolution to match WA Web's
     * {@code logSyncdReceived} contract.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebWaffleAccountLinkStateSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        var accountLinkingEnabled = abPropsService.getBool(ABProp.WEB_WAFFLE);
        if (!accountLinkingEnabled) {
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "waffle account link state: WEB_WAFFLE disabled, {0} mutations unsupported", mutations.size());
        }
        DecryptedMutation.Trusted latest = null;
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            if (!accountLinkingEnabled) {
                results.add(MutationApplicationResult.unsupported());
                continue;
            }

            if (mutation.operation() != SyncdOperation.SET) {
                results.add(MutationApplicationResult.unsupported());
                continue;
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof WaffleAccountLinkStateAction action)
                    || action.linkState().isEmpty()) {
                results.add(SyncdIndexUtils.malformedActionValue(collectionName().name()));
                continue;
            }

            if (latest == null || mutation.timestamp().compareTo(latest.timestamp()) > 0) {
                latest = mutation;
            }
            results.add(MutationApplicationResult.success());
        }
        if (latest != null) {
            var action = (WaffleAccountLinkStateAction) latest.value().flatMap(sav -> sav.action()).orElseThrow();
            var linkState = action.linkState().orElseThrow();
            var previousState = client.store().accountStore().linkedMetaAccountState();
            client.store().accountStore().setLinkedMetaAccountState(linkState);
            client.store().accountStore().setLinkedMetaAccountStateTimestamp(latest.timestamp());
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "waffle account link state: {0} -> {1}", previousState.orElse(null), linkState);
            if (linkState == WaffleAccountLinkStateAction.AccountLinkState.ACTIVE) {
                emitSyncdReceived(previousState);
                if (previousState.orElse(null) != WaffleAccountLinkStateAction.AccountLinkState.ACTIVE) {
                    requestNonceFromPrimary(client);
                }
            }
        }

        return results;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is the single-mutation adapter and applies the same gate-and-validate-and-persist
     * sequence as the batch entry point on a list of size one. When the resolved state is
     * {@link WaffleAccountLinkStateAction.AccountLinkState#ACTIVE} the syncd-received lifecycle event
     * is emitted via {@link #emitSyncdReceived(Optional)} and, unless the account was already active,
     * the {@link #requestNonceFromPrimary(LinkedWhatsAppClient)} side-effect runs inline.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebWaffleAccountLinkStateSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (!abPropsService.getBool(ABProp.WEB_WAFFLE)) {
            return MutationApplicationResult.unsupported();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof WaffleAccountLinkStateAction action)
                || action.linkState().isEmpty()) {
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var linkState = action.linkState().orElseThrow();
        var previousState = client.store().accountStore().linkedMetaAccountState();
        client.store().accountStore().setLinkedMetaAccountState(linkState);
        client.store().accountStore().setLinkedMetaAccountStateTimestamp(mutation.timestamp());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "waffle account link state: {0} -> {1}", previousState.orElse(null), linkState);
        if (linkState == WaffleAccountLinkStateAction.AccountLinkState.ACTIVE) {
            emitSyncdReceived(previousState);
            if (previousState.orElse(null) != WaffleAccountLinkStateAction.AccountLinkState.ACTIVE) {
                requestNonceFromPrimary(client);
            }
        }
        return MutationApplicationResult.success();
    }

    /**
     * Sends a WAFFLE linking-nonce-fetch peer data operation request to the primary device.
     *
     * <p>Called when the resolved link state becomes
     * {@link WaffleAccountLinkStateAction.AccountLinkState#ACTIVE}; the primary device responds with
     * a fresh WAFFLE linking nonce that the companion needs to complete the linking handshake with
     * Meta's servers. A {@link PeerDataOperationRequestType#WAFFLE_LINKING_NONCE_FETCH} request is
     * wrapped in a {@link ProtocolMessage.Type#PEER_DATA_OPERATION_REQUEST_MESSAGE} and dispatched
     * to the current account on device 0. Two telemetry events are committed through the
     * {@link WamService}: the WAFFLE lifecycle event with trace action
     * {@link WaffleLifecycleTraceActionType#NONCE_FETCH_INITIATED} and trace source
     * {@link WaffleLifecycleTraceSourceType#PRIMARY_NONCE_REQUEST}, and the generic
     * peer-data-request event. The method returns without effect when the store has no current JID.
     *
     * @implNote
     * This implementation rebuilds the peer protocol message directly rather than going through WA
     * Web's nonce-fetch API delegation chain. The in-flight promise memoization and 3-second rate
     * limit performed by WA Web are dropped because Cobalt's sync pipeline already serializes
     * mutations on a virtual thread, so every reachable call is an initiated fetch and WA Web's
     * {@link WaffleLifecycleTraceActionType#NONCE_FETCH_DEDUPLICATED} branch is never taken.
     *
     * @param client the {@link LinkedWhatsAppClient} used to dispatch the peer message
     */
    @WhatsAppWebExport(moduleName = "WAWebAccountLinkingNonceFetchAPI", exports = "requestNonceFromPrimary", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSendNonMessageDataRequest", exports = "sendPeerDataOperationRequest", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWaffleLifecycleWamLogger", exports = "logNonceFetch", adaptation = WhatsAppAdaptation.ADAPTED)
    private void requestNonceFromPrimary(LinkedWhatsAppClient client) {
        var me = client.store().accountStore().jid().orElse(null);
        if (me == null) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "waffle nonce fetch: no current jid, skipping request to primary");
            return;
        }

        var request = new PeerDataOperationRequestMessageBuilder()
                .peerDataOperationRequestType(PeerDataOperationRequestType.WAFFLE_LINKING_NONCE_FETCH)
                .build();
        var protocol = new ProtocolMessageBuilder()
                .type(ProtocolMessage.Type.PEER_DATA_OPERATION_REQUEST_MESSAGE)
                .peerDataOperationRequestMessage(request)
                .build();
        var container = new LinkedMessageContainerBuilder()
                .protocolMessage(protocol)
                .build();
        var sessionId = MessageIdGenerator.generate(MessageIdVersion.V2, me);
        this.wamService.commit(new WaffleCompanionStateLifecycleEventBuilder()
                .waffleLifecycleTraceAction(WaffleLifecycleTraceActionType.NONCE_FETCH_INITIATED)
                .waffleLifecycleTraceSource(WaffleLifecycleTraceSourceType.PRIMARY_NONCE_REQUEST)
                .build());
        this.wamService.commit(new NonMessagePeerDataRequestEventBuilder()
                .peerDataRequestCount(1)
                .peerDataRequestType(PeerDataRequestType.WAFFLE_LINKING_NONCE_FETCH)
                .peerDataRequestSessionId(sessionId)
                .build());
        client.sendMessage(me.withDevice(0), container);
        if (Log.INFO) LOGGER.log(Level.INFO, "waffle nonce fetch: requested linking nonce from primary device");
    }

    /**
     * Commits the WAFFLE companion-state lifecycle event for an app-state syncd-received transition.
     *
     * <p>Invoked once per applied mutation (or per applied batch) when the resolved link state is
     * {@link WaffleAccountLinkStateAction.AccountLinkState#ACTIVE}, mirroring WA Web's
     * {@code logSyncdReceived} trace. The {@code previousState} captured before the store overwrite
     * selects the trace action:
     * <ul>
     *     <li>empty (no prior record) yields
     *     {@link WaffleLifecycleTraceActionType#SYNCD_RECEIVED_NO_EXISTING_ROW} with link state
     *     {@link WaffleLifecycleLinkStateType#NOT_APPLICABLE};</li>
     *     <li>a non-{@link WaffleAccountLinkStateAction.AccountLinkState#ACTIVE} prior state yields
     *     {@link WaffleLifecycleTraceActionType#SYNCD_RECEIVED_STATE_TRANSITION} with the prior link
     *     state mapped through {@link #mapLinkStateToWam(WaffleAccountLinkStateAction.AccountLinkState)};</li>
     *     <li>an already-{@link WaffleAccountLinkStateAction.AccountLinkState#ACTIVE} prior state yields
     *     {@link WaffleLifecycleTraceActionType#SYNCD_RECEIVED_ALREADY_ACTIVE} with link state
     *     {@link WaffleLifecycleLinkStateType#ACTIVE}.</li>
     * </ul>
     * The trace source is always {@link WaffleLifecycleTraceSourceType#SYNCD}, and the has-existing-row
     * flag reflects whether {@code previousState} was present.
     *
     * @implNote
     * This implementation reports {@code waffleLifecycleHasAccessToken} as {@code false} unconditionally
     * because Cobalt's account store persists only the link state and its timestamp, never a Meta
     * access token; WA Web derives the flag from the {@code accesstoken} column of its account-linking
     * database row.
     *
     * @param previousState the link state recorded before this mutation was applied, empty when no prior
     *                       WAFFLE record existed
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleLifecycleWamLogger", exports = "logSyncdReceived", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitSyncdReceived(Optional<WaffleAccountLinkStateAction.AccountLinkState> previousState) {
        WaffleLifecycleTraceActionType traceAction;
        WaffleLifecycleLinkStateType linkState;
        if (previousState.isEmpty()) {
            traceAction = WaffleLifecycleTraceActionType.SYNCD_RECEIVED_NO_EXISTING_ROW;
            linkState = WaffleLifecycleLinkStateType.NOT_APPLICABLE;
        } else if (previousState.get() != WaffleAccountLinkStateAction.AccountLinkState.ACTIVE) {
            traceAction = WaffleLifecycleTraceActionType.SYNCD_RECEIVED_STATE_TRANSITION;
            linkState = mapLinkStateToWam(previousState.get());
        } else {
            traceAction = WaffleLifecycleTraceActionType.SYNCD_RECEIVED_ALREADY_ACTIVE;
            linkState = WaffleLifecycleLinkStateType.ACTIVE;
        }
        this.wamService.commit(new WaffleCompanionStateLifecycleEventBuilder()
                .waffleLifecycleTraceAction(traceAction)
                .waffleLifecycleTraceSource(WaffleLifecycleTraceSourceType.SYNCD)
                .waffleLifecycleLinkState(linkState)
                .waffleLifecycleHasExistingRow(previousState.isPresent())
                .waffleLifecycleHasAccessToken(false)
                .build());
    }

    /**
     * Maps a Cobalt {@link WaffleAccountLinkStateAction.AccountLinkState} to its WAM
     * {@link WaffleLifecycleLinkStateType} counterpart.
     *
     * <p>{@link WaffleAccountLinkStateAction.AccountLinkState#UNKNOWN} has no dedicated WAM constant
     * and collapses to {@link WaffleLifecycleLinkStateType#NOT_APPLICABLE}, matching WA Web's
     * {@code mapLinkStateToWam} default branch.
     *
     * @param state the account link state to translate
     * @return the corresponding WAM link-state enum constant
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleLifecycleWamLogger", exports = "mapLinkStateToWam", adaptation = WhatsAppAdaptation.DIRECT)
    private static WaffleLifecycleLinkStateType mapLinkStateToWam(WaffleAccountLinkStateAction.AccountLinkState state) {
        return switch (state) {
            case ACTIVE -> WaffleLifecycleLinkStateType.ACTIVE;
            case PAUSED -> WaffleLifecycleLinkStateType.PAUSED;
            case UNLINKED -> WaffleLifecycleLinkStateType.UNLINKED;
            case UNKNOWN -> WaffleLifecycleLinkStateType.NOT_APPLICABLE;
        };
    }
}
