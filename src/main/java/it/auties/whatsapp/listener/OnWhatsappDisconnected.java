package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;

public interface OnWhatsappDisconnected extends QrDiscardingListener {
    /**
     * Called when {@link Socket} successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param reconnect whether the connection is going to be re-established
     */
    void onDisconnected(Whatsapp whatsapp, boolean reconnect);
}

