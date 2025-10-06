package com.github.auties00.cobalt.exception;

public final class MediaProcessingException extends MediaException {
    public MediaProcessingException(String message) {
        super(message);
    }

    public MediaProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaProcessingException(Throwable cause) {
        super(cause);
    }
}
