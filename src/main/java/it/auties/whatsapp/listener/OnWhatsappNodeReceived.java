package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnWhatsappNodeReceived extends Listener {
    /**
     * Called when {@link SocketHandler} receives a node from Whatsapp
     *
     * @param whatsapp an instance to the calling api
     * @param incoming the non-null node that was just received
     */
    @Override
    void onNodeReceived(Whatsapp whatsapp, Node incoming);
}