package com.github.auties00.cobalt.model.jid;

import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.info.MessageInfoParent;
import com.github.auties00.cobalt.model.newsletter.Newsletter;

/**
 * Utility interface to make providing a value easier
 */
public sealed interface JidProvider permits Chat, Contact, MessageInfoParent, Jid, JidServer, Newsletter {
    /**
     * Returns this object as a value
     *
     * @return a non-null value
     */
    Jid toJid();
}
