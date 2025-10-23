package com.github.auties00.cobalt.model.proto.sync;

public sealed interface Syncable permits RecordSync, MutationSync {
    RecordSync.Operation operation();

    RecordSync record();
}
