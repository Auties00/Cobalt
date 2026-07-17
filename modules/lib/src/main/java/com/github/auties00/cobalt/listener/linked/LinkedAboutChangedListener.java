package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onAboutChanged onAboutChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedAboutChangedListener extends LinkedListener {
    /**
     * Notifies the listener that the user's about/status text has
     * changed.
     *
     * @param whatsapp the client emitting the event
     * @param oldAbout the previous about text
     * @param newAbout the new about text
     */
    void onAboutChanged(LinkedWhatsAppClient whatsapp, String oldAbout, String newAbout);
}
