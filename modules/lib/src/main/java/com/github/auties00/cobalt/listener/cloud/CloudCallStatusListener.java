package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudCallEvent;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onCallStatus onCallStatus} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each business-initiated call status transition ({@code RINGING}, {@code ACCEPTED}, {@code REJECTED})
 * delivered through a {@code statuses[]} entry typed {@code call} on the {@code calls} webhook change.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudCallStatusListener extends CloudListener {
    /**
     * Notifies the listener of a business-initiated call status transition.
     *
     * @param whatsapp the client emitting the event
     * @param event    the call status event
     */
    void onCallStatus(CloudWhatsAppClient whatsapp, CloudCallEvent.Status event);
}
