package it.auties.whatsapp.protobuf.sync;

import it.auties.whatsapp.binary.BinaryArray;

public sealed interface ParsableMutation extends GenericSync permits MutationSync, RecordSync {
    byte[] id();
    BinaryArray valueBlob();
    BinaryArray indexBlob();
}
