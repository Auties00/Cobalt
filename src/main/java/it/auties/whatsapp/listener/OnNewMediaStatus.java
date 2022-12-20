package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;

public interface OnNewMediaStatus
        extends Listener {
    /**
     * Called when the socket receives a new status from WhatsappWeb's Socket
     *
     * @param status the new status message
     */
    @Override
    void onNewStatus(MessageInfo status);
}