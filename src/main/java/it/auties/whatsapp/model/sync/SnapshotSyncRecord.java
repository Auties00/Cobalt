package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.binary.Sync;

import java.util.List;

public record SnapshotSyncRecord(Sync name, SnapshotSync snapshot, List<PatchSync> patches, boolean hasMore) {
    public boolean hasSnapshot() {
        return snapshot != null;
    }

    public boolean hasPatches() {
        return patches != null && !patches.isEmpty();
    }
}
