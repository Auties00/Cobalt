package it.auties.whatsapp.listener;

import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.request.Node;

public interface OnNodeSent extends Listener {
    /**
     * Called when {@link Socket} sends a node to Whatsapp
     *
     * @param outgoing the non-null node that was just sent
     */
    void onNodeSent(Node outgoing);
}