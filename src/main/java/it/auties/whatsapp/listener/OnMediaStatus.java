package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;

import java.util.Collection;

public interface OnMediaStatus extends Listener {
    /**
     * Called when the socket receives all the status updated from WhatsappWeb's Socket.
     *
     * @param status the status
     */
    void onMediaStatus(Collection<MessageInfo> status);
}