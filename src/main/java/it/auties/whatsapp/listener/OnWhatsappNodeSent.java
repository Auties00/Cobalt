package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.request.Node;

public interface OnWhatsappNodeSent extends Listener {
    /**
     * Called when the socket sends a node to Whatsapp
     *
     * @param whatsapp an instance to the calling api
     * @param outgoing the non-null node that was just sent
     */
    @Override
    void onNodeSent(Whatsapp whatsapp, Node outgoing);
}