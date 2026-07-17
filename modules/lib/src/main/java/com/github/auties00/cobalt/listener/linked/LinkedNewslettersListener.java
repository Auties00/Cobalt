package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;

import java.util.Collection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onNewsletters onNewsletters} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedNewslettersListener extends LinkedListener {
    /**
     * Notifies the listener that the full newsletter list has been
     * received from WhatsApp.
     *
     * <p>Fires exactly once per login. On a fresh session it fires
     * after {@link LinkedWhatsAppClient#refreshNewsletters()} completes
     * during the post-success bootstrap; on a reconnect it fires from
     * the cached store immediately after the {@code <success>} stanza.
     * The callback fires even when the newsletter list is empty.
     *
     * @param whatsapp    the client emitting the event
     * @param newsletters the collection of newsletters
     */
    void onNewsletters(LinkedWhatsAppClient whatsapp, Collection<Newsletter> newsletters);
}
