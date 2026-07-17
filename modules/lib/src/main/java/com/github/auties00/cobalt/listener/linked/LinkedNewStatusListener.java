package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onNewStatus onNewStatus} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedNewStatusListener extends LinkedListener {
    /**
     * Notifies the listener that a new status update has been received.
     *
     * @param whatsapp the client emitting the event
     * @param status   the new status message
     */
    void onNewStatus(LinkedWhatsAppClient whatsapp, ChatMessageInfo status);
}
