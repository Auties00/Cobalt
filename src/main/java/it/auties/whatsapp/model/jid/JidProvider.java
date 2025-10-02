package it.auties.whatsapp.model.jid;

import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.newsletter.Newsletter;

/**
 * Utility interface to make providing a value easier
 */
public sealed interface JidProvider permits Chat, Contact, Jid, JidServer, Newsletter {
    /**
     * Returns this object as a value
     *
     * @return a non-null value
     */
    Jid toJid();
}
