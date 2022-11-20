package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnDisconnected extends Listener {
    /**
     * Called when the socket successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param reason the reason why the session was disconnected
     */
    @Override
    void onDisconnected(DisconnectReason reason);
}

