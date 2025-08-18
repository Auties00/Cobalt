package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.BYTES;

@ProtobufMessage(name = "HandshakeMessage.ServerHello")
public record ServerHello(@ProtobufProperty(index = 1, type = BYTES) byte[] ephemeral,
                          @ProtobufProperty(index = 2, type = BYTES) byte[] staticText,
                          @ProtobufProperty(index = 3, type = BYTES) byte[] payload) {
}
