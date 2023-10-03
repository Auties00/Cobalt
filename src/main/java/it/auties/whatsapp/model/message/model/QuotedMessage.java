package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.info.ContextInfo;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;

/**
 * An immutable model class that represents a quoted message
 */
public final class QuotedMessage implements MessageMetadataProvider {
    /**
     * The id of the message
     */
    @NonNull
    private final String id;

    /**
     * The chat of the message
     */
    @NonNull
    private final Chat chat;

    /**
     * The sender of the message
     */
    private final Contact sender;

    /**
     * The message
     */
    @NonNull
    private final MessageContainer message;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public QuotedMessage(@NonNull String id, @NonNull Chat chat, Contact sender, @NonNull MessageContainer message) {
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
    public static Optional<QuotedMessage> of(@NonNull ContextInfo contextInfo) {
        if (!contextInfo.hasQuotedMessage()) {
            return Optional.empty();
        }
        var id = contextInfo.quotedMessageId().orElseThrow();
        var chat = contextInfo.quotedMessageChat().orElseThrow();
        var sender = contextInfo.quotedMessageSender().orElse(null);
        var message = contextInfo.quotedMessage().orElseThrow();
        return Optional.of(new QuotedMessage(id, chat, sender, message));
    }

    @Override
    public Jid chatJid() {
        return chat.jid();
    }

    /**
     * Returns the sender's jid
     *
     * @return a jid
     */
    @Override
    public Jid senderJid() {
        return Objects.requireNonNullElseGet(sender.jid(), this::chatJid);
    }

    /**
     * Returns the sender of this message
     *
     * @return an optional
     */
    public Optional<Contact> sender() {
        return Optional.ofNullable(sender);
    }

    public String id() {
        return id;
    }

    @Override
    public Optional<Chat> chat() {
        return Optional.of(chat);
    }

    public MessageContainer message() {
        return message;
    }
}
