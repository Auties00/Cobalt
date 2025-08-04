package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * An immutable model class that represents a quoted message
 */
@ProtobufMessage
public final class QuotedMessageInfo implements MessageInfo {
    /**
     * The id of the message
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * The chat of the message
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final Chat chat;

    /**
     * The sender of the message
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final Contact sender;

    /**
     * The message
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    MessageContainer message;

    QuotedMessageInfo(String id, Chat chat, Contact sender, MessageContainer message) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.chat = Objects.requireNonNull(chat, "chat cannot be null");
        this.sender = sender;
        this.message = Objects.requireNonNull(message, "message cannot be null");
    }

    /**
     * Constructs a quoted message from a context info
     *
     * @param contextInfo the non-null context info
     * @return an optional quoted message
     */
    public static Optional<QuotedMessageInfo> of(ContextInfo contextInfo) {
        if (!contextInfo.hasQuotedMessage()) {
            return Optional.empty();
        }
        var id = contextInfo.quotedMessageId().orElseThrow();
        var chat = contextInfo.quotedMessageChat().orElseThrow();
        var sender = contextInfo.quotedMessageSender().orElse(null);
        var message = contextInfo.quotedMessage().orElseThrow();
        return Optional.of(new QuotedMessageInfo(id, chat, sender, message));
    }

    @Override
    public Jid parentJid() {
        return chat.jid();
    }

    /**
     * Returns the sender's jid
     *
     * @return a jid
     */
    @Override
    public Jid senderJid() {
        if(sender != null) {
            return sender.jid();
        }else {
            return chat.jid();
        }
    }

    /**
     * Returns the sender of this message
     *
     * @return an optional
     */
    public Optional<Contact> sender() {
        return Optional.ofNullable(sender);
    }

    @Override
    public String id() {
        return id;
    }

    public Optional<Chat> chat() {
        return Optional.of(chat);
    }

    @Override
    public MessageContainer message() {
        return message;
    }

    @Override
    public void setMessage(MessageContainer message) {
        this.message = message;
    }

    @Override
    public OptionalLong timestampSeconds() {
        return OptionalLong.empty();
    }
}
