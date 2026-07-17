package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onMessageQuarantined onMessageQuarantined} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedMessageQuarantinedListener extends LinkedListener {
    /**
     * Notifies the listener that an inbound message was withheld by Defense Mode.
     *
     * <p>A quarantined message is stored but kept out of the
     * {@link com.github.auties00.cobalt.listener.NewMessageListener#onNewMessage onNewMessage}
     * event; this event is the way to observe such messages while Defense Mode holds them back.
     * The message carries its withheld content through
     * {@link ChatMessageInfo#quarantinedMessage()} and can be released with
     * {@link LinkedWhatsAppClient#restoreQuarantinedMessage(com.github.auties00.cobalt.wire.core.message.MessageKey)}.
     *
     * @param whatsapp the client emitting the event
     * @param info     the quarantined message
     */
    void onMessageQuarantined(LinkedWhatsAppClient whatsapp, ChatMessageInfo info);
}
