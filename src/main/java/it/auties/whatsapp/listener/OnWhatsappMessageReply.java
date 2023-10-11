package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.QuotedMessageInfo;

public interface OnWhatsappMessageReply extends Listener {
    /**
     * Called when a message answers a previous message
     *
     * @param whatsapp an instance to the calling api
     * @param info     the answer message
     * @param quoted   the quoted message
     */
    @Override
    void onMessageReply(Whatsapp whatsapp, ChatMessageInfo info, QuotedMessageInfo quoted);
}