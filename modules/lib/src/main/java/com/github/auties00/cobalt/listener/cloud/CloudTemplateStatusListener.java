package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateStatusUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onTemplateStatus onTemplateStatus} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for each webhook delivery whose change field is {@code message_template_status_update}.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudTemplateStatusListener extends CloudListener {
    /**
     * Notifies the listener that a message template changed review status.
     *
     * @param whatsapp the client emitting the event
     * @param update   the status transition
     */
    void onTemplateStatus(CloudWhatsAppClient whatsapp, CloudTemplateStatusUpdate update);
}
