package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.socket.Socket;

public interface OnNodeReceived extends Listener {
    /**
     * Called when {@link Socket} receives a node from Whatsapp
     *
     * @param incoming the non-null node that was just received
     */
    void onNodeReceived(Node incoming);
}