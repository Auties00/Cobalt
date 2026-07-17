package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.wire.linked.call.CallInteraction;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallInteraction onCallInteraction} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallInteractionListener extends LinkedListener {
    /**
     * Notifies the listener that a peer broadcast an in-call
     * interaction (emoji reaction, raise/lower hand, peer-mute
     * request, or keyframe request).
     *
     * @param whatsapp    the client emitting the event
     * @param callId      the identifier of the call
     * @param fromJid     the JID of the participant that sent the
     *                    interaction
     * @param interaction the typed interaction payload
     */
    void onCallInteraction(LinkedWhatsAppClient whatsapp, String callId, Jid fromJid, CallInteraction interaction);
}
