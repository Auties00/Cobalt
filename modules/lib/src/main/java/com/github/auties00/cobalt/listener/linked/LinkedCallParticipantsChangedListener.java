package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallParticipantsChanged onCallParticipantsChanged} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallParticipantsChangedListener extends LinkedListener {
    /**
     * Notifies the listener that participants have been added to or removed
     * from an in-progress group call.
     *
     * @param whatsapp     the client emitting the event
     * @param callId       the identifier of the group call
     * @param groupJid     the group JID that owns the call
     * @param participants the participants that were added or removed
     * @param added        {@code true} if the participants were added,
     *                     {@code false} if they were removed
     */
    void onCallParticipantsChanged(LinkedWhatsAppClient whatsapp, String callId, Jid groupJid, List<Jid> participants, boolean added);
}
