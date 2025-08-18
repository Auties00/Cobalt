package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message holding an emoji reaction inside
 */
@ProtobufMessage(name = "Message.ReactionMessage")
public final class ReactionMessage implements Message {

    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey key;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String content;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String groupingKey;

    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final long timestampSeconds;

    ReactionMessage(ChatMessageKey key, String content, String groupingKey, long timestampSeconds) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.content = content;
        this.groupingKey = groupingKey;
        this.timestampSeconds = timestampSeconds;
    }

    public ChatMessageKey key() {
        return key;
    }

    public String content() {
        return content;
    }

    public Optional<String> groupingKey() {
        return Optional.ofNullable(groupingKey);
    }

    public long timestampSeconds() {
        return timestampSeconds;
    }

    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    @Override
    public Type type() {
        return Type.REACTION;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReactionMessage that
                && Objects.equals(key, that.key)
                && Objects.equals(content, that.content)
                && Objects.equals(groupingKey, that.groupingKey)
                && timestampSeconds == that.timestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, content, groupingKey, timestampSeconds);
    }

    @Override
    public String toString() {
        return "ReactionMessage[" +
                "key=" + key +
                ", content=" + content +
                ", groupingKey=" + groupingKey +
                ", timestampSeconds=" + timestampSeconds +
                ']';
    }
}