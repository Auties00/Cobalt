package it.auties.whatsapp.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ErroneousBinaryRequest extends RuntimeException {
    private final Object error;

    public ErroneousBinaryRequest(String message, Object error) {
        super(message);
        this.error = error;
    }

    public ErroneousBinaryRequest(String message, Object error, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public Object error() {
        return error;
    }
}
