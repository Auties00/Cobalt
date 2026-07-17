package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatus;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onContactTextStatus onContactTextStatus} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedContactTextStatusListener extends LinkedListener {
    /**
     * Notifies the listener that a contact's text status metadata has
     * changed.
     *
     * @param whatsapp the client emitting the event
     * @param contact  the contact whose text status changed
     * @param status   the new text status
     */
    void onContactTextStatus(LinkedWhatsAppClient whatsapp, Jid contact, ContactTextStatus status);
}
