package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.wire.linked.call.CallPeerState;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallPeerStateChanged onCallPeerStateChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallPeerStateChangedListener extends LinkedListener {
    /**
     * Notifies the listener that a peer-state update was received during a
     * call.
     *
     * <p>The peer state is parsed into a typed {@link CallPeerState};
     * unrecognised values surface as {@link CallPeerState#UNKNOWN}.
     *
     * @param whatsapp the client emitting the event
     * @param callId   the identifier of the call
     * @param fromJid  the JID of the peer whose state changed
     * @param state    the parsed peer state
     */
    void onCallPeerStateChanged(LinkedWhatsAppClient whatsapp, String callId, Jid fromJid, CallPeerState state);
}
