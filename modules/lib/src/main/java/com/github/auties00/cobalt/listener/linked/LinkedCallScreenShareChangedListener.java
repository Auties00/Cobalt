package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.call.CallScreenShareState;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallScreenShareChanged onCallScreenShareChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallScreenShareChangedListener extends LinkedListener {
    /**
     * Notifies the listener that a call participant started, stopped, or failed a
     * screen share.
     *
     * @param whatsapp       the client emitting the event
     * @param callId         the identifier of the call
     * @param participantJid the JID of the participant whose screen share state changed
     * @param state          the new screen share state
     */
    void onCallScreenShareChanged(LinkedWhatsAppClient whatsapp, String callId, Jid participantJid, CallScreenShareState state);
}
