package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.SocketEvent;

public interface OnSocketEvent extends QrDiscardingListener {
    @Override
    void onSocketEvent(SocketEvent event);
}
