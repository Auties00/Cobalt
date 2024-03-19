package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;

public interface OnMessageStatus extends Listener {
    /**
     * Called when the status of a message changes
     *
     * @param info the message whose status changed
     */
    @Override
    void onMessageStatus(MessageInfo info);
}
