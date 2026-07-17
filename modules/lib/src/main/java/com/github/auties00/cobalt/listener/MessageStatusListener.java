package com.github.auties00.cobalt.listener;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;

/**
 * A functional interface for the {@code onMessageStatus} event.
 *
 * <p>The event is emitted when an outbound message transitions through its delivery lifecycle: the
 * Linked client raises it for each receipt covering the message, and the Cloud client raises it for
 * each {@code statuses[]} entry of a webhook delivery. The new status is read from
 * {@link LinkedMessageInfo#status()}. The flavour aggregators extend this interface and supply an empty
 * default implementation, so the event can also be observed in isolation as a lambda.
 *
 * @param <C> the client flavour this listener is registered on; the callback receives that exact
 *            flavour, or {@link WhatsAppClient}{@code <?>} for a listener registered on both
 * @see MessageStatus
 */
@FunctionalInterface
public non-sealed interface MessageStatusListener<C extends WhatsAppClient<?>> extends WhatsAppListener {
    /**
     * Notifies the listener that a message's status has changed (sent, delivered, read).
     *
     * @param whatsapp the client emitting the event
     * @param info     the message whose status changed
     */
    void onMessageStatus(C whatsapp, MessageInfo info);
}
