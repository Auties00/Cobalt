package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.socket.Socket;

public interface OnNewStatus extends Listener {
    /**
     * Called when {@link Socket} receives a new status from WhatsappWeb's Socket
     *
     * @param status the new status message
     */
    void onNewStatus(MessageInfo status);
}