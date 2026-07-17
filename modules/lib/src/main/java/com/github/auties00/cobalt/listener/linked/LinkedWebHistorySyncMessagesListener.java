package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.Chat;

import java.util.SequencedCollection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onWebHistorySyncMessages onWebHistorySyncMessages} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedWebHistorySyncMessagesListener extends LinkedListener {
    /**
     * Notifies the listener that messages for a batch of chats have been
     * received during history synchronization.
     *
     * <p>This event is only triggered during initial QR code scanning and
     * history syncing. On subsequent connections messages are already
     * loaded in the chats. The chats are delivered once per history-sync
     * chunk in their server-provided order; a single chunk may carry many
     * chats, and a full sync spans several chunks until {@code last} is
     * {@code true}.
     *
     * @param whatsapp the client emitting the event
     * @param chats    the chats synchronized in this chunk, in arrival order
     * @param last     {@code true} if this is the final chunk of the history
     *                 sync, {@code false} if more chunks are coming
     */
    void onWebHistorySyncMessages(LinkedWhatsAppClient whatsapp, SequencedCollection<Chat> chats, boolean last);
}
