package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

/**
 * Model interface to mark classes that can provide info about a message
 */
public sealed interface MessageMetadataProvider permits MessageInfo, QuotedMessage {
    /**
     * Returns the id of the message
     *
     * @return a string
     */
    String id();


    /**
     * Returns the jid of the chat where the message was sent
     *
     * @return a jid
     */
    Jid chatJid();

    /**
     * Returns the chat of the message
     *
     * @return a chat
     */
    Optional<Chat> chat();

    /**
     * Returns the sender's jid
     *
     * @return a jid
     */
    Jid senderJid();

    /**
     * Returns the sender of the message
     *
     * @return an optional
     */
    Optional<Contact> sender();

    /**
     * Returns the message
     *
     * @return a message container
     */
    MessageContainer message();
}
