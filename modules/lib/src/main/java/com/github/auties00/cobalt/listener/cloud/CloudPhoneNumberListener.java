package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.phone.CloudPhoneNumberUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onPhoneNumberUpdate onPhoneNumberUpdate} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for each webhook delivery whose change field is {@code phone_number_name_update} or {@code phone_number_quality_update}; the concrete update kind is recovered with a pattern-match over the sealed {@link CloudPhoneNumberUpdate} hierarchy.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudPhoneNumberListener extends CloudListener {
    /**
     * Notifies the listener that the phone number changed display name or quality.
     *
     * @param whatsapp the client emitting the event
     * @param update   the phone-number update
     */
    void onPhoneNumberUpdate(CloudWhatsAppClient whatsapp, CloudPhoneNumberUpdate update);
}
