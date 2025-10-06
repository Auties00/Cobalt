package com.github.auties00.cobalt.exception;

public final class MediaUploadException extends MediaException {
    public MediaUploadException(String message) {
        super(message);
    }

    public MediaUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaUploadException(Throwable cause) {
        super(cause);
    }
}
