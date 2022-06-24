package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.chat.Chat;

public interface OnWhatsappChatRecentMessages extends QrDiscardingListener {
    /**
     * Called when {@link Socket} receives the recent message for a chat.
     * This method may be called multiple times depending on the chat's size.
     */
    void onChatRecentMessages(Whatsapp whatsapp, Chat chat);
}