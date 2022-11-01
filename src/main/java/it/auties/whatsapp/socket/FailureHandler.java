package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.exception.ErroneousNodeException;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.JacksonProvider;

import java.util.concurrent.atomic.AtomicBoolean;

class FailureHandler implements JacksonProvider {
    private final Socket socket;
    private final AtomicBoolean failure;
    public FailureHandler(Socket socket){
        this.socket = socket;
        this.failure = new AtomicBoolean();
    }

    protected Node handleNodeFailure(Throwable throwable) {
        handleFailure(ErrorHandler.Location.ERRONEOUS_NODE, throwable);
        return throwable instanceof ErroneousNodeException erroneousNodeException ?
                erroneousNodeException.error() :
                null;
    }

    protected <T> T handleFailure(ErrorHandler.Location location, Throwable throwable) {
        if(location == ErrorHandler.Location.ERRONEOUS_NODE && failure.get()){
            return null;
        }

        var result = socket.options()
                .errorHandler()
                .apply(location, throwable);
        if(failure.get()){
            return null;
        }

        if(result != ErrorHandler.Result.DISCARD){
            failure.set(true);
        }

        switch (result) {
            case RESTORE -> {
                socket.changeKeys();
                socket.disconnect(true);
            }
            case LOG_OUT -> {
                socket.changeKeys();
                socket.disconnect(false);
            }
            case DISCONNECT -> socket.disconnect(false);
            case RECONNECT -> socket.disconnect(true);
        }

        return null;
    }

    public AtomicBoolean failure() {
        return failure;
    }
}
