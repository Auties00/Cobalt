package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnWhatsappNewStatus extends Listener {
    /**
     * Called when {@link SocketHandler} receives a new status from WhatsappWeb's Socket
     *
     * @param whatsapp an instance to the calling api
     * @param status the new status message
     */
    @Override
    void onNewStatus(Whatsapp whatsapp, MessageInfo status);
}