package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessageName("HandshakeMessage")
public record HandshakeMessage(@ProtobufProperty(index = 2, type = OBJECT) ClientHello clientHello,
                               @ProtobufProperty(index = 3, type = OBJECT) ServerHello serverHello,
                               @ProtobufProperty(index = 4, type = OBJECT) ClientFinish clientFinish) implements ProtobufMessage {
}
