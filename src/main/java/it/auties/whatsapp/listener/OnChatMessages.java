package it.auties.whatsapp.listener;

import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.chat.Chat;

public interface OnChatMessages extends Listener {
    /**
     * Called when the socket receives the recent message for a chat
     *
     * @param chat the chat
     * @param last whether the messages in this chat are complete or there are more coming
     */
    @Override
    void onChatMessages(Chat chat, boolean last);
}