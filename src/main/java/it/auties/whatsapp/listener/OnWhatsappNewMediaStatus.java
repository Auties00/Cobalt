package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnWhatsappNewMediaStatus extends Listener {
    /**
     * Called when the socket receives a new status from WhatsappWeb's Socket
     *
     * @param whatsapp an instance to the calling api
     * @param status the new status message
     */
    @Override
    void onNewStatus(Whatsapp whatsapp, MessageInfo status);
}