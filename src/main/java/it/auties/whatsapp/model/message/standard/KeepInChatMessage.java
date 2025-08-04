package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.KeepInChat;
import it.auties.whatsapp.model.message.model.Message;

import java.util.Objects;

@ProtobufMessage(name = "Message.KeepInChatMessage")
public final class KeepInChatMessage implements Message {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey key;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final KeepInChat.Type keepType;

    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long timestampMilliseconds;

    KeepInChatMessage(ChatMessageKey key, KeepInChat.Type keepType, long timestampMilliseconds) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.keepType = Objects.requireNonNull(keepType, "keepType cannot be null");
        this.timestampMilliseconds = timestampMilliseconds;
    }

    public ChatMessageKey key() {
        return key;
    }

    public KeepInChat.Type keepType() {
        return keepType;
    }

    public long timestampMilliseconds() {
        return timestampMilliseconds;
    }

    @Override
    public Type type() {
        return Type.KEEP_IN_CHAT;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KeepInChatMessage that
                && Objects.equals(key, that.key)
                && Objects.equals(keepType, that.keepType)
                && timestampMilliseconds == that.timestampMilliseconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, keepType, timestampMilliseconds);
    }

    @Override
    public String toString() {
        return "KeepInChatMessage[" +
                "key=" + key +
                ", keepType=" + keepType +
                ", timestampMilliseconds=" + timestampMilliseconds +
                ']';
    }
}