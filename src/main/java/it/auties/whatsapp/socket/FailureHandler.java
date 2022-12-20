package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.exception.ErroneousNodeRequestException;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.JacksonProvider;

import java.util.concurrent.atomic.AtomicBoolean;

class FailureHandler
        implements JacksonProvider {
    private final SocketHandler socketHandler;
    private final AtomicBoolean failure;

    public FailureHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
        this.failure = new AtomicBoolean();
    }

    protected Node handleNodeFailure(Throwable throwable) {
        handleFailure(ErrorHandler.Location.ERRONEOUS_NODE, throwable);
        return throwable instanceof ErroneousNodeRequestException erroneousNodeException ?
                erroneousNodeException.error() :
                null;
    }

    protected <T> T handleFailure(ErrorHandler.Location location, Throwable throwable) {
        if (location == ErrorHandler.Location.ERRONEOUS_NODE && failure.get()) {
            return null;
        }

        var result = socketHandler.options()
                .errorHandler()
                .apply(location, throwable);
        if (failure.get()) {
            return null;
        }

        if (result != ErrorHandler.Result.DISCARD) {
            failure.set(true);
        }

        switch (result) {
            case RESTORE -> {
                socketHandler.changeKeys();
                socketHandler.disconnect(true);
            }
            case LOG_OUT -> {
                socketHandler.changeKeys();
                socketHandler.disconnect(false);
            }
            case DISCONNECT -> socketHandler.disconnect(false);
            case RECONNECT -> socketHandler.disconnect(true);
        }

        return null;
    }

    public AtomicBoolean failure() {
        return failure;
    }
}
