package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.socket.Socket;

public interface OnWhatsappAction extends Listener {
    /**
     * Called when {@link Socket} receives an sync from Whatsapp.
     *
     * @param whatsapp an instance to the calling api
     * @param action the sync that was executed
     */
    @Override
    void onAction(Whatsapp whatsapp, Action action);
}
