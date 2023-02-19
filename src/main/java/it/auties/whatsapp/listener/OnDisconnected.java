package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.DisconnectReason;

public interface OnDisconnected extends Listener {
    /**
     * Called when the socket successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param reason the errorReason why the session was disconnected
     */
    @Override
    void onDisconnected(DisconnectReason reason);
}

