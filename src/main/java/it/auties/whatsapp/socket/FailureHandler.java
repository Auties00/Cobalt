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

        if (!socket.options()
                .errorHandler()
                .apply(location, throwable)) {
            return null;
        }

        if (failure.get()) {
            return null;
        }

        failure.set(true);
        socket.changeKeys();
        socket.disconnect(true);
        return null;
    }

    public AtomicBoolean failure() {
        return failure;
    }
}
