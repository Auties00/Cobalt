package it.auties.whatsapp.model.call;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

@ProtobufMessage
public record Call(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid chat,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        Jid caller,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
        long timestampSeconds,
        @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
        boolean video,
        @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
        CallStatus status,
        @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
        boolean offline
) {

}