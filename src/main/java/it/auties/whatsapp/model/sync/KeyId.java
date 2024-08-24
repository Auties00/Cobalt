package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.BYTES;

@ProtobufMessage(name = "KeyId")
public record KeyId(@ProtobufProperty(index = 1, type = BYTES) byte[] id) {
}
