package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;

public interface OnWhatsappMessageStatus extends Listener {
    /**
     * Called when the status of a message changes
     *
     * @param whatsapp an instance to the calling api
     * @param info     the message whose status changed
     */
    @Override
    void onMessageStatus(Whatsapp whatsapp, MessageInfo info);
}
