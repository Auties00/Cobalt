package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.*;


@ProtobufMessageName("Message.KeepInChatMessage")
public record KeepInChatMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        ChatMessageKey key,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        KeepInChatType keepType,
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long timestampMilliseconds
) implements Message {
    @Override
    public MessageType type() {
        return MessageType.KEEP_IN_CHAT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}
