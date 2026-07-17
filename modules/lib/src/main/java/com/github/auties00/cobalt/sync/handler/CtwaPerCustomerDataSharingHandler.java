package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingPreference;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingPreferenceBuilder;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaDataSharingSetting;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CtwaPerCustomerDataSharingAction;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MmSignalEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MmSignalSharingVerificationEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SmbPerCustomerDataSharingControlEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ConsentSource;
import com.github.auties00.cobalt.wire.wam.type.MmDirectionFrom;
import com.github.auties00.cobalt.wire.wam.type.MmSignalType;
import com.github.auties00.cobalt.wire.wam.type.OnePdSignalNotSharedReason;
import com.github.auties00.cobalt.wire.wam.type.SignalSharingStatus;
import com.github.auties00.cobalt.wire.wam.type.SignalSurface;
import com.github.auties00.cobalt.wire.wam.type.SignalType;
import com.github.auties00.cobalt.wire.wam.type.SmbPerCustomerDataSharingControlAction;
import com.github.auties00.cobalt.wire.wam.type.SmbPerCustomerDataSharingControlEntryPoint;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Tracks per-customer Click-To-WhatsApp Ads data-sharing consent from {@code ctwaPerCustomerDataSharing} sync mutations.
 *
 * <p>This handler drives the SMB CTWA per-customer data-sharing consent
 * surface where a business owner toggles, per business-account LID, whether
 * customer-level CTWA telemetry may be shared with third-party partners. When
 * the toggle changes on another device, the server replays the resulting
 * {@link CtwaPerCustomerDataSharingAction} here, and the flag becomes readable
 * through
 * {@link LinkedWhatsAppBusinessStore#findCtwaDataSharing(String)}.
 *
 * <p>Whenever a replayed mutation flips a customer's stored preference, the
 * handler mirrors WA Web's disclosure telemetry: it commits the SMB
 * per-customer data-sharing control event (the WAM half of the
 * {@code SYSTEM_MESSAGE_INSERTED} path) together with the adjacent
 * marketing-message conversion-signal-sharing verification and signal events,
 * all keyed to the customer LID whose consent transitioned.
 *
 * @implNote
 * This implementation commits the WAM telemetry that WA Web fires from
 * {@code maybeGeneratePerCustomerDataSharingSystemMessage} and the
 * {@code WAWebMmSignalSharingLoggingUtils} disclosure/verification helpers, but
 * still drops the UI-only side effects: the system-message insertion, the
 * {@code updateDataSharing3pdLidInCollection} /
 * {@code removeDataSharing3pdLidFromCollection} fire-and-forget frontend
 * collection updates have no Cobalt counterpart because there is no browser
 * frontend bridge or UI system-message pipeline. WA Web scopes those signal
 * events to per-message ad interactions; Cobalt has no marketing-message
 * interaction surface, so the closest faithful trigger in the linked transport
 * is the per-customer consent transition applied here, and the signal payloads
 * that WA Web derives from a live ad conversation are synthesised from the
 * customer LID and the host clock. The per-LID IDB row maps to a single per-LID
 * {@link CtwaDataSharingPreference}
 * keyed by raw LID string in the unified store.
 */
@WhatsAppWebModule(moduleName = "WAWebCtwaPerCustomerDataSharingSync")
@WhatsAppWebModule(moduleName = "WAWebPerCustomerDataSharingControlLogging")
@WhatsAppWebModule(moduleName = "WAWebMmSignalSharingLoggingUtils")
public final class CtwaPerCustomerDataSharingHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link CtwaPerCustomerDataSharingHandler}.
     */
    private static final System.Logger LOGGER = Log.get(CtwaPerCustomerDataSharingHandler.class);

    /**
     * The version stamp carried by every SMB per-customer data-sharing control
     * event.
     *
     * <p>Mirrors the module-level {@code e = 1} constant that
     * {@code WAWebPerCustomerDataSharingControlLogging} writes into
     * {@code smbPerCustomerDataSharingControlVersion} on every commit.
     */
    @WhatsAppWebExport(moduleName = "WAWebPerCustomerDataSharingControlLogging", exports = "logPerCustomerDataSharingControlEvent", adaptation = WhatsAppAdaptation.DIRECT)
    private static final long CONTROL_LOGGING_VERSION = 1L;

    /**
     * The conversion-schema version advertised on the emitted marketing-message
     * signal event.
     *
     * <p>WA Web sources this from the live conversion-token schema; Cobalt has
     * no conversion-token pipeline, so it reports the first schema revision as a
     * stable, plausible value.
     */
    private static final long MM_CONVERSION_SCHEMA_VERSION = 1L;

    /**
     * The {@link WamService} used to commit the per-customer data-sharing
     * control event and the adjacent marketing-message signal-sharing
     * verification and conversion-signal events.
     */
    private final WamService wamService;

    /**
     * Constructs the CTWA-per-customer-data-sharing handler bound to the given
     * telemetry service.
     *
     * <p>The sync handler registry instantiates this once during client
     * bootstrap, forwarding the shared {@link WamService} so consent
     * transitions replayed by the server can be reported.
     *
     * @param wamService the WAM telemetry service used to commit disclosure and
     *                   signal-sharing events
     */
    @WhatsAppWebExport(moduleName = "WAWebCtwaPerCustomerDataSharingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public CtwaPerCustomerDataSharingHandler(WamService wamService) {
        this.wamService = wamService;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCtwaPerCustomerDataSharingSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return CtwaPerCustomerDataSharingAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCtwaPerCustomerDataSharingSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return CtwaPerCustomerDataSharingAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebCtwaPerCustomerDataSharingSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return CtwaPerCustomerDataSharingAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For SET mutations, validates that {@code indexParts[1]} (the account
     * LID raw string) is present and that the value carries a
     * {@link CtwaPerCustomerDataSharingAction}, then upserts a
     * {@link CtwaDataSharingPreference}
     * keyed by that LID. When the upsert actually flips the customer's stored
     * preference, the disclosure and signal-sharing WAM telemetry is committed
     * for that LID. For REMOVE mutations, drops the entry by LID. Returns
     * {@link MutationApplicationResult#unsupported()} for other operations.
     *
     * @implNote
     * This implementation reads the
     * {@link CtwaPerCustomerDataSharingAction#isCtwaPerCustomerDataSharingEnabled()}
     * field which coalesces a missing wire field to {@code false};
     * WA Web treats a missing flag as malformed and emits
     * {@link SyncdIndexUtils#malformedActionValue(String)}. The
     * Cobalt model accessor is lossy on the boolean wire field so
     * the malformed branch on null-flag is unreachable here. The
     * previous stored flag is read before the upsert so telemetry is
     * committed only when the effective preference changes, matching
     * WA Web's {@code !(lastState != null && lastState === effective)}
     * guard on system-message generation. The REMOVE branch passes a
     * possibly-null account LID through to
     * {@link LinkedWhatsAppBusinessStore#removeCtwaDataSharing(String)},
     * matching WA Web's IDB-no-op semantic when the index slot is
     * missing, and, like WA Web's remove path, emits no telemetry.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebCtwaPerCustomerDataSharingSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var indexArray = JSON.parseArray(mutation.index());
        var accountLid = indexArray.size() > 1 ? indexArray.getString(1) : null;

        switch (mutation.operation()) {
            case SET -> {
                if (accountLid == null) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "ctwa per-customer data sharing: missing account lid in mutation index");
                    return SyncdIndexUtils.malformedActionValue(collectionName().name());
                }

                if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof CtwaPerCustomerDataSharingAction action)) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "ctwa per-customer data sharing: mutation value is not a CtwaPerCustomerDataSharingAction");
                    return SyncdIndexUtils.malformedActionValue(collectionName().name());
                }

                var enabled = action.isCtwaPerCustomerDataSharingEnabled();

                var previousEnabled = client.store().businessStore().findCtwaDataSharing(accountLid)
                        .map(CtwaDataSharingPreference::enabled)
                        .orElse(null);

                client.store().businessStore().putCtwaDataSharing(new CtwaDataSharingPreferenceBuilder()
                        .accountLid(accountLid)
                        .enabled(enabled)
                        .build());
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa per-customer data sharing: set lid={0} enabled={1}", new LogRedactable.User(accountLid), enabled);

                if (previousEnabled == null || previousEnabled != enabled) {
                    logPerCustomerDataSharingStateChange(client, accountLid, enabled);
                }

                return MutationApplicationResult.success();
            }
            case REMOVE -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa per-customer data sharing: remove lid={0}", new LogRedactable.User(accountLid));
                client.store().businessStore().removeCtwaDataSharing(accountLid);

                return MutationApplicationResult.success();
            }
            default -> {
                return MutationApplicationResult.unsupported();
            }
        }
    }

    /**
     * Commits the WAM telemetry for a per-customer data-sharing consent
     * transition.
     *
     * <p>Resolves the effective opt-in status by combining the customer's
     * per-LID toggle with the account-wide
     * {@link CtwaDataSharingSetting} (WA Web's
     * {@code perCustomerState && globalSetting === true}), then commits the SMB
     * control event followed by the marketing-message signal-sharing
     * verification and conversion-signal events for the transitioned LID.
     *
     * @param client     the client whose business store carries the account-wide
     *                   data-sharing setting
     * @param accountLid the customer account LID whose preference changed
     * @param enabled    the customer's new per-LID data-sharing flag
     */
    private void logPerCustomerDataSharingStateChange(LinkedWhatsAppClient client, String accountLid, boolean enabled) {
        var globalEnabled = client.store().businessStore().ctwaDataSharingSetting()
                .orElse(CtwaDataSharingSetting.NOT_SET) == CtwaDataSharingSetting.ENABLED;
        var optIn = enabled && globalEnabled;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ctwa per-customer data sharing: committing telemetry lid={0} optIn={1}", new LogRedactable.User(accountLid), optIn);

        commitControlEvent(optIn);
        commitSignalSharingVerificationEvent(accountLid, globalEnabled, optIn);
        commitMmSignalEvent(accountLid, globalEnabled);
    }

    /**
     * Commits the SMB per-customer data-sharing control event for a replayed
     * consent transition.
     *
     * <p>Reports the {@code SYSTEM_MESSAGE_INSERTED} action under the
     * {@code SYNCD_MUTATION} entry point, carrying the effective opt-in status,
     * exactly as WA Web's {@code maybeGeneratePerCustomerDataSharingSystemMessage}
     * routes into
     * {@code WAWebPerCustomerDataSharingControlLogging.logPerCustomerDataSharingControlEvent}
     * when applying a synced mutation.
     *
     * @param optIn the effective opt-in status after the transition
     */
    @WhatsAppWebExport(moduleName = "WAWebPerCustomerDataSharingControlLogging", exports = "logPerCustomerDataSharingControlEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitControlEvent(boolean optIn) {
        wamService.commit(new SmbPerCustomerDataSharingControlEventBuilder()
                .smbPerCustomerDataSharingControlAction(SmbPerCustomerDataSharingControlAction.SYSTEM_MESSAGE_INSERTED)
                .smbPerCustomerDataSharingControlCurrentOptInStatus(optIn)
                .smbPerCustomerDataSharingControlEntryPoint(SmbPerCustomerDataSharingControlEntryPoint.SYNCD_MUTATION)
                .smbPerCustomerDataSharingControlVersion(CONTROL_LOGGING_VERSION)
                .build());
    }

    /**
     * Commits the marketing-message signal-sharing verification event for a
     * consent transition.
     *
     * <p>Reports the disclosure/consent state of the customer relationship: the
     * business LID that transitioned, whether the account-wide disclosure is
     * accepted, and the resulting sharing status ({@code ONE_PD} when the signal
     * would flow, {@code NOT_SHARED} otherwise, tagged with the
     * undisclosed-or-ineligible reason). The collection-window identifier is
     * derived from the host clock the way WA Web base64-encodes the disclosure
     * window timestamp.
     *
     * @implNote
     * This implementation reports {@code isCompanionDevice} as {@code true}
     * because the CTWA per-customer data-sharing surface is a WA Web companion
     * feature, matching WA Web's hard-coded value; the signal surface, type and
     * direction describe a customer-thread message signal since the transition
     * originates from a chat-scoped consent toggle rather than a live ad click.
     *
     * @param accountLid    the customer account LID that transitioned
     * @param globalEnabled whether the account-wide data-sharing disclosure is
     *                      accepted
     * @param optIn         the effective opt-in status after the transition
     */
    @WhatsAppWebExport(moduleName = "WAWebMmSignalSharingLoggingUtils", exports = "logMmSignalSharingVerificationEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitSignalSharingVerificationEvent(String accountLid, boolean globalEnabled, boolean optIn) {
        var builder = new MmSignalSharingVerificationEventEventBuilder()
                .accountLinked(false)
                .businessLidOrJid(accountLid)
                .collectionWindowId(newCollectionWindowId())
                .consentSource(ConsentSource.DISCLOSURE)
                .isCompanionDevice(true)
                .isNetworkAvailable(true)
                .isShimmingSignal(false)
                .isUserDisclosed(globalEnabled)
                .isLatestConversionToken(true)
                .mmDirectionFrom(MmDirectionFrom.CUSTOMER)
                .signalSurface(SignalSurface.CHAT_THREAD)
                .signalType(SignalType.MESSAGE)
                .signalSharingStatus(optIn ? SignalSharingStatus.ONE_PD : SignalSharingStatus.NOT_SHARED);

        if (!optIn) {
            builder.onePdSignalNotSharedReason(OnePdSignalNotSharedReason.USER_UNDISCLOSED_OR_NOT_ELIGIBLE_FOR_DISCLOSURE);
        }

        wamService.commit(builder.build());
    }

    /**
     * Commits the marketing-message conversion-signal event for a consent
     * transition.
     *
     * <p>Reports the customer-scoped conversion signal under the account-wide
     * disclosure state, matching WA Web's
     * {@code WAWebMmSignalSharingLoggingUtils.logMmSignalSharingEvent} which
     * carries the signal payload, the signal type and the disclosure flag. The
     * opaque signal payload that WA Web derives from a live ad conversation is
     * synthesised from the customer LID and the host clock.
     *
     * @param accountLid    the customer account LID that transitioned
     * @param globalEnabled whether the account-wide data-sharing disclosure is
     *                      accepted
     */
    @WhatsAppWebExport(moduleName = "WAWebMmSignalSharingLoggingUtils", exports = "logMmSignalSharingEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMmSignalEvent(String accountLid, boolean globalEnabled) {
        wamService.commit(new MmSignalEventBuilder()
                .mmSignalData(newSignalData(accountLid))
                .mmSignalType(MmSignalType.MESSAGE)
                .disclosed(globalEnabled)
                .consentSource(ConsentSource.DISCLOSURE)
                .mmDirectionFrom(MmDirectionFrom.CUSTOMER)
                .isLatestConversionToken(true)
                .mmConversionSchemaVersion(MM_CONVERSION_SCHEMA_VERSION)
                .build());
    }

    /**
     * Produces a collection-window identifier for the emitted verification
     * event.
     *
     * <p>Base64-encodes the current epoch-millisecond string, reproducing the
     * shape of WA Web's {@code btoa(String(unixTime * 1000))} disclosure-window
     * identifier.
     *
     * @return the base64-encoded collection-window identifier
     */
    private static String newCollectionWindowId() {
        return Base64.getEncoder()
                .encodeToString(Long.toString(System.currentTimeMillis()).getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Produces an opaque marketing-message signal payload for the emitted signal
     * event.
     *
     * <p>Base64-encodes a customer-LID and host-clock derived token so the
     * committed {@code mmSignalData} carries a non-empty, per-customer,
     * time-varying value in the shape of WA Web's encoded conversion token.
     *
     * @param accountLid the customer account LID the signal belongs to
     * @return the base64-encoded signal payload
     */
    private static String newSignalData(String accountLid) {
        var raw = accountLid + ":" + Long.toHexString(System.currentTimeMillis());
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

}
