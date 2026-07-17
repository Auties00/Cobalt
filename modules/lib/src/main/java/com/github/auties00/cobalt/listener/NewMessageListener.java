package com.github.auties00.cobalt.listener;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;

/**
 * A functional interface for the {@code onNewMessage} event.
 *
 * <p>The event is emitted by every {@link WhatsAppClient} flavour: the Linked client raises it for
 * each message decrypted from the socket, and the Cloud client raises it for each inbound message
 * decoded from a webhook delivery whose change field is {@code messages}. The flavour aggregators
 * extend this interface and supply an empty default implementation, so the event can also be
 * observed in isolation as a lambda.
 *
 * @param <C> the client flavour this listener is registered on; the callback receives that exact
 *            flavour, or {@link WhatsAppClient}{@code <?>} for a listener registered on both
 */
@FunctionalInterface
public non-sealed interface NewMessageListener<C extends WhatsAppClient<?>> extends WhatsAppListener {
    /**
     * Notifies the listener that a new message has been received.
     *
     * @param whatsapp the client emitting the event
     * @param info     the message that was received
     */
    void onNewMessage(C whatsapp, MessageInfo info);
}
