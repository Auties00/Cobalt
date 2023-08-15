package it.auties.whatsapp.exception;

import it.auties.whatsapp.socket.Request;

import java.io.IOException;

public class RequestException extends IOException {
    private final Request request;

    public RequestException(Request request, String message, Throwable cause) {
        super(message, cause);
        this.request = request;
    }

    public RequestException(Request request, String message) {
        super(message);
        this.request = request;
    }

    public RequestException(Request request, Throwable cause) {
        super(cause);
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }
}
