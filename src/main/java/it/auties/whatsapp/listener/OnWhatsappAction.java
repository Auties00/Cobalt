package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.action.Action;

public interface OnWhatsappAction extends QrDiscardingListener {
    /**
     * Called when {@link Socket} receives an sync from Whatsapp.
     *
     * @param action the sync that was executed
     */
    @Override
    void onAction(Whatsapp whatsapp, Action action);
}
