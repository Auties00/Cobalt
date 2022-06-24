package it.auties.whatsapp.listener;

import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.chat.Chat;

public interface OnChatRecentMessages extends QrDiscardingListener {
    /**
     * Called when {@link Socket} receives the recent message for a chat.
     * This method may be called multiple times depending on the chat's size.
     */
    void onChatRecentMessages(Chat chat);
}