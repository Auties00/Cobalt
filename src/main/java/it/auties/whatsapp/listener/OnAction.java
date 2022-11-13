package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnAction extends Listener {
    /**
     * Called when {@link SocketHandler} receives an sync from Whatsapp.
     *
     * @param action the sync that was executed
     */
    @Override
    void onAction(Action action);
}
