package com.github.auties00.cobalt.listener;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;

/**
 * A functional interface for the {@code onMessageDeleted} event.
 *
 * <p>The event is emitted when a message is deleted: the Linked client raises it when a revoke
 * protocol message lands, and the Cloud client raises it for each {@code statuses[]} entry whose
 * status is {@code deleted}. The flavour aggregators extend this interface and supply an empty
 * default implementation, so the event can also be observed in isolation as a lambda.
 *
 * @param <C> the client flavour this listener is registered on; the callback receives that exact
 *            flavour, or {@link WhatsAppClient}{@code <?>} for a listener registered on both
 */
@FunctionalInterface
public non-sealed interface MessageDeletedListener<C extends WhatsAppClient<?>> extends WhatsAppListener {
    /**
     * Notifies the listener that a message has been deleted.
     *
     * @param whatsapp the client emitting the event
     * @param info     the message that was deleted
     * @param everyone whether the message was deleted for everyone or only for the sender
     */
    void onMessageDeleted(C whatsapp, MessageInfo info, boolean everyone);
}
