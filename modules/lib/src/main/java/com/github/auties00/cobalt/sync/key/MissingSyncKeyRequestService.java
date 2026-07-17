package com.github.auties00.cobalt.sync.key;

import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.sync.WebAppStateService;

import java.util.Collection;

/**
 * Asks companion devices for app state sync keys whose key id was referenced by a snapshot or
 * patch but is absent from the local sync key store.
 *
 * <p>The syncd decrypt path invokes this service when patch or snapshot decryption fails against
 * a key id that is not in the local store. It filters away ids that are already tracked,
 * broadcasts an {@code AppStateSyncKeyRequest} {@link ProtocolMessage} to every companion device,
 * records per-device delivery outcomes against the missing-key tracker, and emits the
 * bootstrap-progress event that drives the critical-data spinner on the first-load screen. The
 * decrypt-time entry point {@link #requestMissingKeys(Collection)} is fire-and-forget; the
 * periodic entry point {@link #reRequestMissingKeys(Collection)} is driven by
 * {@link MissingSyncKeyTimeoutScheduler#startPeriodicReRequestJob()}.
 *
 * @implSpec
 * Implementations must drop {@code null} key ids, must broadcast to every companion device that
 * could hold the key, and must record the asked-device set against the missing-key tracker.
 */
public interface MissingSyncKeyRequestService {
    /**
     * Wires the timeout scheduler dependency after construction.
     *
     * <p>Must be invoked exactly once by {@link WebAppStateService} before
     * {@link #requestMissingKeys(Collection)} runs; otherwise the inline timeout reschedule that
     * follows each broadcast is silently skipped and the wait-for-key guarantee is lost. This
     * post-construction wiring breaks the circular dependency between this service and the
     * scheduler, which depends on this service for its periodic re-request job.
     *
     * @implSpec
     * Implementations must retain {@code timeoutScheduler} for the inline reschedule performed
     * after each tracked broadcast.
     *
     * @param timeoutScheduler the timeout scheduler to wire in
     */
    void setTimeoutScheduler(MissingSyncKeyTimeoutScheduler timeoutScheduler);

    /**
     * Requests the supplied missing app state sync key ids from companion devices.
     *
     * <p>Invoked from inside the syncd snapshot and patch decrypt paths whenever a key id fails
     * to resolve against the local sync key store. The call is fire-and-forget; the caller does
     * not await any companion response. {@code null} entries are dropped, and the resume guard
     * and already-tracked deduplication filter are applied before the broadcast.
     *
     * @implSpec
     * Implementations must skip the broadcast until offline resume completes, must drop already
     * tracked ids, and must track the surviving ids after broadcasting.
     *
     * @param keyIds the missing key ids; {@code null} entries are dropped
     */
    void requestMissingKeys(Collection<byte[]> keyIds);

    /**
     * Requests a single missing app state sync key id from companion devices.
     *
     * <p>Convenience overload for the case where a single decrypt failure surfaces only one id.
     *
     * @implSpec
     * Implementations must behave as a single-element {@link #requestMissingKeys(Collection)}.
     *
     * @param keyId the missing key id
     */
    void requestMissingKey(byte[] keyId);

    /**
     * Re-broadcasts an {@code AppStateSyncKeyRequest} for a previously tracked set of missing
     * key ids without re-running the resume guard or duplicating tracker entries.
     *
     * <p>Driven exclusively by {@link MissingSyncKeyTimeoutScheduler#startPeriodicReRequestJob()}
     * to recover keys when the original peer broadcast was dropped or a new companion has come
     * online since the original ask. An empty input is a no-op; {@code null} entries are dropped.
     *
     * @implSpec
     * Implementations must skip the resume guard and tracker deduplication and simply re-broadcast
     * the supplied ids.
     *
     * @param keyIds the missing key ids to re-broadcast; {@code null} entries are dropped
     */
    void reRequestMissingKeys(Collection<byte[]> keyIds);
}
