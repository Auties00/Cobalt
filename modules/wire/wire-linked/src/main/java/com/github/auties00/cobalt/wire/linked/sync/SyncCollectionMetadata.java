package com.github.auties00.cobalt.wire.linked.sync;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Per-collection bookkeeping container for app-state sync.
 *
 * <p>App-state mutations are partitioned across a fixed family of
 * collections (see {@link SyncPatchType}). For each collection the
 * client maintains a small set of bookkeeping fields used to drive the
 * sync state machine: the current monotonic version counter, the
 * 128-byte LT-Hash that lets the server detect tampering or missed
 * mutations, the timestamps of the last successful sync and the last
 * observed error, the current lifecycle {@link SyncCollectionState}, a
 * retry counter, a sticky MAC-mismatch flag, and a "bootstrapped" flag
 * that lets the client tell apart "never synced yet" from "synced and
 * empty".
 */
@ProtobufMessage
public final class SyncCollectionMetadata {
    /**
     * The identifier of the collection this metadata describes.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    SyncPatchType name;

    /**
     * The current version counter, monotonically increasing as
     * mutations are applied.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    long version;

    /**
     * The current 128-byte LT-Hash used for tamper detection against
     * the server hash.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] ltHash;

    /**
     * The instant of the most recent successful sync. Encoded on the
     * wire as a 64-bit millisecond epoch via
     * {@link InstantMillisMixin}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant lastSyncTimestamp;

    /**
     * The current state in the collection sync lifecycle.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    SyncCollectionState state;

    /**
     * The number of consecutive retry attempts since the last error.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT32)
    int retryCount;

    /**
     * The instant of the most recent observed error. Encoded on the
     * wire as a 64-bit millisecond epoch via
     * {@link InstantMillisMixin}.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant lastErrorTimestamp;

    /**
     * Whether a snapshot MAC mismatch has been observed; sticky across
     * state transitions.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    boolean macMismatch;

    /**
     * Whether this collection has completed at least one sync round.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    boolean bootstrapped;

    /**
     * Constructs a new {@code SyncCollectionMetadata} with the supplied
     * field values.
     *
     * @param name               the identifier of the collection
     * @param version            the current version counter
     * @param ltHash             the current 128-byte LT-Hash
     * @param lastSyncTimestamp  the instant of the most recent
     *                           successful sync, or {@code null} if
     *                           never synced
     * @param state              the current sync lifecycle state
     * @param retryCount         the consecutive retry attempt count
     * @param lastErrorTimestamp the instant of the most recent observed
     *                           error, or {@code null} if no error has
     *                           been observed
     * @param macMismatch        the sticky MAC-mismatch flag
     * @param bootstrapped       the bootstrapped flag
     */
    SyncCollectionMetadata(SyncPatchType name, long version, byte[] ltHash, Instant lastSyncTimestamp, SyncCollectionState state, int retryCount, Instant lastErrorTimestamp, boolean macMismatch, boolean bootstrapped) {
        this.name = name;
        this.version = version;
        this.ltHash = ltHash;
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.state = state;
        this.retryCount = retryCount;
        this.lastErrorTimestamp = lastErrorTimestamp;
        this.macMismatch = macMismatch;
        this.bootstrapped = bootstrapped;
    }

    /**
     * Returns the identifier of the collection this metadata describes.
     *
     * @return the collection identifier
     */
    public SyncPatchType name() {
        return name;
    }

    /**
     * Returns the current version counter, monotonically increasing as
     * mutations are applied.
     *
     * @return the current version counter
     */
    public long version() {
        return version;
    }

    /**
     * Returns the current 128-byte LT-Hash used for tamper detection
     * against the server hash.
     *
     * @return the LT-Hash bytes
     */
    public byte[] ltHash() {
        return ltHash;
    }

    /**
     * Returns the instant of the most recent successful sync, if any.
     *
     * @return an {@code Optional} containing the last sync instant, or
     *         empty if this collection has never synced
     */
    public Optional<Instant> lastSyncTimestamp() {
        return Optional.ofNullable(lastSyncTimestamp);
    }

    /**
     * Returns the current state in the collection sync lifecycle.
     *
     * @return the current sync state
     */
    public SyncCollectionState state() {
        return state;
    }

    /**
     * Returns the number of consecutive retry attempts since the last
     * error.
     *
     * @return the retry attempt counter
     */
    public int retryCount() {
        return retryCount;
    }

    /**
     * Returns the instant of the most recent observed error, if any.
     *
     * @return an {@code Optional} containing the last error instant, or
     *         empty if no error has been observed
     */
    public Optional<Instant> lastErrorTimestamp() {
        return Optional.ofNullable(lastErrorTimestamp);
    }

    /**
     * Returns whether a snapshot MAC mismatch has been observed for
     * this collection.
     *
     * @return {@code true} if a MAC mismatch is sticky on this
     *         collection, {@code false} otherwise
     */
    public boolean macMismatch() {
        return macMismatch;
    }

    /**
     * Returns whether this collection has completed at least one sync
     * round.
     *
     * @return {@code true} if the collection has been bootstrapped,
     *         {@code false} otherwise
     */
    public boolean bootstrapped() {
        return bootstrapped;
    }

    /**
     * Sets the identifier of the collection this metadata describes.
     *
     * @param name the new collection identifier
     */
    public void setName(SyncPatchType name) {
        this.name = name;
    }

    /**
     * Sets the current version counter.
     *
     * @param version the new version counter value
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Sets the current 128-byte LT-Hash bytes.
     *
     * @param ltHash the new LT-Hash bytes
     */
    public void setLtHash(byte[] ltHash) {
        this.ltHash = ltHash;
    }

    /**
     * Sets the instant of the most recent successful sync.
     *
     * @param lastSyncTimestamp the new last-sync instant, or
     *                          {@code null} to clear it
     */
    public void setLastSyncTimestamp(Instant lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }

    /**
     * Sets the current state in the collection sync lifecycle.
     *
     * @param state the new sync lifecycle state
     */
    public void setState(SyncCollectionState state) {
        this.state = state;
    }

    /**
     * Sets the number of consecutive retry attempts since the last
     * error.
     *
     * @param retryCount the new retry attempt count
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * Sets the instant of the most recent observed error.
     *
     * @param lastErrorTimestamp the new last-error instant, or
     *                           {@code null} to clear it
     */
    public void setLastErrorTimestamp(Instant lastErrorTimestamp) {
        this.lastErrorTimestamp = lastErrorTimestamp;
    }

    /**
     * Sets whether a snapshot MAC mismatch has been observed for this
     * collection.
     *
     * @param macMismatch the new MAC-mismatch flag
     */
    public void setMacMismatch(boolean macMismatch) {
        this.macMismatch = macMismatch;
    }

    /**
     * Sets whether this collection has completed at least one sync
     * round.
     *
     * @param bootstrapped the new bootstrapped flag
     */
    public void setBootstrapped(boolean bootstrapped) {
        this.bootstrapped = bootstrapped;
    }
}
