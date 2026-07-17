package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallVideoUpgradeRequest onCallVideoUpgradeRequest} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallVideoUpgradeRequestListener extends LinkedListener {
    /**
     * Notifies the listener that the peer is asking to upgrade an
     * audio-only call to audio plus video.
     *
     * <p>The application can call
     * {@link LinkedWhatsAppClient#acceptCallVideoUpgrade}
     * or
     * {@link LinkedWhatsAppClient#rejectCallVideoUpgrade}
     * in response. Acceptance triggers the local side to start its
     * own video track; rejection sends a denial signal back to the
     * peer.
     *
     * @param whatsapp the client emitting the event
     * @param callId   the identifier of the call
     * @param fromJid  the JID of the peer requesting the upgrade
     */
    void onCallVideoUpgradeRequest(LinkedWhatsAppClient whatsapp, String callId, Jid fromJid);
}
