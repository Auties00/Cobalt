package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.QuotedMessage;

public interface OnMessageReply
        extends Listener {
    /**
     * Called when a message answers a previous message
     *
     * @param info   the answer message
     * @param quoted the quoted message
     */
    @Override
    void onMessageReply(MessageInfo info, QuotedMessage quoted);
}