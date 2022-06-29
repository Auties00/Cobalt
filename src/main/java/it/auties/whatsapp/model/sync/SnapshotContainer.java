package it.auties.whatsapp.model.sync;

import java.util.List;

public record SnapshotContainer(List<SnapshotSyncRecord> records, boolean complete) {
}
