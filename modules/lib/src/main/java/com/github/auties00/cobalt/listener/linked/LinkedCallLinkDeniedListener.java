package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.wire.linked.call.JoinableCallLink;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallLinkDenied onCallLinkDenied} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallLinkDeniedListener extends LinkedListener {
    /**
     * Notifies the listener that the host of a call-link declined
     * the local user's join request; terminal for that link
     * attempt.
     *
     * @param whatsapp the client emitting the event
     * @param link     the link that was denied
     */
    void onCallLinkDenied(LinkedWhatsAppClient whatsapp, JoinableCallLink link);
}
