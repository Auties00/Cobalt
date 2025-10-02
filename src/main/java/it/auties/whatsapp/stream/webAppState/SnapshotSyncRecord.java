package it.auties.whatsapp.stream.webAppState;

import it.auties.whatsapp.model.sync.PatchSync;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.model.sync.SnapshotSync;

import java.util.List;

record SnapshotSyncRecord(PatchType patchType, SnapshotSync snapshot, List<PatchSync> patches,
                          boolean hasMore) {
    public boolean hasSnapshot() {
        return snapshot != null;
    }

    public boolean hasPatches() {
        return patches != null && !patches.isEmpty();
    }
}
