package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudAccountUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onAccountUpdate onAccountUpdate} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for each webhook delivery whose change field is {@code account_update}, {@code account_alerts}, or {@code account_review_update}.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudAccountUpdateListener extends CloudListener {
    /**
     * Notifies the listener that the WhatsApp Business Account changed status.
     *
     * @param whatsapp the client emitting the event
     * @param update   the account update
     */
    void onAccountUpdate(CloudWhatsAppClient whatsapp, CloudAccountUpdate update);
}
