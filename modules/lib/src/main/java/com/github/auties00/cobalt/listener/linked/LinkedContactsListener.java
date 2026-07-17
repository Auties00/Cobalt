package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.contact.Contact;

import java.util.Collection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onContacts onContacts} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedContactsListener extends LinkedListener {
    /**
     * Notifies the listener that the full contact list has been received
     * from WhatsApp.
     *
     * <p>Fires exactly once per login. On a fresh session it fires after
     * the initial-bootstrap history-sync chunk has been processed; on a
     * reconnect it fires from the cached store immediately after the
     * {@code <success>} stanza. The callback fires even when the
     * contact list is empty.
     *
     * @param whatsapp the client emitting the event
     * @param contacts the collection of contacts
     */
    void onContacts(LinkedWhatsAppClient whatsapp, Collection<Contact> contacts);
}
