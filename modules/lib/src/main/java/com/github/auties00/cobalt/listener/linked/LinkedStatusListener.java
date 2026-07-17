package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;

import java.util.Collection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onStatus onStatus} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedStatusListener extends LinkedListener {
    /**
     * Notifies the listener that the full status feed has been received
     * from WhatsApp.
     *
     * <p>Fires exactly once per login. On a fresh session it fires
     * after the initial-status-v3 history-sync chunk has been
     * processed; on a reconnect it fires from the cached store
     * immediately after the {@code <success>} stanza. The callback
     * fires even when the status feed is empty.
     *
     * @param whatsapp the client emitting the event
     * @param status   the collection of status updates
     */
    void onStatus(LinkedWhatsAppClient whatsapp, Collection<ChatMessageInfo> status);
}
