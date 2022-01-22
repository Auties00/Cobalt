package it.auties.whatsapp.protobuf.sync;

import java.util.List;

public record SnapshotRecord(LTHashState state, MutationsRecord mutations) {
}
