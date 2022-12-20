package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.Whatsapp;

public interface OnWhatsappDisconnected
        extends Listener {
    /**
     * Called when the socket successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param whatsapp an instance to the calling api
     * @param reason   the reason why the session was disconnected
     */
    @Override
    void onDisconnected(Whatsapp whatsapp, DisconnectReason reason);
}

