package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.business.webgraphql.WhatsAppWebGraphQlSession;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onGraphQlSessionChanged onWhatsAppWebGraphQlSessionChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedGraphQlSessionChangedListener extends LinkedListener {
    /**
     * Notifies the listener that the WhatsApp Web GraphQL session
     * credentials have changed.
     *
     * <p>Fires after the {@code http_relay} GraphQL transport
     * re-bootstraps its session cookie and {@code lsd} token, both when
     * the refresh runs automatically after a successful connection on a
     * WhatsApp Web client and when it is invoked directly.
     *
     * @param whatsapp the client emitting the event
     * @param session  the {@link WhatsAppWebGraphQlSession} now in effect
     */
    void onGraphQlSessionChanged(LinkedWhatsAppClient whatsapp, WhatsAppWebGraphQlSession session);
}
