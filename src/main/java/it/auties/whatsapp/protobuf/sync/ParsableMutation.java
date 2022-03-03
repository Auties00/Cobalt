package it.auties.whatsapp.protobuf.sync;

import it.auties.bytes.Bytes;

public sealed interface ParsableMutation extends GenericSync permits MutationSync, RecordSync {
    byte[] id();
    Bytes valueBlob();
    Bytes indexBlob();
}
