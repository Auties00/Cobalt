package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * An index that contains data about a setting change or an action
 */
@ProtobufMessage
public final class MessageIndexInfo implements Info {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String type;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String targetId;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String messageId;

    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean fromMe;

    MessageIndexInfo(String type, String targetId, String messageId, boolean fromMe) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.targetId = targetId;
        this.messageId = messageId;
        this.fromMe = fromMe;
    }

    public String type() {
        return type;
    }

    public Optional<String> targetId() {
        return Optional.ofNullable(targetId);
    }

    public Optional<String> messageId() {
        return Optional.ofNullable(messageId);
    }

    public boolean fromMe() {
        return fromMe;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MessageIndexInfo that
                && fromMe == that.fromMe
                && Objects.equals(type, that.type)
                && Objects.equals(targetId, that.targetId)
                && Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, targetId, messageId, fromMe);
    }

    @Override
    public String toString() {
        return "MessageIndexInfo[" +
                "type=" + type + ", " +
                "targetId=" + targetId + ", " +
                "messageId=" + messageId + ", " +
                "fromMe=" + fromMe + ']';
    }
}
