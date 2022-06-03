package it.auties.whatsapp.model.sync;

import java.util.List;

public record SnapshotsRecord(LTHashState state, List<MutationsRecord> mutations) {
}
