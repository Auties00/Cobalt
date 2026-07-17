package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallVideoStateChanged onCallVideoStateChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallVideoStateChangedListener extends LinkedListener {
    /**
     * Notifies the listener that a call participant has turned video on or
     * off.
     *
     * @param whatsapp the client emitting the event
     * @param callId   the identifier of the call
     * @param fromJid  the JID of the participant whose video state changed
     * @param enabled  {@code true} if announcing video-on, {@code false}
     *                 for video-off
     */
    void onCallVideoStateChanged(LinkedWhatsAppClient whatsapp, String callId, Jid fromJid, boolean enabled);
}
