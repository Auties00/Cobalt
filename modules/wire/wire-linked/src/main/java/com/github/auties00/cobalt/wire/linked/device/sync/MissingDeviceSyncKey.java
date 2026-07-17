package com.github.auties00.cobalt.wire.linked.device.sync;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a Syncd encryption key that is referenced by an incoming app-state
 * patch but is not present in the local key cache, together with the set of
 * companion devices that have been queried for it.
 *
 * <p>App-state mutations (archive, mute, pin, label, privacy, and so on) are
 * encrypted with rotating Syncd keys. When the server ships a patch whose key
 * identifier is not known locally, the client must ask its other companion
 * devices (phone, secondary browsers, desktop clients) to share that key.
 * A {@code MissingDeviceSyncKey} instance records:
 *
 * <ul>
 *   <li>which key is missing (the raw key identifier bytes),</li>
 *   <li>when it was first seen as missing, for expiry tracking,</li>
 *   <li>which companion device indexes have already been asked, and</li>
 *   <li>which of those devices replied that they also do not have the key.</li>
 * </ul>
 *
 * <p>The entry is resolved and removed as soon as any device returns the key.
 * If every asked device responds with "key not found", the key is considered
 * missing on the entire multi-device cluster and Cobalt raises a fatal Syncd
 * error so that a full snapshot recovery can be triggered.
 *
 * <p>The two device sets are backed by {@link ConcurrentHashMap#newKeySet()},
 * so instances are safe to update concurrently from multiple virtual threads
 * handling simultaneous device responses.
 */
@ProtobufMessage
public final class MissingDeviceSyncKey {
    /**
     * The raw Syncd key identifier that could not be resolved locally.
     *
     * <p>This is the opaque byte sequence carried inside an app-state patch
     * header and used as the primary lookup key in the Syncd key store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final byte[] keyId;

    /**
     * Instant at which the key was first detected as missing.
     *
     * <p>Used to determine when the key has been missing for longer than the
     * configured "wait for key" timeout, at which point the entry is considered
     * expired and the client gives up and reports a fatal Syncd error.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    final Instant timestamp;

    /**
     * Device indexes that have been asked whether they hold the missing key.
     *
     * <p>Populated as "key-share" requests are dispatched to companion devices.
     * A device index is a small integer identifying a single client inside the
     * account's multi-device cluster (index {@code 0} is the primary device,
     * higher indexes are linked companions).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32, packed = true)
    final Set<Integer> askedDevices;

    /**
     * Subset of {@link #askedDevices} whose replies indicated they also do not
     * have the key.
     *
     * <p>When this set becomes equal to {@link #askedDevices} every queried
     * device has reported the key as missing and there is no companion left
     * that can share it. The entry is then treated as a fatal condition.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32, packed = true)
    final Set<Integer> respondedWithoutKey;

    /**
     * Creates a new missing-key record.
     *
     * <p>The supplied device collections are defensively copied into
     * concurrent sets, so callers are free to mutate or discard the originals
     * after construction. Either or both device collections may be
     * {@code null}, in which case the corresponding set starts out empty.
     *
     * @param keyId               the raw Syncd key identifier
     * @param timestamp           the instant at which the key was first observed as missing
     * @param askedDevices        device indexes that have already been queried, or {@code null}
     * @param respondedWithoutKey device indexes that have replied with no key, or {@code null}
     */
    MissingDeviceSyncKey(byte[] keyId, Instant timestamp, Set<Integer> askedDevices, Set<Integer> respondedWithoutKey) {
        this.keyId = keyId;
        this.timestamp = timestamp;
        this.askedDevices = ConcurrentHashMap.newKeySet();
        this.respondedWithoutKey = ConcurrentHashMap.newKeySet();
        if (askedDevices != null) {
            this.askedDevices.addAll(askedDevices);
        }
        if (respondedWithoutKey != null) {
            this.respondedWithoutKey.addAll(respondedWithoutKey);
        }
    }

    /**
     * Returns the raw Syncd key identifier that this entry is tracking.
     *
     * <p>The returned array is the backing storage, not a defensive copy,
     * and must be treated as read-only by callers.
     *
     * @return the key identifier bytes
     */
    public byte[] keyId() {
        return keyId;
    }

    /**
     * Returns the instant at which the key was first recorded as missing.
     *
     * @return the detection timestamp
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Records that a key-share request has been dispatched to the companion
     * device with the given index.
     *
     * <p>Subsequent calls for the same device are idempotent.
     *
     * @param deviceId the index of the companion device that was queried
     */
    public void markDeviceAsked(int deviceId) {
        askedDevices.add(deviceId);
    }

    /**
     * Returns whether the given companion device has already been queried for
     * this key.
     *
     * <p>Useful to avoid sending duplicate key-share requests to the same
     * device during the wait window.
     *
     * @param deviceId the index of the companion device to check
     * @return {@code true} if the device has been queried, {@code false} otherwise
     */
    public boolean wasAsked(int deviceId) {
        return askedDevices.contains(deviceId);
    }

    /**
     * Records that the given companion device replied that it also does not
     * have the key.
     *
     * <p>When every queried device has been marked in this way the entry is
     * considered missing on all clients and should be treated as a fatal
     * Syncd error.
     *
     * @param deviceId the index of the companion device whose reply has arrived
     */
    public void markDeviceRespondedWithoutKey(int deviceId) {
        respondedWithoutKey.add(deviceId);
    }

    /**
     * Returns whether the given companion device has already replied that it
     * does not have the key.
     *
     * @param deviceId the index of the companion device to check
     * @return {@code true} if the device responded without the key, {@code false} otherwise
     */
    public boolean hasDeviceRespondedWithoutKey(int deviceId) {
        return respondedWithoutKey.contains(deviceId);
    }

    /**
     * Drops all traces of the given device from both the asked and the
     * responded-without-key sets.
     *
     * <p>Invoked when a companion device is unlinked from the account so
     * that stale indexes do not prevent the missing-key workflow from
     * resolving.
     *
     * @param deviceId the index of the companion device to forget
     */
    public void removeDevice(int deviceId) {
        askedDevices.remove(deviceId);
        respondedWithoutKey.remove(deviceId);
    }

    /**
     * Prunes both the asked and the responded-without-key sets so that only
     * entries for devices still present in the account remain.
     *
     * <p>Called after the companion device list is refreshed (for example
     * when a device is removed or re-linked) to keep this record consistent
     * with the current cluster.
     *
     * @param currentDeviceIds the device indexes that currently exist in the account
     */
    public void retainDevices(Set<Integer> currentDeviceIds) {
        askedDevices.retainAll(currentDeviceIds);
        respondedWithoutKey.retainAll(currentDeviceIds);
    }

    /**
     * Returns whether every companion device that has been queried replied
     * that it does not have the key.
     *
     * <p>A {@code true} result means the key is missing on the entire
     * multi-device cluster, which is a fatal Syncd condition: the usual
     * remediation is to discard the Syncd state and request a full
     * snapshot recovery. The method returns {@code false} when no device
     * has been asked yet, to avoid flagging a pristine entry as fatal.
     *
     * @return {@code true} if at least one device has been asked and all
     *         asked devices responded without the key, {@code false} otherwise
     */
    public boolean isMissingOnAllDevices() {
        return !askedDevices.isEmpty()
                && askedDevices.equals(respondedWithoutKey);
    }

    /**
     * Returns whether at least one queried device has not yet responded.
     *
     * <p>Used by the waiting loop to decide whether it is still worth
     * staying in the "waiting for key" state or whether the outcome can
     * already be determined.
     *
     * @return {@code true} if some asked device has not yet replied,
     *         {@code false} when every asked device has responded
     */
    public boolean hasPendingResponses() {
        return askedDevices.size() > respondedWithoutKey.size();
    }
}
