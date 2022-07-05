package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.api.Whatsapp;

public interface OnWhatsappSocketEvent extends Listener {
    /**
     * Called when an event regarding the underlying is fired
     *
     * @param whatsapp an instance to the calling api
     * @param event    the event
     */
    @Override
    void onSocketEvent(Whatsapp whatsapp, SocketEvent event);
}
