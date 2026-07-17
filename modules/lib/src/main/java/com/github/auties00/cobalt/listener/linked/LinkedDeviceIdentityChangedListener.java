package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Set;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onDeviceIdentityChanged onDeviceIdentityChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedDeviceIdentityChangedListener extends LinkedListener {
    /**
     * Notifies the listener that a device's identity key has changed.
     *
     * <p>This indicates that the device was reset, reinstalled, or
     * potentially compromised. Applications should display a security
     * warning to users.
     *
     * @param whatsapp       the client emitting the event
     * @param userJid        the user whose device changed
     * @param changedDevices the devices with new identity keys
     */
    void onDeviceIdentityChanged(LinkedWhatsAppClient whatsapp, Jid userJid, Set<Jid> changedDevices);
}
