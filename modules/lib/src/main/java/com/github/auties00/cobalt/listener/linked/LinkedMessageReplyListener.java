package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onMessageReply onMessageReply} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedMessageReplyListener extends LinkedListener {
    /**
     * Notifies the listener that a message has been sent in reply to a
     * previous message.
     *
     * @param whatsapp the client emitting the event
     * @param response the reply message
     * @param quoted   the message being replied to
     */
    void onMessageReply(LinkedWhatsAppClient whatsapp, LinkedMessageInfo response, LinkedMessageInfo quoted);
}
