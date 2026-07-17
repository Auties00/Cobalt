package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateQualityUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onTemplateQuality onTemplateQuality} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for each webhook delivery whose change field is {@code message_template_quality_update}.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudTemplateQualityListener extends CloudListener {
    /**
     * Notifies the listener that a message template changed quality score.
     *
     * @param whatsapp the client emitting the event
     * @param update   the quality transition
     */
    void onTemplateQuality(CloudWhatsAppClient whatsapp, CloudTemplateQualityUpdate update);
}
