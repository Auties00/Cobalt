package it.auties.whatsapp.protobuf.sync;

import it.auties.whatsapp.binary.BinaryArray;

public interface ParsableMutation extends GenericSync {
    byte[] id();
    BinaryArray valueBlob();
    BinaryArray indexBlob();
}
