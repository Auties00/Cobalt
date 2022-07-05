package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.socket.Socket;

public interface OnDisconnected extends Listener {
    /**
     * Called when {@link Socket} successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param reason   the reason why the session was disconnected
     */
    @Override
    void onDisconnected(DisconnectReason reason);
}

