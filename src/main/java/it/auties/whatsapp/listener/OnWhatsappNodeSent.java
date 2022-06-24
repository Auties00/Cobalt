package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.request.Node;

public interface OnWhatsappNodeSent extends QrDiscardingListener {
    /**
     * Called when {@link Socket} sends a node to Whatsapp
     *
     * @param outgoing the non-null node that was just sent
     */
    void onNodeSent(Whatsapp whatsapp, Node outgoing);
}