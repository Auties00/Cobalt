package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.SocketEvent;

public interface OnSocketEvent extends Listener {
    @Override
    void onSocketEvent(SocketEvent event);
}
