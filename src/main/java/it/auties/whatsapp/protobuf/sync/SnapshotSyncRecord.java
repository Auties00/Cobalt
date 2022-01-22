package it.auties.whatsapp.protobuf.sync;

import java.util.List;

public record SnapshotSyncRecord(String name, SnapshotSync snapshot, List<PatchSync> patches, boolean hasMore) {
    public boolean hasSnapshot(){
        return snapshot != null;
    }
}
