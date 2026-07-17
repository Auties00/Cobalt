package com.github.auties00.cobalt.sync.key;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.wire.wam.type.BootstrapAppStateDataStageCode;

import java.util.List;

/**
 * Manages the lifecycle of the active app state sync key: detects expiry, detects companion
 * device removal, mints fresh keys with a monotonically increasing epoch, and shares them with
 * every peer device.
 *
 * <p>Two entry points dominate. {@link #getActiveKey(boolean)} resolves the current key for
 * outgoing patches and rotates inline when a rotation trigger fires. {@link #handleKeyShare(int, List)}
 * consumes inbound {@code AppStateSyncKeyShare} {@link ProtocolMessage} payloads from companion
 * devices and reconciles them against the local key store and the missing-key tracker. A periodic
 * 27-day background check ({@link #startPeriodicRotationJob()}) ensures rotation also happens
 * during long mutation-free windows.
 *
 * @implSpec
 * Implementations must keep the read-then-rotate-then-store sequence of
 * {@link #getActiveKey(boolean)} atomic against concurrent encrypt callers, and must mint each
 * fresh key with a monotonically increasing epoch.
 */
public interface SyncKeyRotationService {
    /**
     * Reconciles an incoming {@code AppStateSyncKeyShare} protocol message against the local
     * sync key store and missing-key tracker.
     *
     * <p>Invoked by the message stream handler when an
     * {@link ProtocolMessage.Type#APP_STATE_SYNC_KEY_SHARE} message is received. Each shared key
     * is either a positive response carrying key data, which is added to the store and clears any
     * matching missing-key tracker entry, or a negative response without key data, which records
     * the sender as having answered without the key. When a tracker entry then has a no-key answer
     * from every asked device the all-devices-responded grace check is scheduled via
     * {@link WebAppStateService#scheduleAllDevicesRespondedCheck()}. When any tracker entry is
     * resolved the wait-for-key timeout is rescheduled. After reconciliation any blocked syncd
     * collections are unblocked and resynced.
     *
     * @implSpec
     * Implementations must store positive shares, mark negative shares against the tracker,
     * schedule the all-devices-responded check when a tracker entry is exhausted, and resync any
     * blocked collections afterwards.
     *
     * @param senderDeviceId the device id of the peer that sent the share
     * @param keys the keys carried by the share message
     */
    void handleKeyShare(int senderDeviceId, List<AppStateSyncKey> keys);

    /**
     * Emits the {@link BootstrapAppStateDataStageCode#MISSING_KEYS_RECEIVED} bootstrap-stage event
     * from the validation half of the inbound key-share path.
     *
     * <p>The message stream handler validates an inbound {@code AppStateSyncKeyShare} on its own
     * thread before delegating storage to {@link #handleKeyShare(int, List)}. This method is
     * exposed so the validator can fire the bootstrap beacon at the correct call position
     * regardless of whether any shared key survives validation.
     *
     * @implSpec
     * Implementations must emit the beacon only while the critical-data sync is still in progress.
     */
    void logMissingKeysReceived();

    /**
     * Ensures the active sync key is fit for use, rotating inline when {@code triggerRotation} is
     * set and the active key is stale.
     *
     * <p>Convenience wrapper for code paths that need only the side effect of rotation without
     * consuming the resulting key reference, such as the periodic 27-day background check.
     * Equivalent to discarding the return value of {@link #getActiveKey(boolean)}.
     *
     * @implSpec
     * Implementations must behave as a {@link #getActiveKey(boolean)} call whose result is
     * discarded.
     *
     * @param triggerRotation whether to rotate when the active key is stale
     */
    void ensureActiveKey(boolean triggerRotation);

    /**
     * Returns the active sync key, rotating inline when {@code triggerRotation} is {@code true}
     * and the newest key is either expired or carries a stale device fingerprint.
     *
     * <p>Called before encrypting outgoing patches and by the periodic background check in
     * {@link #startPeriodicRotationJob()}. A {@code triggerRotation} of {@code false} is the
     * read-only mode that returns the existing key without inducing rotation.
     *
     * @implSpec
     * Implementations must rotate only when {@code triggerRotation} is set and the newest key is
     * expired or carries a stale device fingerprint, and must keep the read-rotate-store sequence
     * atomic.
     *
     * @param triggerRotation whether to rotate when the active key is stale
     * @return the active {@link AppStateSyncKey}
     * @throws IllegalStateException when no sync key exists at all
     */
    AppStateSyncKey getActiveKey(boolean triggerRotation);

    /**
     * Returns the newest sync key pair from the store.
     *
     * <p>Reads the store's available keys and selects the newest. The read-only contract makes it
     * cheap to use as a precondition check before invoking {@link #getActiveKey(boolean)}.
     *
     * @implSpec
     * Implementations must not rotate and must return {@code null} when no keys exist.
     *
     * @return the newest {@link AppStateSyncKey}, or {@code null} when no keys exist
     */
    AppStateSyncKey getNewestKeyPair();

    /**
     * Sends a missing-key share back to a peer that previously requested it.
     *
     * <p>Called by the inbound {@code AppStateSyncKeyRequest} handler when this client holds the
     * requested keys. The response carries any keys that were not found locally as orphan key ids
     * so the requester can mark them missing and stop asking.
     *
     * @implSpec
     * Implementations must include {@code orphanKeyIds} as data-less entries so the requester can
     * mark them missing.
     *
     * @param keys the keys this client holds for the peer's request
     * @param orphanKeyIds the requested key ids that were not found locally
     * @param peerDeviceJid the peer device that issued the request
     */
    void sendMissingKeyShare(List<AppStateSyncKey> keys, List<byte[]> orphanKeyIds, Jid peerDeviceJid);

    /**
     * Starts the periodic 27-day background rotation check.
     *
     * <p>Ensures a session that has not pushed any mutations for weeks, and therefore has not
     * invoked {@link #getActiveKey(boolean)} on the encrypt path, still rotates expired keys on
     * schedule. Any previously running job is cancelled before scheduling a new one so the service
     * is idempotent across reconnects.
     *
     * @implSpec
     * Implementations must cancel any previously running job before scheduling a new one.
     */
    void startPeriodicRotationJob();

    /**
     * Cancels the periodic 27-day rotation check, if any.
     *
     * <p>Called by {@link WebAppStateService} on disconnect; the partner of
     * {@link #startPeriodicRotationJob()}.
     *
     * @implSpec
     * Implementations must tolerate being called when no job is running.
     */
    void stopPeriodicRotationJob();
}
