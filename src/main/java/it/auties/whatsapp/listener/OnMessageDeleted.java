package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.MessageInfo;

public interface OnMessageDeleted extends Listener {
    /**
     * Called when a message is deleted
     *
     * @param info     the message that was deleted
     * @param everyone whether this message was deleted by you only for yourself or whether the
     *                 message was permanently removed
     */
    @Override
    void onMessageDeleted(MessageInfo info, boolean everyone);
}