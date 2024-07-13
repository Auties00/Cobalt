package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.BYTES;

@ProtobufMessage(name = "HandshakeMessage.ClientFinish")
public record ClientFinish(@ProtobufProperty(index = 1, type = BYTES) byte[] _static,
                           @ProtobufProperty(index = 2, type = BYTES) byte[] payload) {
}
