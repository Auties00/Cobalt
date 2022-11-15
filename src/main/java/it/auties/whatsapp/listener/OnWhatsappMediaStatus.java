package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;

import java.util.Collection;

public interface OnWhatsappMediaStatus extends Listener {
    /**
     * Called when the socket receives all the status updated from WhatsappWeb's Socket.
     *
     * @param whatsapp an instance to the calling api
     * @param status the status
     */
    void onMediaStatus(Whatsapp whatsapp, Collection<MessageInfo> status);
}