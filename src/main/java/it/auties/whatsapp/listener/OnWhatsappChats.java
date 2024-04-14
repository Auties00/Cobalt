package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;

import java.util.Collection;

public interface OnWhatsappChats extends Listener {
    /**
     * Called when the socket receives all the chats from WhatsappWeb's Socket. When this event is
     * fired, it is guaranteed that all metadata excluding messages will be present. If you also need
     * the messages to be loaded, please refer to {@link Listener#onChatMessagesSync(Chat, boolean)}.
     * Particularly old chats may come later through
     * {@link Listener#onChatMessagesSync(Chat, boolean)}
     *
     * @param whatsapp an instance to the calling api
     * @param chats    the chats
     */
    @Override
    void onChats(Whatsapp whatsapp, Collection<Chat> chats);
}