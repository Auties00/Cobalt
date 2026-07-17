package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.wire.cloud.CloudCallEvent;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onCall onCall} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each inbound signaling event delivered through the {@code calls} webhook change field, either a
 * {@link CloudCallEvent.Connect} carrying an SDP offer or a {@link CloudCallEvent.Terminate} carrying
 * the final disposition. Business-initiated call status transitions reach
 * {@link CloudWhatsAppClient#addCallStatusListener
 * CloudCallStatusListener} and consumer permission replies reach
 * {@link CloudWhatsAppClient#addCallPermissionListener
 * CloudCallPermissionListener} instead.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudCallListener extends CloudListener {
    /**
     * Notifies the listener of an inbound call signaling event.
     *
     * @param whatsapp the client emitting the event
     * @param event    the signaling event, a {@link CloudCallEvent.Connect} or a
     *                 {@link CloudCallEvent.Terminate}
     */
    void onCall(CloudWhatsAppClient whatsapp, CloudCallEvent.Signaling event);
}
