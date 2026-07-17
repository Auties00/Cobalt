package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onRegistrationCode onRegistrationCode} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedRegistrationCodeListener extends LinkedListener {
    /**
     * Notifies the listener that a registration code (OTP) has been
     * requested from a new device.
     *
     * <p>This event is only triggered for the mobile API.
     *
     * @param whatsapp the client emitting the event
     * @param code     the registration code
     */
    void onRegistrationCode(LinkedWhatsAppClient whatsapp, long code);
}
