package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.Chat;

import java.util.Collection;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onChats onChats} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedChatsListener extends LinkedListener {
    /**
     * Notifies the listener that the full chat list has been received
     * from WhatsApp.
     *
     * <p>Fires exactly once per login, after the chat metadata is
     * complete and independently of message backfill. On a fresh
     * session it fires after the initial-bootstrap history-sync chunk
     * has been processed; on a reconnect it fires from the cached
     * store immediately after the {@code <success>} stanza. Message
     * content for each chat continues to stream in through
     * {@link LinkedWebHistorySyncMessagesListener#onWebHistorySyncMessages onWebHistorySyncMessages}
     * after this callback returns; particularly old chats may also be
     * discovered later through the history-sync process. The callback
     * fires even when the chat list is empty.
     *
     * @param whatsapp the client emitting the event
     * @param chats    the collection of chats
     */
    void onChats(LinkedWhatsAppClient whatsapp, Collection<Chat> chats);
}
