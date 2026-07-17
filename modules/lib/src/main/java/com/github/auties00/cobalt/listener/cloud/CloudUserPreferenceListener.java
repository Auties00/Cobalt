package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudUserPreferenceUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onUserPreference onUserPreference} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised once per entry of a webhook delivery whose change field is {@code user_preferences}.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudUserPreferenceListener extends CloudListener {
    /**
     * Notifies the listener that a user changed a marketing preference.
     *
     * @param whatsapp the client emitting the event
     * @param update   the preference change
     */
    void onUserPreference(CloudWhatsAppClient whatsapp, CloudUserPreferenceUpdate update);
}
