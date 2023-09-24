package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.BYTES;

@ProtobufMessageName("HandshakeMessage.ClientHello")
public record ClientHello(@ProtobufProperty(index = 1, type = BYTES) byte[] ephemeral,
                          @ProtobufProperty(index = 2, type = BYTES) byte[] _static,
                          @ProtobufProperty(index = 3, type = BYTES) byte[] payload) implements ProtobufMessage {
}
