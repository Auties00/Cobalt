package it.auties.whatsapp.listener;

import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.request.Node;

public interface OnNodeReceived extends QrDiscardingListener {
    /**
     * Called when {@link Socket} receives a node from Whatsapp
     *
     * @param incoming the non-null node that was just received
     */
    void onNodeReceived(Node incoming);
}