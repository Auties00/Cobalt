package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudBusinessCapabilityUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onBusinessCapabilityUpdate onBusinessCapabilityUpdate} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for each webhook delivery whose change field is {@code business_capability_update}.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudBusinessCapabilityListener extends CloudListener {
    /**
     * Notifies the listener that the business messaging capacity changed.
     *
     * @param whatsapp the client emitting the event
     * @param update   the capability update
     */
    void onBusinessCapabilityUpdate(CloudWhatsAppClient whatsapp, CloudBusinessCapabilityUpdate update);
}
