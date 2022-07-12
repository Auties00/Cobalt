package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.ErroneousNodeException;
import it.auties.whatsapp.util.JacksonProvider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class FailureHandler implements JacksonProvider {
    private final Socket socket;

    protected Node handleNodeFailure(Throwable throwable) {
        handleFailure(ErrorHandler.Location.ERRONEOUS_NODE, throwable);
        return throwable instanceof ErroneousNodeException erroneousNodeException ?
                erroneousNodeException.error() :
                null;
    }

    protected <T> T handleFailure(ErrorHandler.Location location, Throwable throwable) {
        if (socket.state() == SocketState.RESTORING_FAILURE) {
            return null;
        }

        if (!socket.options()
                .errorHandler()
                .apply(location, throwable)) {
            return null;
        }

        socket.state(SocketState.RESTORING_FAILURE);
        socket.store()
                .clear();
        socket.changeKeys();
        socket.reconnect();
        return null;
    }
}
