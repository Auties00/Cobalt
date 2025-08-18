package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.MESSAGE;

@ProtobufMessage(name = "HandshakeMessage")
public record HandshakeMessage(@ProtobufProperty(index = 2, type = MESSAGE) ClientHello clientHello,
                               @ProtobufProperty(index = 3, type = MESSAGE) ServerHello serverHello,
                               @ProtobufProperty(index = 4, type = MESSAGE) ClientFinish clientFinish) {
}
