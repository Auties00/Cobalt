package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;

public interface OnNewMessage extends QrDiscardingListener {
    /**
     * Called when a new message is received in a chat
     *
     * @param info the message that was sent
     */
    void onNewMessage(MessageInfo info);
}