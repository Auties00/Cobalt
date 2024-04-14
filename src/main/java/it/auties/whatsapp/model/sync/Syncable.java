package it.auties.whatsapp.model.sync;

public sealed interface Syncable permits RecordSync, MutationSync {
    RecordSync.Operation operation();

    RecordSync record();
}
