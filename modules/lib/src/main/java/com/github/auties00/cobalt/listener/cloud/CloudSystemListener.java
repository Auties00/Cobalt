package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudSystemUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onSystemUpdate onSystemUpdate}
 * event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each {@code system}-typed inbound message in a {@code messages} change, reporting a consumer phone
 * number change or account identity change.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudSystemListener extends CloudListener {
    /**
     * Notifies the listener that a consumer changed their phone number or account identity.
     *
     * @param whatsapp the client emitting the event
     * @param update   the system update
     */
    void onSystemUpdate(CloudWhatsAppClient whatsapp, CloudSystemUpdate update);
}
