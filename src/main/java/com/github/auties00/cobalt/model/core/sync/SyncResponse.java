package com.github.auties00.cobalt.model.core.sync;

import com.github.auties00.cobalt.model.proto.sync.PatchSync;
import com.github.auties00.cobalt.model.proto.sync.PatchType;
import com.github.auties00.cobalt.model.proto.sync.SnapshotSync;

import java.util.SequencedCollection;

/**
 * Parsed sync response from the server.
 *
 * @param collectionName the collection name
 * @param version        the server version
 * @param hasMore        whether more patches are available
 * @param patches        list of patches (empty if snapshot provided)
 * @param snapshot       the snapshot (null if patches provided)
 */
public record SyncResponse(
        PatchType collectionName,
        long version,
        boolean hasMore,
        SequencedCollection<PatchSync> patches,
        SnapshotSync snapshot
) {
    /**
     * Returns whether this is a snapshot response.
     *
     * @return true if snapshot, false if patches
     */
    public boolean isSnapshot() {
        return snapshot != null;
    }
}
