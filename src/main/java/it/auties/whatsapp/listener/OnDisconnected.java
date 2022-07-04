package it.auties.whatsapp.listener;

import it.auties.whatsapp.socket.Socket;

public interface OnDisconnected extends Listener {
    /**
     * Called when {@link Socket} successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param reconnect whether the connection is going to be re-established
     */
    void onDisconnected(boolean reconnect);
}

