package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.util.BufferedProtobufInputStream;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.message.send.id.MessageIdGenerator;
import com.github.auties00.cobalt.message.send.id.MessageIdVersion;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestMessageSyncDCollectionFatalRecoveryRequestBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestType;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdSnapshotRecovery;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdSnapshotRecoverySpec;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.NonMessagePeerDataRequestEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.PeerDataRequestType;

import java.io.ByteArrayInputStream;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * Live implementation of {@link SnapshotRecoveryService} that drives the companion-to-primary
 * syncd snapshot recovery peer-data operation.
 *
 * <p>The service is invoked by {@link WebAppStateService} as part of the syncd patch pipeline,
 * after a {@link WhatsAppWebAppStateSyncException} marked as fatal is raised; embedders never
 * call it directly. Recovery covers a snapshot mismatch on a non-{@link SyncPatchType#CRITICAL_BLOCK}
 * collection when the AB-prop gate is on, the primary device supports recovery, and the mutation
 * count is within the configured budget.
 */
@WhatsAppWebModule(moduleName = "WAWebRequestSyncdSnapshotRecovery")
@WhatsAppWebModule(moduleName = "WAWebSyncdSnapshotRecoveryGatingUtils")
@WhatsAppWebModule(moduleName = "WAWebSendNonMessageDataRequest")
public final class LiveSnapshotRecoveryService implements SnapshotRecoveryService {
    /**
     * The logger for {@link LiveSnapshotRecoveryService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveSnapshotRecoveryService.class);

    /**
     * The hard upper bound, in milliseconds, on a single recovery call.
     */
    private static final long RECOVERY_TIMEOUT_MS = 60_000;

    /**
     * The {@link LinkedWhatsAppClient} used to send the peer-data operation request
     * to the primary device.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The {@link ABPropsService} used to read the recovery-enabled and
     * maximum-mutation-count gates.
     */
    private final ABPropsService abPropsService;

    /**
     * The {@link WamService} used to commit the per-request telemetry event.
     */
    private final WamService wamService;

    /**
     * The map of in-flight per-collection recovery futures.
     */
    private final Map<SyncPatchType, CompletableFuture<SyncdSnapshotRecovery>> pendingRecoveries;

    /**
     * The semaphore that serialises concurrent recovery attempts across
     * collections.
     */
    private final Semaphore recoverySemaphore;

    /**
     * Builds a new recovery service.
     *
     * <p>Constructed once by {@link WebAppStateService}; the service caches
     * its dependencies and lives for the lifetime of the parent client.
     *
     * @param client         the {@link LinkedWhatsAppClient} used to send the
     *                       recovery peer-message
     * @param abPropsService the {@link ABPropsService} that gates the
     *                       recovery flow
     * @param wamService     the {@link WamService} used to commit the
     *                       per-request telemetry event
     */
    @WhatsAppWebExport(moduleName = "WAWebRequestSyncdSnapshotRecovery", exports = "SyncdSnapshotRecoveryModule", adaptation = WhatsAppAdaptation.ADAPTED)
    public LiveSnapshotRecoveryService(LinkedWhatsAppClient client, ABPropsService abPropsService, WamService wamService) {
        this.client = client;
        this.abPropsService = abPropsService;
        this.wamService = wamService;
        this.pendingRecoveries = new ConcurrentHashMap<>();
        this.recoverySemaphore = new Semaphore(1);
    }

    @WhatsAppWebExport(moduleName = "WAWebSyncdSnapshotRecoveryGatingUtils", exports = "syncdSnapshotRecoveryEnabled", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public boolean isRecoveryEnabled() {
        if (!client.store().syncStore().primaryDeviceSupportsSyncdRecovery()) {
            return false;
        }

        return abPropsService.getBool(ABProp.ENABLE_PEER_SNAPSHOT_RECOVERY);
    }

    @WhatsAppWebExport(moduleName = "WAWebSyncdSnapshotRecoveryGatingUtils", exports = "updatePrimaryDeviceSupportsSyncdRecovery", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void updatePrimaryDeviceSupportsSyncdRecovery(boolean supported) {
        client.store().syncStore().setPrimaryDeviceSupportsSyncdRecovery(supported);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation expects the caller (typically
     * {@link WebAppStateService}) to have already classified the triggering
     * exception as fatal; Cobalt's sealed exception hierarchy enforces that
     * statically at call sites rather than with an {@code instanceof} check
     * here.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdSnapshotRecoveryGatingUtils", exports = "shouldPreformSnapshotRecovery", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public boolean shouldAttemptRecovery(SyncPatchType collectionName, int mutationCount) {
        if (!isRecoveryEnabled()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "snapshot recovery disabled, skipping collection {0}", collectionName);
            return false;
        }

        if (collectionName == SyncPatchType.CRITICAL_BLOCK) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "snapshot recovery not supported for critical block collection");
            return false;
        }

        var maxMutations = abPropsService.getInt(ABProp.SNAPSHOT_RECOVERY_MAX_MUTATIONS_COUNT_ALLOWED);
        var withinBudget = mutationCount <= maxMutations;
        if (!withinBudget && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "snapshot recovery mutation count {0} exceeds budget {1} for collection {2}", mutationCount, maxMutations, collectionName);
        }
        return withinBudget;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation funnels every concurrent recovery through
     * {@link #recoverySemaphore} so two collections cannot race the primary's
     * promise registry. The same {@value #RECOVERY_TIMEOUT_MS} ms budget
     * covers both the semaphore acquisition and the response wait, so a slow
     * concurrent recovery does not extend the effective per-call timeout. The
     * {@code primaryDevice == null} bail-out is Cobalt-specific because the
     * caller's JID may not be set yet during early startup.
     */
    @WhatsAppWebExport(moduleName = "WAWebRequestSyncdSnapshotRecovery", exports = "SyncdSnapshotRecoveryModule", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public SyncdSnapshotRecovery requestRecovery(SyncPatchType collectionName) {
        var startTime = System.currentTimeMillis();
        try {
            if (!recoverySemaphore.tryAcquire(RECOVERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot recovery timed out waiting for concurrent recovery to complete, collection {0}", collectionName);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        try {
            var responseFuture = pendingRecoveries.computeIfAbsent(collectionName,
                    _ -> new CompletableFuture<>());

            sendRecoveryRequest(collectionName);

            var elapsed = System.currentTimeMillis() - startTime;
            var remainingTimeout = RECOVERY_TIMEOUT_MS - elapsed;
            if (remainingTimeout <= 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot recovery timed out after semaphore acquisition for collection {0}", collectionName);
                return null;
            }
            var recovered = responseFuture.get(remainingTimeout, TimeUnit.MILLISECONDS);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "snapshot recovery completed for collection {0}", collectionName);
            return recovered;
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot recovery failed for collection " + collectionName, e);
            return null;
        } finally {
            pendingRecoveries.remove(collectionName);
            recoverySemaphore.release();
        }
    }

    @WhatsAppWebExport(moduleName = "WAWebRequestSyncdSnapshotRecovery", exports = "SyncdSnapshotRecoveryModule", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void resolveRecovery(
            SyncPatchType collectionName,
            SyncdSnapshotRecovery recoveredSnapshot
    ) {
        var future = pendingRecoveries.get(collectionName);
        if (future != null) {
            future.complete(recoveredSnapshot);
        } else {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "received snapshot recovery response for {0} but no pending request found", collectionName);
        }
    }

    @WhatsAppWebExport(moduleName = "WAWebNonMessageDataRequestHandler", exports = "m", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public SyncdSnapshotRecovery decodeRecoverySnapshot(
            PeerDataOperationRequestResponseMessage.PeerDataOperationResult.SyncDSnapshotFatalRecoveryResponse response
    ) {
        var snapshotBytes = response.collectionSnapshot()
                .orElseThrow(() -> new NoSuchElementException("Missing snapshot"));
        try {
            if (response.isCompressed()) {
                try (var protobufStream = new BufferedProtobufInputStream(new GZIPInputStream(new ByteArrayInputStream(snapshotBytes)))) {
                    return SyncdSnapshotRecoverySpec.decode(protobufStream);
                }
            } else {
                return SyncdSnapshotRecoverySpec.decode(snapshotBytes);
            }
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to decode snapshot recovery response", e);
            throw new WhatsAppWebAppStateSyncException.ExternalDecodeFailed(e);
        }
    }

    /**
     * Builds and sends the snapshot fatal-recovery peer-data operation
     * request to the primary device.
     *
     * <p>The request is wrapped in a {@link ProtocolMessage} of type
     * {@link ProtocolMessage.Type#PEER_DATA_OPERATION_REQUEST_MESSAGE} and a
     * {@link com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer} before
     * being sent via {@link LinkedWhatsAppClient#sendPeerMessage}. Per-request
     * telemetry is committed through {@link WamService#commit} as a
     * {@link com.github.auties00.cobalt.wire.wam.event.NonMessagePeerDataRequestEvent}
     * keyed on the outbound peer-message id.
     *
     * @implNote This implementation performs the message wrapping inline
     * because Cobalt has no shared key-message send helper.
     *
     * @param collectionName the collection to recover
     * @throws IllegalStateException if no primary device JID can be resolved
     *                               or the local user's JID is not set
     */
    @WhatsAppWebExport(moduleName = "WAWebSendNonMessageDataRequest", exports = "sendPeerDataOperationRequest", adaptation = WhatsAppAdaptation.ADAPTED)
    private void sendRecoveryRequest(SyncPatchType collectionName) {
        var primaryDevice = getPrimaryDevice();
        if (primaryDevice == null) {
            throw new IllegalStateException("No primary device available for snapshot recovery");
        }

        var recoveryRequest = new PeerDataOperationRequestMessageSyncDCollectionFatalRecoveryRequestBuilder()
                .collectionName(collectionName.toString())
                .timestamp(Instant.now())
                .build();

        var requestMessage = new PeerDataOperationRequestMessageBuilder()
                .peerDataOperationRequestType(PeerDataOperationRequestType.COMPANION_SYNCD_SNAPSHOT_FATAL_RECOVERY)
                .syncdCollectionFatalRecoveryRequest(recoveryRequest)
                .build();

        var protocolMessage = new ProtocolMessageBuilder()
                .type(ProtocolMessage.Type.PEER_DATA_OPERATION_REQUEST_MESSAGE)
                .peerDataOperationRequestMessage(requestMessage)
                .build();

        var messageContainer = new LinkedMessageContainerBuilder()
                .protocolMessage(protocolMessage)
                .build();

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending snapshot recovery request for collection {0} to primary device {1}", collectionName, primaryDevice);
        var self = client.store().accountStore().jid().orElse(null);
        if (self == null) {
            throw new IllegalStateException("Own JID not available for snapshot recovery request");
        }

        var peerMessageId = MessageIdGenerator.generate(MessageIdVersion.V2, self);
        var messageKey = new MessageKeyBuilder()
                .id(peerMessageId)
                .parentJid(self)
                .fromMe(true)
                .senderJid(self)
                .build();
        var messageInfo = new ChatMessageInfoBuilder()
                .key(messageKey)
                .message(messageContainer)
                .build();
        wamService.commit(new NonMessagePeerDataRequestEventBuilder()
                .peerDataRequestCount(1)
                .peerDataRequestType(PeerDataRequestType.SYNCD_SNAPSHOT_RECOVERY)
                .peerDataRequestSessionId(peerMessageId)
                .build());
        client.sendPeerMessage(primaryDevice, messageInfo);
    }

    /**
     * Builds the device-zero JID of the primary device.
     *
     * <p>Resolves the destination of the recovery peer-data operation request
     * for {@link #sendRecoveryRequest(SyncPatchType)}.
     *
     * @return the primary device JID, or {@code null} when the local user's
     *         JID has not been set yet
     */
    @WhatsAppWebExport(moduleName = "WAWebSendNonMessageDataRequest", exports = "D", adaptation = WhatsAppAdaptation.ADAPTED)
    private Jid getPrimaryDevice() {
        var myJid = client.store().accountStore().jid().orElse(null);
        if (myJid == null) {
            return null;
        }

        return Jid.of(myJid.user(), myJid.server(), 0, 0);
    }
}
