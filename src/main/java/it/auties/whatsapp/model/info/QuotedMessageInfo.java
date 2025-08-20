package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.newsletter.Newsletter;

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
     * The sender of the message
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    Contact sender;

    /**
     * The message
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    MessageContainer message;

    // FIXME: Add a feature in ModernProtobuf that evaluates sealed types
    //  and considers if all implementations match the expected type instead of simulating it like this

    /**
     * The chat of the message
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    Chat parentChat;

    /**
     * The newsletter of the message
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    Newsletter parentNewsletter;

    QuotedMessageInfo(String id, Contact sender, MessageContainer message, Chat chat, Newsletter newsletter) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.sender = sender;
        this.message = Objects.requireNonNull(message, "message cannot be null");
        if(chat == null && newsletter == null) {
            throw new NullPointerException("parent cannot be null");
        }
        this.parentChat = chat;
        this.parentNewsletter = newsletter;
    }

    QuotedMessageInfo(String id, Contact sender, MessageContainer message, MessageInfoParent parent) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.sender = sender;
        this.message = Objects.requireNonNull(message, "message cannot be null");
       setParent(parent);
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
        var parent = contextInfo.quotedMessageParent().orElseThrow();
        var sender = contextInfo.quotedMessageSender().orElse(null);
        var message = contextInfo.quotedMessage().orElseThrow();
        return Optional.of(new QuotedMessageInfo(id, sender, message, parent));
    }

    @Override
    public MessageStatus status() {
        return MessageStatus.UNKNOWN;
    }

    @Override
    public void setStatus(MessageStatus status) {

    }

    @Override
    public Jid parentJid() {
        if(parentChat != null) {
            return parentChat.jid();
        }else if(parentNewsletter != null) {
            return parentNewsletter.jid();
        }else {
            throw new InternalError();
        }
    }

    @Override
    public Optional<MessageInfoParent> parent() {
        if(parentChat != null) {
            return Optional.of(parentChat);
        } else if(parentNewsletter != null) {
            return Optional.of(parentNewsletter);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void setParent(MessageInfoParent parent) {
        switch (Objects.requireNonNull(parent, "parent cannot be null")) {
            case Chat chat -> this.parentChat = chat;
            case Newsletter newsletter -> this.parentNewsletter = newsletter;
        }
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
        } else if(parentChat != null) {
            return parentChat.jid();
        }else if(parentNewsletter != null) {
            return parentNewsletter.jid();
        }else {
            throw new InternalError();
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
    public void setSender(Contact sender) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        this.sender = sender;
    }

    @Override
    public String id() {
        return id;
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
