package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onContactPresence onContactPresence} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedContactPresenceListener extends LinkedListener {
    /**
     * Notifies the listener that a contact's presence status has been
     * updated.
     *
     * @param whatsapp     the client emitting the event
     * @param conversation the chat related to this presence update
     * @param participant  the contact whose presence status changed
     */
    void onContactPresence(LinkedWhatsAppClient whatsapp, Jid conversation, Jid participant);
}
