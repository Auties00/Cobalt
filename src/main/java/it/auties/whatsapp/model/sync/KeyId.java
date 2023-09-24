package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.BYTES;

@ProtobufMessageName("KeyId")
public record KeyId(@ProtobufProperty(index = 1, type = BYTES) byte[] id) implements ProtobufMessage {
}
