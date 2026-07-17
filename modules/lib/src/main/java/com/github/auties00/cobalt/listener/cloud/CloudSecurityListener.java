package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudSecurityUpdate;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onSecurity onSecurity} event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each webhook delivery whose change field is {@code security}.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudSecurityListener extends CloudListener {
    /**
     * Notifies the listener of a phone-number security event.
     *
     * @param whatsapp the client emitting the event
     * @param update   the security event
     */
    void onSecurity(CloudWhatsAppClient whatsapp, CloudSecurityUpdate update);
}
