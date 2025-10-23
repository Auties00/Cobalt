package com.github.auties00.cobalt.model.proto.jid;

import com.github.auties00.cobalt.model.proto.contact.Contact;
import com.github.auties00.cobalt.model.proto.info.MessageInfoParent;

/**
 * Utility interface to make providing a value easier
 */
public sealed interface JidProvider permits Contact, MessageInfoParent, Jid, JidServer {
    /**
     * Returns this object as a value
     *
     * @return a non-null value
     */
    Jid toJid();
}
