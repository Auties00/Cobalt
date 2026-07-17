package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.privacy.AccountDisappearingMode;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onDisappearingModeChanged onDisappearingModeChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedDisappearingModeChangedListener extends LinkedListener {
    /**
     * Notifies the listener that the account-level Disappearing
     * Messages setting was refreshed against the server.
     *
     * @apiNote
     * Fires each time
     * {@link LinkedWhatsAppClient#refreshDisappearingMode()} commits a
     * fresh server-authoritative value, regardless of whether the
     * value changed.
     *
     * @param whatsapp         the client emitting the event
     * @param disappearingMode the new authoritative disappearing mode
     */
    void onDisappearingModeChanged(LinkedWhatsAppClient whatsapp, AccountDisappearingMode disappearingMode);
}
