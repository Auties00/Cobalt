package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

import java.util.List;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onWebAppPrimaryFeatures onWebAppPrimaryFeatures} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedWebAppPrimaryFeaturesListener extends LinkedListener {
    /**
     * Notifies the listener that the primary feature flags have been
     * received from WhatsApp Web.
     *
     * <p>This event is only triggered for web client connections.
     *
     * @param whatsapp the client emitting the event
     * @param features the collection of feature flags that were sent
     */
    void onWebAppPrimaryFeatures(LinkedWhatsAppClient whatsapp, List<String> features);
}
