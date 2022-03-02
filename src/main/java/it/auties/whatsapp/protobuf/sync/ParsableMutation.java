package it.auties.whatsapp.protobuf.sync;

import it.auties.buffer.ByteBuffer;

public sealed interface ParsableMutation extends GenericSync permits MutationSync, RecordSync {
    byte[] id();
    ByteBuffer valueBlob();
    ByteBuffer indexBlob();
}
