package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupPastParticipant;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Collection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onWebHistorySyncPastParticipants onWebHistorySyncPastParticipants} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedWebHistorySyncPastParticipantsListener extends LinkedListener {
    /**
     * Notifies the listener that past participants for a group have been
     * received during history synchronization.
     *
     * @param whatsapp              the client emitting the event
     * @param chatJid               the group chat JID
     * @param groupPastParticipants the collection of past participants
     */
    void onWebHistorySyncPastParticipants(LinkedWhatsAppClient whatsapp, Jid chatJid, Collection<GroupPastParticipant> groupPastParticipants);
}
