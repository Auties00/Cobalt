package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudCallSettings;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onAccountSettings onAccountSettings}
 * event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each webhook delivery whose change field is {@code account_settings_update} and that carries a
 * {@code calling} settings object.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudAccountSettingsListener extends CloudListener {
    /**
     * Notifies the listener that the phone-number account settings changed.
     *
     * @param whatsapp the client emitting the event
     * @param settings the updated Calling configuration carried by the change
     */
    void onAccountSettings(CloudWhatsAppClient whatsapp, CloudCallSettings settings);
}
