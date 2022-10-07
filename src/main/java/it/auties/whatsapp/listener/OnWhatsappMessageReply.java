package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.QuotedMessage;

public interface OnWhatsappMessageReply extends Listener {
    /**
     * Called when a message answers a previous message
     *
     * @param whatsapp an instance to the calling api
     * @param info   the answer message
     * @param quoted the quoted message
     */
    @Override
    void onMessageReply(Whatsapp whatsapp, MessageInfo info, QuotedMessage quoted);
}