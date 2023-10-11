package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.QuotedMessageInfo;

public interface OnMessageReply extends Listener {
    /**
     * Called when a message answers a previous message
     *
     * @param info   the answer message
     * @param quoted the quoted message
     */
    @Override
    void onMessageReply(ChatMessageInfo info, QuotedMessageInfo quoted);
}