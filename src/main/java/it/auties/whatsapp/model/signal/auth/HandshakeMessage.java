package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessage(name = "HandshakeMessage")
public record HandshakeMessage(@ProtobufProperty(index = 2, type = OBJECT) ClientHello clientHello,
                               @ProtobufProperty(index = 3, type = OBJECT) ServerHello serverHello,
                               @ProtobufProperty(index = 4, type = OBJECT) ClientFinish clientFinish) {
}
