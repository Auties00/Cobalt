package com.github.auties00.cobalt.model.sync;

public sealed interface Syncable permits RecordSync, MutationSync {
    RecordSync.Operation operation();

    RecordSync record();
}
