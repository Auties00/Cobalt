package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onNameChanged onNameChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedNameChangedListener extends LinkedListener {
    /**
     * Notifies the listener that the user's display name has changed.
     *
     * @param whatsapp the client emitting the event
     * @param oldName  the previous name
     * @param newName  the new name
     */
    void onNameChanged(LinkedWhatsAppClient whatsapp, String oldName, String newName);
}
