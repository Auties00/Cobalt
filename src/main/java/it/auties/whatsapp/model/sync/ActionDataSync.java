package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.MessageIndexInfo;

import java.nio.charset.StandardCharsets;

@ProtobufMessage(name = "SyncActionData")
public record ActionDataSync(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] index,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        ActionValueSync value,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] padding,
        @ProtobufProperty(index = 4, type = ProtobufType.INT32)
        Integer version
) {
    public MessageIndexInfo messageIndex() {
        var jsonIndex = new String(index, StandardCharsets.UTF_8);
        return MessageIndexInfo.ofJson(jsonIndex);
    }
}