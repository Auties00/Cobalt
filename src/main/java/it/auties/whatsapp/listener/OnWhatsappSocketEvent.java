package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.api.Whatsapp;

public interface OnWhatsappSocketEvent extends Listener {
    @Override
    void onSocketEvent(Whatsapp whatsapp, SocketEvent event);
}
