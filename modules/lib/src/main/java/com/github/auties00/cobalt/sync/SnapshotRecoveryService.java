package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestResponseMessage;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdSnapshotRecovery;

import java.util.NoSuchElementException;

/**
 * Drives the companion-to-primary syncd snapshot recovery peer-data operation that lets a
 * companion ask the primary device for a corrected snapshot when its local snapshot MAC fails to
 * validate.
 *
 * <p>The service is invoked by {@link WebAppStateService} as part of the syncd patch pipeline,
 * after a {@link WhatsAppWebAppStateSyncException} marked as fatal is raised; embedders never call
 * it directly. Recovery covers a snapshot mismatch on a non-{@link SyncPatchType#CRITICAL_BLOCK}
 * collection when the AB-prop gate is on, the primary device supports recovery, and the mutation
 * count is within the configured budget.
 *
 * @implSpec
 * Implementations must serialise concurrent recovery requests, must enforce an upper bound on a
 * single recovery call, and must return {@code null} from {@link #requestRecovery(SyncPatchType)}
 * on any failure so the caller can fall back to a full re-link.
 */
public interface SnapshotRecoveryService {
    /**
     * Returns whether snapshot recovery is enabled for this device.
     *
     * <p>Both the persisted primary-device support flag and the
     * recovery-enabled AB-prop must be set for recovery to be attempted.
     *
     * @implSpec
     * Implementations must require both the primary-device support flag and the AB-prop gate.
     *
     * @return {@code true} when both the primary-device support flag and the
     *         AB-prop are enabled
     */
    boolean isRecoveryEnabled();

    /**
     * Persists the flag indicating whether the primary device supports syncd
     * snapshot recovery.
     *
     * <p>Called from the device-capabilities ingest path when the primary
     * advertises (or stops advertising) recovery support; the flag is read
     * back by {@link #isRecoveryEnabled()}.
     *
     * @implSpec
     * Implementations must persist {@code supported} so a later {@link #isRecoveryEnabled()}
     * observes it.
     *
     * @param supported {@code true} when the primary supports recovery
     */
    void updatePrimaryDeviceSupportsSyncdRecovery(boolean supported);

    /**
     * Returns whether a recovery attempt should be made for the given
     * collection.
     *
     * <p>Composes {@link #isRecoveryEnabled()} with the collection-name and
     * mutation-count gates: recovery is rejected when the global gate is off,
     * when the collection is {@link SyncPatchType#CRITICAL_BLOCK}, or when the
     * mutation count is strictly greater than the configured maximum.
     *
     * @implSpec
     * Implementations must reject {@link SyncPatchType#CRITICAL_BLOCK} and any mutation count
     * above the configured maximum, and must require {@link #isRecoveryEnabled()}.
     *
     * @param collectionName the collection that failed snapshot validation
     * @param mutationCount  the number of decrypted mutations in the failing
     *                       snapshot
     * @return {@code true} when recovery should be attempted
     */
    boolean shouldAttemptRecovery(SyncPatchType collectionName, int mutationCount);

    /**
     * Sends a recovery request for {@code collectionName} and blocks until
     * the primary device responds or the timeout elapses.
     *
     * <p>Called by {@link WebAppStateService} when an incoming snapshot fails
     * validation and {@link #shouldAttemptRecovery(SyncPatchType, int)} has
     * already returned {@code true}. Returns {@code null} on any failure
     * (timeout, semaphore-acquisition timeout, primary error) so the caller
     * can fall back to a full re-link.
     *
     * @implSpec
     * Implementations must block until the primary responds or the per-call timeout elapses and
     * must return {@code null} on any failure.
     *
     * @param collectionName the collection to recover
     * @return the decoded snapshot returned by the primary, or {@code null}
     *         on any failure
     */
    SyncdSnapshotRecovery requestRecovery(SyncPatchType collectionName);

    /**
     * Completes the in-flight recovery future for {@code collectionName} with
     * the decoded snapshot.
     *
     * <p>Called by the protocol-message receiver when a peer-data operation
     * response of the snapshot fatal-recovery type arrives; the receiver
     * decodes the snapshot via
     * {@link #decodeRecoverySnapshot(PeerDataOperationRequestResponseMessage.PeerDataOperationResult.SyncDSnapshotFatalRecoveryResponse)}
     * first and forwards the decoded value here. A missing future is dropped.
     *
     * @implSpec
     * Implementations must complete a matching in-flight {@link #requestRecovery(SyncPatchType)}
     * and must silently drop a response with no pending request.
     *
     * @param collectionName    the collection name carried by the decoded
     *                          snapshot
     * @param recoveredSnapshot the decoded {@link SyncdSnapshotRecovery}
     */
    void resolveRecovery(SyncPatchType collectionName, SyncdSnapshotRecovery recoveredSnapshot);

    /**
     * Decodes a snapshot fatal-recovery response payload into a
     * {@link SyncdSnapshotRecovery}.
     *
     * <p>Called by the protocol-message receiver before invoking
     * {@link #resolveRecovery(SyncPatchType, SyncdSnapshotRecovery)} so that
     * the same decoded value is shared between the resolver and any
     * downstream consumer. Gzip-compressed responses are decompressed transparently.
     *
     * @implSpec
     * Implementations must transparently decompress a gzip-compressed response.
     *
     * @param response the raw snapshot fatal-recovery response carried by the
     *                 peer-data operation response
     * @return the decoded {@link SyncdSnapshotRecovery}
     * @throws NoSuchElementException                                if the
     *         response carries no {@code collectionSnapshot} bytes
     * @throws WhatsAppWebAppStateSyncException.ExternalDecodeFailed if the
     *         bytes cannot be decoded
     */
    SyncdSnapshotRecovery decodeRecoverySnapshot(
            PeerDataOperationRequestResponseMessage.PeerDataOperationResult.SyncDSnapshotFatalRecoveryResponse response
    );
}
