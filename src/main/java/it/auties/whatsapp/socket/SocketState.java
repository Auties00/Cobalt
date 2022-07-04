package it.auties.whatsapp.socket;

enum SocketState {
    WAITING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    RESTORING_FAILURE;

    public boolean isConnected() {
        return this == CONNECTED || this == RESTORING_FAILURE;
    }
}
