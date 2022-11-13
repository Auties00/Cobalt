package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnNodeReceived extends Listener {
    /**
     * Called when {@link SocketHandler} receives a node from Whatsapp
     *
     * @param incoming the non-null node that was just received
     */
    @Override
    void onNodeReceived(Node incoming);
}