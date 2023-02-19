package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.ContextInfo;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;
import java.util.Optional;

/**
 * An immutable model class that represents a quoted message
 */
@AllArgsConstructor
@Value
@Accessors(fluent = true)
public class QuotedMessage implements MessageMetadataProvider {
    /**
     * The id of the message
     */
    @NonNull String id;

    /**
     * The chat of the message
     */
    @NonNull Chat chat;

    /**
     * The sender of the message
     */
    Contact sender;

    /**
     * The message
     */
    @NonNull MessageContainer message;

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
        var id = contextInfo.quotedMessageId().get();
        var chat = contextInfo.quotedMessageChat().get();
        var sender = contextInfo.quotedMessageSender().orElse(null);
        var message = contextInfo.quotedMessage().get();
        return Optional.of(new QuotedMessage(id, chat, sender, message));
    }

    /**
     * Returns the sender's jid
     *
     * @return a jid
     */
    @Override
    public ContactJid senderJid() {
        return Objects.requireNonNullElseGet(sender.jid(), chat::jid);
    }

    /**
     * Returns the sender of this message
     *
     * @return an optional
     */
    public Optional<Contact> sender() {
        return Optional.ofNullable(sender);
    }
}
