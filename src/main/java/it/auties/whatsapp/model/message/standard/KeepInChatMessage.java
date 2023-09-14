package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;


public record KeepInChatMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        MessageKey key,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
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
