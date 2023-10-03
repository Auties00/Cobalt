package it.auties.whatsapp.model.jid;

import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility interface to make providing a jid easier
 */
public sealed interface JidProvider permits Chat, Contact, Jid {
    /**
     * Returns this object as a jid
     *
     * @return a non-null jid
     */
    @NonNull
    Jid toJid();
}
