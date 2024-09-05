package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonCreator;
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
public final class QuotedMessageInfo implements MessageInfo<QuotedMessageInfo> {
    /**
     * The id of the message
     */
    private final String id;

    /**
     * The chat of the message
     */
    private final Chat chat;

    /**
     * The sender of the message
     */
    private final Contact sender;

    /**
     * The message
     */
    private MessageContainer message;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public QuotedMessageInfo(String id, Chat chat, Contact sender, MessageContainer message) {
        this.id = id;
        this.chat = chat;
        this.sender = sender;
        this.message = message;
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
        return Objects.requireNonNullElseGet(sender.jid(), this::parentJid);
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
    public QuotedMessageInfo setMessage(MessageContainer message) {
        this.message = message;
        return this;
    }

    @Override
    public OptionalLong timestampSeconds() {
        return OptionalLong.empty();
    }
}
