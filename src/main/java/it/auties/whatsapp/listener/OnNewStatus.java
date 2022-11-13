package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnNewStatus extends Listener {
    /**
     * Called when {@link SocketHandler} receives a new status from WhatsappWeb's Socket
     *
     * @param status the new status message
     */
    @Override
    void onNewStatus(MessageInfo status);
}