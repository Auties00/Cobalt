package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.info.MessageIndexInfo;

public interface OnWhatsappAction
        extends Listener {
    /**
     * Called when the socket receives a sync from Whatsapp.
     *
     * @param whatsapp         an instance to the calling api
     * @param action           the sync that was executed
     * @param messageIndexInfo the data about this action
     */
    @Override
    void onAction(Whatsapp whatsapp, Action action, MessageIndexInfo messageIndexInfo);
}
