package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudPaymentConfiguration;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onPaymentConfiguration onPaymentConfiguration}
 * event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each webhook delivery whose change field is {@code payment_configuration_update}.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudPaymentConfigurationListener extends CloudListener {
    /**
     * Notifies the listener that a payment gateway configuration changed.
     *
     * @param whatsapp the client emitting the event
     * @param update   the payment configuration change
     */
    void onPaymentConfiguration(CloudWhatsAppClient whatsapp, CloudPaymentConfiguration update);
}
