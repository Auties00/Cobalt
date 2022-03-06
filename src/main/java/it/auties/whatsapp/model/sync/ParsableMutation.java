package it.auties.whatsapp.model.sync;

import it.auties.bytes.Bytes;

public sealed interface ParsableMutation extends GenericSync permits MutationSync, RecordSync {
    byte[] id();
    Bytes valueBlob();
    Bytes indexBlob();
}
