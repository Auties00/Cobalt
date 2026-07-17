package com.github.auties00.cobalt.listener;

import com.github.auties00.cobalt.client.WhatsAppClient;

/**
 * A functional interface for the {@code onLoggedIn} event.
 *
 * <p>The event is emitted once the client is live: the Linked client raises it when the connection
 * and login handshake to a WhatsApp account completes, and the Cloud client raises it once the
 * access token has been validated and the webhook receiver has started. The flavour aggregators
 * extend this interface and supply an empty default implementation, so the event can also be
 * observed in isolation as a lambda.
 *
 * @param <C> the client flavour this listener is registered on; the callback receives that exact
 *            flavour, or {@link WhatsAppClient}{@code <?>} for a listener registered on both
 */
@FunctionalInterface
public non-sealed interface LoggedInListener<C extends WhatsAppClient<?>> extends WhatsAppListener {
    /**
     * Notifies the listener that the client has connected and authenticated.
     *
     * <p>On the Linked transport, data such as chats and contacts may not yet be loaded into memory
     * when this event fires; use the corresponding data-specific events to observe them.
     *
     * @param whatsapp the client emitting the event
     */
    void onLoggedIn(C whatsapp);
}
