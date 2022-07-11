package it.auties.whatsapp.util;

import it.auties.whatsapp.model.request.Node;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ErroneousNodeException extends RuntimeException {
    private final Node error;
    public ErroneousNodeException(String message, Node error){
        super(message);
        this.error = error;
    }

    public ErroneousNodeException(String message, Node error, Throwable cause){
        super(message, cause);
        this.error = error;
    }

    public Node error() {
        return error;
    }
}
