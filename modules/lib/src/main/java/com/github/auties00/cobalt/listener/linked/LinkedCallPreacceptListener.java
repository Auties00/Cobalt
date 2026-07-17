package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallPreaccept onCallPreaccept} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallPreacceptListener extends LinkedListener {
    /**
     * Notifies the listener that the local pre-acceptance phase of an
     * incoming call has begun.
     *
     * <p>Fired when the peer confirms this device is alerting the user.
     *
     * @param whatsapp the client emitting the event
     * @param callId   the identifier of the call
     * @param fromJid  the JID of the peer that sent the preaccept
     */
    void onCallPreaccept(LinkedWhatsAppClient whatsapp, String callId, Jid fromJid);
}
