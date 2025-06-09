package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

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
    final Jid chatJid;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String messageId;

    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean fromMe;

    MessageIndexInfo(String type, Jid chatJid, String messageId, boolean fromMe) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.chatJid = chatJid;
        this.messageId = messageId;
        this.fromMe = fromMe;
    }

    public String type() {
        return type;
    }

    public Optional<Jid> chatJid() {
        return Optional.ofNullable(chatJid);
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
                && Objects.equals(chatJid, that.chatJid)
                && Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, chatJid, messageId, fromMe);
    }

    @Override
    public String toString() {
        return "MessageIndexInfo[" +
                "type=" + type + ", " +
                "chatJid=" + chatJid + ", " +
                "messageId=" + messageId + ", " +
                "fromMe=" + fromMe + ']';
    }
}
