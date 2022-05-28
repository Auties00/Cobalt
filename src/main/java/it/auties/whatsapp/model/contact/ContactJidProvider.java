package it.auties.whatsapp.model.contact;

import it.auties.whatsapp.model.chat.Chat;

/**
 * Utility interface to make providing a jid easier
 */
public sealed interface ContactJidProvider permits Chat, Contact, ContactJid {
    /**
     * Returns this object as a jid
     *
     * @return a non-null jid
     */
    ContactJid toJid();
}
