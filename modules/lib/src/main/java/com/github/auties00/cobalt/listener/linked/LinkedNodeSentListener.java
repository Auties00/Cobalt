package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onNodeSent onNodeSent} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedNodeSentListener extends LinkedListener {
    /**
     * Notifies the listener that a stanza has been sent to the WhatsApp
     * server.
     *
     * @param whatsapp the client emitting the event
     * @param outgoing the stanza that was sent
     */
    void onNodeSent(LinkedWhatsAppClient whatsapp, Stanza outgoing);
}
