package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudAppStateSyncContact;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onAppStateSync onAppStateSync}
 * event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised once
 * per contact entry carried by a webhook delivery whose change field is {@code smb_app_state_sync},
 * delivered shortly after a WhatsApp Coexistence onboarding succeeds.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudAppStateSyncListener extends CloudListener {
    /**
     * Notifies the listener that a Coexistence contact was synced.
     *
     * @param whatsapp the client emitting the event
     * @param contact  the synced contact entry
     */
    void onAppStateSync(CloudWhatsAppClient whatsapp, CloudAppStateSyncContact contact);
}
