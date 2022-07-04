package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;

public interface OnWhatsappChatMessages extends Listener {
    /**
     * Called when the socket receives the recent message for a chat
     *
     * @param whatsapp an instance to the calling api
     * @param chat the chat
     * @param last whether the messages in this chat are complete or there are more coming
     */
    @Override
    void onChatMessages(Whatsapp whatsapp, Chat chat, boolean last);
}