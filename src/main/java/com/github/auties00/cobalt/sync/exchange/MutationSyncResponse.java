package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.model.sync.PatchSync;
import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.model.sync.SnapshotSync;

import java.util.SequencedCollection;

public record MutationSyncResponse(
        PatchType collectionName,
        long version,
        boolean hasMore,
        SequencedCollection<PatchSync> patches,
        SnapshotSync snapshot
) {
    public boolean isSnapshot() {
        return snapshot != null;
    }
}
