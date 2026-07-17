package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaAccessTokenSession;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onFacebookGraphQlSessionChanged onFacebookGraphQlSessionChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedFacebookGraphQlSessionChangedListener extends LinkedListener {
    /**
     * Notifies the listener that the WhatsApp Business Facebook GraphQL session
     * credentials have changed.
     *
     * <p>Fires after the {@code http_comet} GraphQL transport mints a
     * fresh Facebook access token through a silent-nonce exchange, both
     * when the refresh runs automatically after a successful connection
     * on a WhatsApp Web client (when no Facebook GraphQL session was previously
     * established) and when it is invoked directly.
     *
     * @param whatsapp the client emitting the event
     * @param session  the {@link CtwaAccessTokenSession} now in effect
     */
    void onFacebookGraphQlSessionChanged(LinkedWhatsAppClient whatsapp, CtwaAccessTokenSession session);
}
