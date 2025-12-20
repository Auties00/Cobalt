package com.github.auties00.cobalt.model.sync;

import com.github.auties00.cobalt.model.message.model.ChatMessageKey;
import com.github.auties00.cobalt.model.message.model.MessageContainer;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "AIQueryFanout")
public final class AIQueryFanout {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey messageKey;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final MessageContainer message;

    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long timestamp;

    public AIQueryFanout(ChatMessageKey messageKey, MessageContainer message, long timestamp) {
        this.messageKey = messageKey;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Optional<ChatMessageKey> messageKey() {
        return Optional.ofNullable(messageKey);
    }

    public Optional<MessageContainer> message() {
        return Optional.ofNullable(message);
    }

    public long timestamp() {
        return timestamp;
    }
}
