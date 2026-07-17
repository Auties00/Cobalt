package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.business.BusinessDataSharingConsent;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onBusinessPrivacySettingChanged onBusinessPrivacySettingChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedBusinessPrivacySettingChangedListener extends LinkedListener {
    /**
     * Notifies the listener that the account's WhatsApp Business
     * data-sharing-with-Meta consent has changed.
     *
     * <p>Fires both after the consent value is fetched from the server
     * and after a server push reports a new value, so a listener
     * observing this event sees the same consent regardless of which
     * path updated it.
     *
     * @param whatsapp the client emitting the event
     * @param consent  the {@link BusinessDataSharingConsent} now in effect
     */
    void onBusinessPrivacySettingChanged(LinkedWhatsAppClient whatsapp, BusinessDataSharingConsent consent);
}
