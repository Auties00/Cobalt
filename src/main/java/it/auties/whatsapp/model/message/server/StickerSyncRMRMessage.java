package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;

import java.util.List;

@ProtobufMessage(name = "Message.StickerSyncRMRMessage")
public record StickerSyncRMRMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        List<String> hash,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String rmrSource,
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long requestTimestamp
) implements ServerMessage {
    @Override
    public MessageType type() {
        return MessageType.STICKER_SYNC;
    }
}
