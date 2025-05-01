package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.DisconnectReason;

public enum SocketState {
    WAITING,
    HANDSHAKE,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    LOGGED_OUT,
    RESTORE,
    BANNED,
    PAUSED;

    static SocketState of(DisconnectReason reason) {
        return switch (reason) {
            case DISCONNECTED -> DISCONNECTED;
            case RECONNECTING -> RECONNECTING;
            case LOGGED_OUT -> LOGGED_OUT;
            case BANNED -> BANNED;
            case RESTORE -> RESTORE;
        };
    }

    DisconnectReason toReason() {
        return switch (this) {
            case CONNECTED, RECONNECTING -> DisconnectReason.RECONNECTING;
            case WAITING, HANDSHAKE, DISCONNECTED, PAUSED -> DisconnectReason.DISCONNECTED;
            case LOGGED_OUT -> DisconnectReason.LOGGED_OUT;
            case RESTORE -> DisconnectReason.RESTORE;
            case BANNED -> DisconnectReason.BANNED;
        };
    }
}
