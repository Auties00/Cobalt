package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplatePauseUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onTemplatePause onTemplatePause}
 * event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each {@code message_template_pause} and {@code message_template_unpause} webhook change, reporting that
 * the server halted a template whose recent sends triggered too many negative signals or that sending
 * may resume.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudTemplatePauseListener extends CloudListener {
    /**
     * Notifies the listener that a message template was paused or unpaused.
     *
     * @param whatsapp the client emitting the event
     * @param update   the template pause or unpause update
     */
    void onTemplatePause(CloudWhatsAppClient whatsapp, CloudTemplatePauseUpdate update);
}
