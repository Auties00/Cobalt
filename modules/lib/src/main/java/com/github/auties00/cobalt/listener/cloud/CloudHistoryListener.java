package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudHistorySync;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onHistorySync onHistorySync} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised once per chunk of a webhook delivery whose change field is {@code history}, in stream order.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudHistoryListener extends CloudListener {
    /**
     * Notifies the listener that a history synchronization chunk arrived.
     *
     * @param whatsapp the client emitting the event
     * @param chunk    the history chunk
     */
    void onHistorySync(CloudWhatsAppClient whatsapp, CloudHistorySync chunk);
}
