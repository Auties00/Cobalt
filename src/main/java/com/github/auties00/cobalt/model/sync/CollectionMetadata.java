package com.github.auties00.cobalt.model.sync;

import java.util.Arrays;
import java.util.Objects;

/**
 * Metadata for a synced collection.
 *
 * <p>This record tracks the synchronization state of a single collection,
 * including its version number, LT-Hash for integrity verification,
 * current sync state, and retry information.
 *
 * @param name the collection name (e.g., "regular", "critical_block")
 * @param version the current version number (monotonically increasing)
 * @param ltHash the LT-Hash for anti-tampering verification (128 bytes)
 * @param lastSyncTimestamp the timestamp of the last successful sync (Unix millis)
 * @param state the current synchronization state
 * @param retryCount the number of retry attempts (for error states)
 * @param lastErrorTimestamp the timestamp of the last error (Unix millis)
 */
public record CollectionMetadata(
        PatchType name,
        long version,
        byte[] ltHash,
        long lastSyncTimestamp,
        CollectionState state,
        int retryCount,
        long lastErrorTimestamp
) {
    /**
     * Creates a new CollectionMetadata with validation.
     *
     * @throws NullPointerException if name, ltHash, or state is null
     * @throws IllegalArgumentException if version is negative or ltHash is not 128 bytes
     */
    public CollectionMetadata {
        if (name == null) {
            throw new NullPointerException("Collection name cannot be null");
        }
        if (ltHash == null) {
            throw new NullPointerException("LT-Hash cannot be null");
        }
        if (state == null) {
            throw new NullPointerException("Collection state cannot be null");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative: " + version);
        }
        if (ltHash.length != 128) {
            throw new IllegalArgumentException("LT-Hash must be 128 bytes, got " + ltHash.length);
        }
        if (retryCount < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative: " + retryCount);
        }
    }

    /**
     * Creates a copy with incremented retry count.
     *
     * @return a new metadata with retry count + 1
     */
    public CollectionMetadata incrementRetry() {
        return new CollectionMetadata(
                name,
                version,
                ltHash,
                lastSyncTimestamp,
                state,
                retryCount + 1,
                System.currentTimeMillis()
        );
    }

    /**
     * Creates a copy with reset retry count.
     *
     * @return a new metadata with retry count = 0
     */
    public CollectionMetadata resetRetry() {
        return new CollectionMetadata(
                name,
                version,
                ltHash,
                lastSyncTimestamp,
                state,
                0,
                0
        );
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CollectionMetadata(var thatName, var thatVersion, var thatHash, var thatSyncTimestamp, var thatState, var thatCount, var thatErrorTimestamp)
               && version == thatVersion
               && retryCount == thatCount
               && lastSyncTimestamp == thatSyncTimestamp
               && lastErrorTimestamp == thatErrorTimestamp
               && Objects.equals(name, thatName)
               && Objects.deepEquals(ltHash, thatHash)
               && state == thatState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, Arrays.hashCode(ltHash), lastSyncTimestamp, state, retryCount, lastErrorTimestamp);
    }
}
