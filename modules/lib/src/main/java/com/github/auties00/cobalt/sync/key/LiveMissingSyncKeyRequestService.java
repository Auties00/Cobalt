package com.github.auties00.cobalt.sync.key;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.send.id.MessageIdGenerator;
import com.github.auties00.cobalt.message.send.id.MessageIdVersion;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.device.sync.MissingDeviceSyncKeyBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyIdBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyRequest;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyRequestBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncdCoordinator;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MdBootstrapAppStateCriticalDataProcessingEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.BootstrapAppStateDataStageCode;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.*;

/**
 * Live implementation of {@link MissingSyncKeyRequestService} that asks companion devices for
 * app state sync keys whose key id was referenced by a snapshot or patch but is absent from the
 * local sync key store.
 *
 * <p>The syncd decrypt path invokes this service when patch or snapshot decryption fails
 * against a key id that is not in the local store. It filters away ids that are already
 * tracked, broadcasts an {@code AppStateSyncKeyRequest} {@link ProtocolMessage} to every
 * companion device, records per-device delivery outcomes against the missing-key tracker, and
 * emits the bootstrap-progress event that drives the critical-data spinner on the first-load
 * screen. The decrypt-time entry point {@link #requestMissingKeys(Collection)} is
 * fire-and-forget; the periodic entry point {@link #reRequestMissingKeys(Collection)} is
 * driven by {@link MissingSyncKeyTimeoutScheduler#startPeriodicReRequestJob()}.
 *
 * @implNote This implementation collapses WA Web's separate snapshot-record and patch-mutation
 * missing-key scans into the inline id collection that the Cobalt syncd decrypt path already
 * performs, and passes the result directly to {@link #requestMissingKeys(Collection)}.
 * Per-device dispatch is sequential rather than batched so a single misbehaving companion does
 * not stall the whole broadcast under a virtual thread.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdHandleMissingKeys")
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestAllSyncdMissingKeysJob")
@WhatsAppWebModule(moduleName = "WAWebKeyManagementSendKeyRequestApi")
@WhatsAppWebModule(moduleName = "WAWebSyncdStoreMissingKeys")
public final class LiveMissingSyncKeyRequestService implements MissingSyncKeyRequestService {
    /**
     * The logger for {@link LiveMissingSyncKeyRequestService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveMissingSyncKeyRequestService.class);

    /**
     * Holds the injected client used to dispatch peer messages and to resolve the shared store.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Holds the shared store consulted for the current device list, the offline resume state,
     * the missing-key tracker, and the {@link SyncPatchType#CRITICAL_BLOCK} bootstrap flag.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Holds the companion timeout scheduler wired in after construction via
     * {@link #setTimeoutScheduler(MissingSyncKeyTimeoutScheduler)}.
     *
     * @implNote This implementation uses post-construction wiring because the scheduler also
     * depends on this service for its periodic re-request job, producing a circular
     * construction dependency that the owning
     * {@link com.github.auties00.cobalt.sync.WebAppStateService} resolves by constructing both
     * and then calling {@link #setTimeoutScheduler(MissingSyncKeyTimeoutScheduler)}.
     */
    private MissingSyncKeyTimeoutScheduler timeoutScheduler;

    /**
     * Holds the WAM service used to commit the bootstrap progress beacons fired during the
     * critical-data sync.
     */
    private final WamService wamService;

    /**
     * Holds the shared syncd coordinator whose monitor serializes the missing-key tracker
     * mutations against the apply path and the inbound key-share.
     */
    private final SyncdCoordinator coordinator;

    /**
     * Constructs a new request service bound to the supplied client and WAM service.
     *
     * <p>The owning {@link com.github.auties00.cobalt.sync.WebAppStateService} must complete
     * the wiring with {@link #setTimeoutScheduler(MissingSyncKeyTimeoutScheduler)}; without
     * that call the tracker reschedule at the end of {@link #trackMissingKeys(Collection, Set)}
     * is silently skipped.
     *
     * @param client the client used to dispatch peer messages
     * @param wamService the WAM service used to commit critical-bootstrap progress events
     * @param coordinator the shared syncd coordinator serializing tracker mutations
     */
    public LiveMissingSyncKeyRequestService(LinkedWhatsAppClient client, WamService wamService, SyncdCoordinator coordinator) {
        this.client = client;
        this.store = client.store();
        this.wamService = wamService;
        this.coordinator = coordinator;
    }

    @Override
    public void setTimeoutScheduler(MissingSyncKeyTimeoutScheduler timeoutScheduler) {
        this.timeoutScheduler = timeoutScheduler;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation merges WA Web's separate snapshot and patch missing-key
     * entry points into a single id collection assembled inline during decryption, then applies
     * the resume guard and already-tracked deduplication filter before broadcasting.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdHandleMissingKeys",
            exports = {"handleMissingKeysInSnapshot", "handleMissingKeysInPatches"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void requestMissingKeys(Collection<byte[]> keyIds) {
        handleMissingKeys(keyIds);
    }

    @WhatsAppWebExport(moduleName = "WAWebSyncdHandleMissingKeys",
            exports = "handleMissingKeys",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void requestMissingKey(byte[] keyId) {
        requestMissingKeys(List.of(keyId));
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation intentionally bypasses the {@code handleMissingKeys} body:
     * the resume gate ({@link LinkedWhatsAppStore#isResumeFromRestartComplete()}) and the
     * deduplication filter against {@link LinkedWhatsAppSyncStore#findMissingSyncKey(byte[])} are skipped
     * because the keys are by construction already tracked and the periodic job runs only after
     * resume has long completed.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdHandleMissingKeys",
            exports = "requestAllMissingKeys",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestAllSyncdMissingKeysJob",
            exports = "requestAllSyncdMissingKeysJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void reRequestMissingKeys(Collection<byte[]> keyIds) {
        if (keyIds.isEmpty()) {
            return;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "re-requesting {0} missing sync key(s)", keyIds.size());
        var keyIdList = keyIds.stream()
                .filter(Objects::nonNull)
                .map(id -> new AppStateSyncKeyIdBuilder()
                        .keyId(Arrays.copyOf(id, id.length))
                        .build())
                .toList();
        var keyRequest = new AppStateSyncKeyRequestBuilder()
                .keyIds(keyIdList)
                .build();

        sendKeyRequestToAllDevices(keyRequest);
    }

    /**
     * Returns every companion {@link Jid} this client may legitimately ask for sync keys.
     *
     * <p>The result is the registered own-device list with the current device filtered out, so
     * a broadcast reaches every linked phone or other web session that could plausibly hold the
     * key. When the own JID is unavailable an empty list is returned. When the device list is
     * missing or fails to load the result falls back to the primary device (device id
     * {@code 0}).
     *
     * @implNote This implementation logs a warning before returning the singleton primary-only
     * fallback list, matching WA Web's catch branch.
     *
     * @return the companion device {@link Jid}s, or a singleton primary device on lookup failure
     */
    @WhatsAppWebExport(moduleName = "WAWebKeyManagementUtils",
            exports = "getPeerDevices",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private List<Jid> getCompanionDevices() {
        var myJid = store.accountStore().jid()
                .orElse(null);
        if (myJid == null) {
            return List.of();
        }

        try {
            var myDeviceList = store.contactStore().findDeviceList(myJid.toUserJid());
            if (myDeviceList.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "getPeerDevices: no device list, falling back to primary device only");
                return List.of(Jid.of(myJid.user(), myJid.server(), 0, 0));
            }

            return myDeviceList.get()
                    .devices()
                    .stream()
                    .filter(device -> device.id() != myJid.device())
                    .map(device -> device.toDeviceJid(myJid.user(), myJid.server()))
                    .toList();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "getPeerDevices: device list lookup failed, falling back to primary device only", e);
            return List.of(Jid.of(myJid.user(), myJid.server(), 0, 0));
        }
    }

    /**
     * Filters out already-tracked key ids, broadcasts the request, then records the new
     * trackers via {@link #trackMissingKeys(Collection, Set)}.
     *
     * <p>The shared body invoked by both {@link #requestMissingKeys(Collection)} and the
     * single-id overload. Returns early when the offline-resume sequence has not yet completed
     * ({@link LinkedWhatsAppStore#isResumeFromRestartComplete()} is {@code false}), when the input is
     * empty, and when every supplied id is already tracked. Surviving ids are wrapped into an
     * {@code AppStateSyncKeyRequest} and broadcast; the accepting device ids are then recorded
     * against the tracker.
     *
     * @implNote This implementation defensively copies each incoming {@code byte[]} before
     * storing it because the protobuf model exposes mutable byte arrays through its getters.
     *
     * @param keyIds the missing key ids
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdHandleMissingKeys",
            exports = "handleMissingKeys",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void handleMissingKeys(Collection<byte[]> keyIds) {
        if (keyIds.isEmpty()) {
            return;
        }

        var requestedKeyIds = coordinator.runLocked(() -> {
            if (!store.connectionStore().isResumeFromRestartComplete()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "handleMissingKeys: skip, resume in progress");
                return List.<byte[]>of();
            }
            return keyIds.stream()
                    .filter(Objects::nonNull)
                    .map(id -> Arrays.copyOf(id, id.length))
                    .filter(id -> store.syncStore().findMissingSyncKey(id).isEmpty())
                    .toList();
        });
        if (requestedKeyIds.isEmpty()) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "handleMissingKeys: requesting {0} new missing key(s)", requestedKeyIds.size());

        var keyIdList = requestedKeyIds.stream()
                .map(id -> new AppStateSyncKeyIdBuilder()
                        .keyId(id)
                        .build())
                .toList();
        var keyRequest = new AppStateSyncKeyRequestBuilder()
                .keyIds(keyIdList)
                .build();

        var successfulDeviceIds = sendKeyRequestToAllDevices(keyRequest);

        coordinator.runLocked(() -> trackMissingKeys(requestedKeyIds, successfulDeviceIds));
    }

    /**
     * Builds and dispatches the {@code AppStateSyncKeyRequest} peer message to every companion
     * device, returning the device ids that accepted the send.
     *
     * <p>Throws when the device list resolves to an empty companion set, since no peer can then
     * answer the request, and when every per-device send fails. A partial failure logs a
     * warning and returns the surviving device ids. After the fan-out, the
     * {@link BootstrapAppStateDataStageCode#MISSING_KEYS_REQUESTED} bootstrap beacon is emitted.
     *
     * @implNote This implementation runs the per-device sends sequentially rather than in
     * parallel: under the virtual-thread model the parallel fan-out savings do not justify the
     * bookkeeping, and the sequential loop preserves the peer-message store ordering established
     * by {@link LinkedWhatsAppChatStore#addPeerMessage(String, ChatMessageInfo)}. The beacon fires after
     * the fan-out so it reflects the delivery outcome rather than the intent.
     *
     * @param keyRequest the {@code AppStateSyncKeyRequest} payload
     * @return the device ids that successfully accepted the peer message
     * @throws IllegalStateException when no companion devices exist or every send fails
     */
    @WhatsAppWebExport(moduleName = "WAWebKeyManagementSendKeyRequestApi",
            exports = "sendAppStateSyncKeyRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Set<Integer> sendKeyRequestToAllDevices(AppStateSyncKeyRequest keyRequest) {
        var companionDevices = getCompanionDevices();

        if (companionDevices.isEmpty()) {
            throw new IllegalStateException(
                    "syncd: sendAppStateSyncKeyRequest: no peer devices available to request key from");
        }

        var messages = buildKeyRequestMessages(companionDevices, keyRequest);

        var keyIdHexes = keyRequest.keyIds().stream()
                .map(id -> id.keyId().map(SyncKeyUtils::syncKeyIdToHex).orElse("?"))
                .toList();
        var deviceIds = companionDevices.stream()
                .map(Jid::device)
                .toList();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "send key request: keyIds={0} deviceIds={1}", keyIdHexes, deviceIds);

        for (var entry : messages.entrySet()) {
            var msgId = entry.getValue().key().id().orElse(null);
            if (msgId != null) {
                store.chatStore().addPeerMessage(msgId, entry.getValue());
            }
        }

        var successfulDeviceIds = new LinkedHashSet<Integer>();
        var failureMessages = new ArrayList<String>();
        for (var entry : messages.entrySet()) {
            var device = entry.getKey();
            var messageInfo = entry.getValue();
            try {
                client.sendPeerMessage(device, messageInfo);
                successfulDeviceIds.add(device.device());
            } catch (Exception e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "send key request failed for device {0}: {1}", device, e.getMessage());
                failureMessages.add(e.getMessage());
            }
        }

        var failureCount = failureMessages.size();
        if (failureCount > 0 && failureCount < companionDevices.size()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sendAppStateSyncKeyRequest: {0}/{1} peer device(s) failed",
                    failureCount, companionDevices.size());
        } else if (failureCount == companionDevices.size()) {
            var errorDetails = String.join(", ", failureMessages);
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sendAppStateSyncKeyRequest: all {0} peers failed: {1}", companionDevices.size(), errorDetails);
            throw new IllegalStateException(
                    "syncd: sendAppStateSyncKeyRequest failed for all " + companionDevices.size() + " peer device(s): " + errorDetails);
        }

        logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.MISSING_KEYS_REQUESTED);

        return successfulDeviceIds;
    }

    /**
     * Emits a critical-data processing event for the supplied bootstrap stage when the critical
     * data sync is still in progress.
     *
     * <p>Drives the WAM-side critical-data progress beacon that surfaces the preparing-your-data
     * sub-states on the first-load screen. The method is a no-op once the
     * {@link SyncPatchType#CRITICAL_BLOCK} collection has been bootstrapped.
     *
     * @implNote This implementation approximates WA Web's critical-data sync state machine by
     * reading the {@code bootstrapped} flag on the local
     * {@link com.github.auties00.cobalt.sync.WebAppStateService} state for
     * {@link SyncPatchType#CRITICAL_BLOCK}; Cobalt exposes no separate global critical-data sync
     * flag.
     *
     * @param stage the bootstrap stage reached
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCriticalBootstrapProcessingApi", exports = "logCriticalBootstrapStageIfNecessary", adaptation = WhatsAppAdaptation.ADAPTED)
    private void logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode stage) {
        if (store.syncStore().findWebAppState(SyncPatchType.CRITICAL_BLOCK).bootstrapped()) {
            return;
        }
        wamService.commit(new MdBootstrapAppStateCriticalDataProcessingEventBuilder()
                .bootstrapAppStateDataStage(stage)
                .mdTimestamp((int) System.currentTimeMillis())
                .build());
    }

    /**
     * Builds one {@code AppStateSyncKeyRequest} {@link ChatMessageInfo} per target device,
     * preserving the iteration order of the input device list.
     *
     * <p>All messages share the same {@link ProtocolMessage} payload but each carries a freshly
     * generated {@link com.github.auties00.cobalt.wire.core.message.MessageKey} id so the relay
     * treats them as independent peer messages rather than a duplicate broadcast. Every message
     * is parented to the user-level JID without a device suffix. The own JID must be available;
     * its absence throws.
     *
     * @implNote This implementation populates both {@code senderJid} and {@code timestamp} on
     * each message because Cobalt's message infrastructure requires them, whereas WA Web derives
     * both downstream.
     *
     * @param companionDevices the target device {@link Jid}s; iteration order is preserved
     * @param keyRequest the {@code AppStateSyncKeyRequest} payload
     * @return a {@link LinkedHashMap} from device {@link Jid} to the built {@link ChatMessageInfo}
     * @throws IllegalStateException when no own JID is available
     */
    @WhatsAppWebExport(moduleName = "WAWebKeyManagementSendKeyRequestApi",
            exports = "sendAppStateSyncKeyRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private LinkedHashMap<Jid, ChatMessageInfo> buildKeyRequestMessages(List<Jid> companionDevices, AppStateSyncKeyRequest keyRequest) {
        var self = store.accountStore().jid().orElseThrow(() ->
                new IllegalStateException("syncd: sendAppStateSyncKeyRequest: no JID available"));

        var protocolMessage = new ProtocolMessageBuilder()
                .type(ProtocolMessage.Type.APP_STATE_SYNC_KEY_REQUEST)
                .appStateSyncKeyRequest(keyRequest)
                .build();
        var messageContainer = new LinkedMessageContainerBuilder()
                .protocolMessage(protocolMessage)
                .build();

        var userJid = self.toUserJid();
        var messages = new LinkedHashMap<Jid, ChatMessageInfo>();
        for (var device : companionDevices) {
            var messageKey = new MessageKeyBuilder()
                    .id(MessageIdGenerator.generate(MessageIdVersion.V1, self))
                    .parentJid(userJid)
                    .fromMe(true)
                    .senderJid(self)
                    .build();
            var messageInfo = new ChatMessageInfoBuilder()
                    .key(messageKey)
                    .message(messageContainer)
                    .timestamp(Instant.now())
                    .senderJid(self)
                    .build();
            messages.put(device, messageInfo);
        }
        return messages;
    }

    /**
     * Records each newly asked key in the missing-key store with its asked-device set, then
     * triggers an inline reschedule of the wait-for-key timeout.
     *
     * <p>Each entry is created with the current timestamp; an existing entry for the same key id
     * is overwritten so the asked-device set always reflects the latest broadcast. After the
     * upserts, the wait-for-key timeout is rescheduled through
     * {@link MissingSyncKeyTimeoutScheduler#scheduleTimeoutCheck()} when the scheduler has been
     * wired in.
     *
     * @implNote This implementation upserts per entry rather than in a single transaction
     * because Cobalt's store exposes no bulk-update primitive. The terminal scheduler call is
     * gated on {@link #timeoutScheduler} being non-null so test harnesses can exercise the
     * request path without the scheduler.
     *
     * @param keyIds the key ids to track
     * @param successfulDeviceIds the device ids that accepted the broadcast
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdStoreMissingKeys",
            exports = "addMissingKeys",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void trackMissingKeys(Collection<byte[]> keyIds, Set<Integer> successfulDeviceIds) {
        for (var keyId : keyIds) {
            var missingKey = new MissingDeviceSyncKeyBuilder()
                    .keyId(keyId)
                    .timestamp(Instant.now())
                    .askedDevices(Set.copyOf(successfulDeviceIds))
                    .build();
            store.syncStore().addMissingSyncKey(missingKey);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tracked {0} missing sync key(s), asked {1} device(s)",
                keyIds.size(), successfulDeviceIds.size());

        if (timeoutScheduler != null) {
            timeoutScheduler.scheduleTimeoutCheck();
        }
    }
}
