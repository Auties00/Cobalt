package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.flow.CloudFlowStatusUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onFlowStatus onFlowStatus} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for each webhook delivery whose change field is {@code flows}.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudFlowListener extends CloudListener {
    /**
     * Notifies the listener that a Flow changed status or reported an endpoint health alert.
     *
     * @param whatsapp the client emitting the event
     * @param update   the flow event
     */
    void onFlowStatus(CloudWhatsAppClient whatsapp, CloudFlowStatusUpdate update);
}
