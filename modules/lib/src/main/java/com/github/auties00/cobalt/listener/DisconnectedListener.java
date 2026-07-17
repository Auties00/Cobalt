package com.github.auties00.cobalt.listener;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;

/**
 * A functional interface for the {@code onDisconnected} event.
 *
 * <p>The event is emitted when the client goes down: the Linked client raises it when the socket
 * closes, and the Cloud client raises it when the webhook receiver stops. The flavour aggregators
 * extend this interface and supply an empty default implementation, so the event can also be
 * observed in isolation as a lambda.
 *
 * @param <C> the client flavour this listener is registered on; the callback receives that exact
 *            flavour, or {@link WhatsAppClient}{@code <?>} for a listener registered on both
 */
@FunctionalInterface
public non-sealed interface DisconnectedListener<C extends WhatsAppClient<?>> extends WhatsAppListener {
    /**
     * Notifies the listener that the client has disconnected.
     *
     * @param whatsapp the client emitting the event
     * @param reason   the reason for the disconnection
     * @see WhatsAppClientDisconnectReason
     */
    void onDisconnected(C whatsapp, WhatsAppClientDisconnectReason reason);
}
