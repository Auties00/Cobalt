package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudMessagePricing;
import com.github.auties00.cobalt.wire.core.message.MessageKey;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onMessagePricing onMessagePricing}
 * event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each {@code statuses[]} entry of a {@code messages} webhook change that carries a {@code pricing}
 * object, alongside the shared message-status or message-deleted event the same entry produces.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudMessagePricingListener extends CloudListener {
    /**
     * Notifies the listener that an outbound message reported its billing information.
     *
     * @param whatsapp   the client emitting the event
     * @param messageKey the key of the outbound message the pricing applies to
     * @param pricing    the billing information carried by the status transition
     */
    void onMessagePricing(CloudWhatsAppClient whatsapp, MessageKey messageKey, CloudMessagePricing pricing);
}
