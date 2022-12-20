package it.auties.whatsapp.listener;

import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.chat.Chat;

import java.util.Collection;

public interface OnChats
        extends Listener {
    /**
     * Called when the socket receives all the chats from WhatsappWeb's WebSocket.
     * When this event is fired, it is guaranteed that all metadata excluding messages will be present.
     * To access this data use {@link Store#chats()}.
     * If you also need the messages to be loaded, please refer to {@link Listener#onChatMessagesSync(Chat, boolean)}.
     * Particularly old chats may come later through {@link Listener#onChatMessagesSync(Chat, boolean)}.
     *
     * @param chats the chats
     */
    @Override
    void onChats(Collection<Chat> chats);
}