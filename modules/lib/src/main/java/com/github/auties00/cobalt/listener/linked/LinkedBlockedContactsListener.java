package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;

import java.util.Collection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onBlockedContacts onBlockedContacts} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedBlockedContactsListener extends LinkedListener {
    /**
     * Notifies the listener that the Blocked Contacts privacy list was
     * refreshed against the server.
     *
     * @apiNote
     * Fires once each time
     * {@link LinkedWhatsAppClient#refreshBlockList()} commits a fresh
     * server-authoritative copy of the list, regardless of whether
     * individual entries changed. Use to redraw the Blocked Contacts
     * settings surface against the new authoritative set. The
     * collection is the same view returned by
     * {@link LinkedWhatsAppContactStore#blockedContacts()} after the refresh.
     *
     * @param whatsapp        the client emitting the event
     * @param blockedContacts the new authoritative set of blocked
     *                        contacts; may be empty
     */
    void onBlockedContacts(LinkedWhatsAppClient whatsapp, Collection<Jid> blockedContacts);
}
