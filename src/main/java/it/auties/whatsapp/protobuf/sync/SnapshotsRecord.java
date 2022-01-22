package it.auties.whatsapp.protobuf.sync;

import java.util.List;

public record SnapshotsRecord(LTHashState state, List<MutationsRecord> mutations) {
}
