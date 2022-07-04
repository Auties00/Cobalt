package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.ErrorHandler;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class FailureHandler {
    private final Socket socket;

    protected <T> T handleFailure(ErrorHandler.Location location, Throwable throwable) {
        if (socket.state() == SocketState.RESTORING_FAILURE) {
            return null;
        }

        if (!socket.options().errorHandler()
                .apply(location, throwable)) {
            return null;
        }

        socket.state(SocketState.RESTORING_FAILURE);
        socket.store().clear();
        socket.changeKeys();
        socket.reconnect();
        return null;
    }
}
