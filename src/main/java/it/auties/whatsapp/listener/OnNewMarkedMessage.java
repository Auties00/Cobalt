package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;

public interface OnNewMarkedMessage extends Listener {
    /**
     * Called when a new message is received in a chat
     *
     * @param info the message that was sent
     * @param offline  whether this message was received while the client was offline
     */
    @Override
    void onNewMessage(MessageInfo info, boolean offline);
}