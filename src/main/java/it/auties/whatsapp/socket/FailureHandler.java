package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.ErroneousNodeException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class FailureHandler {
    private final Socket socket;

    protected Node handleNodeFailure(Throwable throwable){
        var returnValue = getCause(throwable);
        handleFailure(ErrorHandler.Location.ERRONEOUS_NODE, throwable);
        return returnValue;
    }

    protected <T> T handleFailure(ErrorHandler.Location location, Throwable throwable) {
        var returnValue = getCause(throwable);
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

    private Node getCause(Throwable throwable) {
        return throwable instanceof ErroneousNodeException erroneousNodeException ?
                erroneousNodeException.error() :
                null;
    }
}
