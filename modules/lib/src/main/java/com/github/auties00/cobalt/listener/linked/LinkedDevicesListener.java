package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Collection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onLinkedDevices onLinkedDevices} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedDevicesListener extends LinkedListener {
    /**
     * Notifies the listener that the list of devices linked to this
     * account was refreshed against the server.
     *
     * @apiNote
     * Fires each time {@link LinkedWhatsAppClient#refreshLinkedDevices()}
     * commits a fresh server-authoritative copy of the paired-device
     * list. Use to redraw the Linked Devices settings surface.
     *
     * @param whatsapp       the client emitting the event
     * @param linkedDevices  the new authoritative set of device JIDs;
     *                       may be empty
     */
    void onLinkedDevices(LinkedWhatsAppClient whatsapp, Collection<Jid> linkedDevices);
}
