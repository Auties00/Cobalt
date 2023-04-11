package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Locale;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A container for unique identifiers and metadata linked to a {@link Message} and contained in
 * {@link MessageInfo}.
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ToString(exclude = {"chat", "sender"})
public class MessageKey implements ProtobufMessage {
    /**
     * The jid of the chat where the message was sent
     */
    @ProtobufProperty(index = 1, type = STRING)
    @NonNull
    private ContactJid chatJid;

    /**
     * The chat where the message was sent
     */
    @JsonBackReference
    private Chat chat;

    /**
     * Determines whether the message was sent by you or by someone else
     */
    @ProtobufProperty(index = 2, type = BOOL)
    private boolean fromMe;

    /**
     * The id of the message
     */
    @ProtobufProperty(index = 3, type = STRING)
    @NonNull
    @Default
    private String id = randomId();

    /**
     * The jid of the sender
     */
    @ProtobufProperty(index = 4, type = STRING)
    private ContactJid senderJid;

    /**
     * The sender of the message
     */
    private Contact sender;

    /**
     * Generates a random message id
     *
     * @return a non-null String
     */
    public static String randomId() {
        return Bytes.ofRandom(8).toHex().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the contact that sent the message
     *
     * @return an optional
     */
    public Optional<Contact> sender() {
        return Optional.ofNullable(sender);
    }

    /**
     * Returns the jid of the contact that sent the message
     *
     * @return an optional
     */
    public Optional<ContactJid> senderJid() {
        return Optional.ofNullable(senderJid);
    }

    /**
     * Copies this key
     *
     * @return a non-null message key
     */
    public MessageKey copy() {
        return new MessageKey(chatJid, chat, fromMe, id, senderJid, sender);
    }
}
