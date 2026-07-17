package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.wire.linked.call.JoinableCallLink;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallLinkAdmitted onCallLinkAdmitted} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallLinkAdmittedListener extends LinkedListener {
    /**
     * Notifies the listener that the host of a call-link they
     * clicked has admitted them out of the lobby; the call is now
     * starting. Followed by a regular {@code onCall} once the
     * underlying call session is created.
     *
     * @param whatsapp the client emitting the event
     * @param link     the link that was admitted
     */
    void onCallLinkAdmitted(LinkedWhatsAppClient whatsapp, JoinableCallLink link);
}
