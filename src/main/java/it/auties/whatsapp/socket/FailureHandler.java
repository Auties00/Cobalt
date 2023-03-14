package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.ErrorHandler;

class FailureHandler {
    private final SocketHandler socketHandler;

    protected FailureHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    protected <T> T handleFailure(ErrorHandler.Location location, Throwable throwable) {
        if (socketHandler.state() == SocketState.RESTORE || socketHandler.state() == SocketState.LOGGED_OUT) {
            return null;
        }
        var result = socketHandler.options().errorHandler().apply(location, throwable);
        switch (result) {
            case RESTORE -> socketHandler.disconnect(DisconnectReason.RESTORE);
            case LOG_OUT -> socketHandler.disconnect(DisconnectReason.LOGGED_OUT);
            case DISCONNECT -> socketHandler.disconnect(DisconnectReason.DISCONNECTED);
            case RECONNECT -> socketHandler.disconnect(DisconnectReason.RECONNECTING);
        }
        return null;
    }
}
