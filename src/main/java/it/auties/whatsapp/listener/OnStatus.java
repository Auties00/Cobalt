package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.ChatMessageInfo;

import java.util.Collection;

public interface OnStatus extends Listener {
    /**
     * Called when the socket receives all the status updated from WhatsappWeb's Socket.
     *
     * @param status the status
     */
    void onStatus(Collection<ChatMessageInfo> status);
}