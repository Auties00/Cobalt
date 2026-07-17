package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onError onError} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised when
 * a webhook delivery fails its signature check, cannot be decoded, or a listener dispatch throws.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudErrorListener extends CloudListener {
    /**
     * Notifies the listener of a processing failure.
     *
     * @param whatsapp the client emitting the event
     * @param error    the failure
     */
    void onError(CloudWhatsAppClient whatsapp, Throwable error);
}
