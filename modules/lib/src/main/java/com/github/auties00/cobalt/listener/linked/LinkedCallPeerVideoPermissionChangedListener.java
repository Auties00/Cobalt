package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallPeerVideoPermissionChanged onCallPeerVideoPermissionChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallPeerVideoPermissionChangedListener extends LinkedListener {
    /**
     * Notifies the listener that a peer's permission to send video was granted or
     * revoked during a group call.
     *
     * @param whatsapp  the client emitting the event
     * @param callId    the identifier of the call
     * @param peerJid   the JID of the peer whose video permission changed
     * @param permitted {@code true} when the peer may now send video, {@code false} when revoked
     */
    void onCallPeerVideoPermissionChanged(LinkedWhatsAppClient whatsapp, String callId, Jid peerJid, boolean permitted);
}
