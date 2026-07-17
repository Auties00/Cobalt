package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onNodeReceived onNodeReceived} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedNodeReceivedListener extends LinkedListener {
    /**
     * Notifies the listener that a stanza has been received from the
     * WhatsApp server.
     *
     * @param whatsapp the client emitting the event
     * @param incoming the stanza that was received
     */
    void onNodeReceived(LinkedWhatsAppClient whatsapp, Stanza incoming);
}
