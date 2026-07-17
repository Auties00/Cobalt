package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

import java.util.Set;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onTosNoticesChanged onTosNoticesChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedTosNoticesChangedListener extends LinkedListener {
    /**
     * Notifies the listener that the set of Terms-of-Service notice
     * identifiers recorded for the account has changed.
     *
     * <p>Fires both after the notices are fetched from the server and
     * after a server push records new notices, so a listener observing
     * this event sees the same identifier set regardless of which path
     * updated it.
     *
     * @param whatsapp  the client emitting the event
     * @param noticeIds the identifiers of the notices now recorded for the account
     */
    void onTosNoticesChanged(LinkedWhatsAppClient whatsapp, Set<String> noticeIds);
}
