package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onMessageEcho onMessageEcho}
 * event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each message the business sent from the WhatsApp app, delivered through the {@code smb_message_echoes}
 * webhook change field.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudMessageEchoListener extends CloudListener {
    /**
     * Notifies the listener that the business sent a message from the WhatsApp app.
     *
     * @param whatsapp the client emitting the event
     * @param info     the echoed message
     */
    void onMessageEcho(CloudWhatsAppClient whatsapp, MessageInfo info);
}
