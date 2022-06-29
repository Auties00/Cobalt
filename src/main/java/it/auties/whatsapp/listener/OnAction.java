package it.auties.whatsapp.listener;

import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.action.Action;

public interface OnAction extends Listener {
    /**
     * Called when {@link Socket} receives an sync from Whatsapp.
     *
     * @param action the sync that was executed
     */
    @Override
    void onAction(Action action);
}
