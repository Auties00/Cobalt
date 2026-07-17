package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.Chat;

import java.util.Collection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onGroups onGroups} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedGroupsListener extends LinkedListener {
    /**
     * Notifies the listener that the full groups list has been
     * refreshed against the server.
     *
     * @apiNote
     * Fires each time {@link LinkedWhatsAppClient#refreshGroups()} commits
     * a fresh server-authoritative view of the groups this account
     * participates in. Use to redraw the groups section of the chat
     * list against the new authoritative set.
     *
     * @param whatsapp the client emitting the event
     * @param groups   the collection of groups
     */
    void onGroups(LinkedWhatsAppClient whatsapp, Collection<Chat> groups);
}
