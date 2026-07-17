package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.alibaba.fastjson2.JSONObject;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onWebhookReceived onWebhookReceived} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for every verified webhook envelope before any decoding, so it observes the raw JSON of fields the typed listeners do not model.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudWebhookReceivedListener extends CloudListener {
    /**
     * Notifies the listener that a raw webhook envelope has been received.
     *
     * @param whatsapp the client emitting the event
     * @param envelope the raw webhook envelope
     */
    void onWebhookReceived(CloudWhatsAppClient whatsapp, JSONObject envelope);
}
