package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacySetting;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onStatusPrivacyChanged onStatusPrivacyChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedStatusPrivacyChangedListener extends LinkedListener {
    /**
     * Notifies the listener that the Status story privacy setting
     * was refreshed against the server.
     *
     * @apiNote
     * Fires each time {@link LinkedWhatsAppClient#refreshStatusPrivacy()}
     * commits a fresh server-authoritative value, regardless of
     * whether the value changed.
     *
     * @param whatsapp      the client emitting the event
     * @param statusPrivacy the new authoritative status privacy setting
     */
    void onStatusPrivacyChanged(LinkedWhatsAppClient whatsapp, StatusPrivacySetting statusPrivacy);
}
