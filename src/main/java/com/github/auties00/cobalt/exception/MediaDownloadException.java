package com.github.auties00.cobalt.exception;

public final class MediaDownloadException extends MediaException {
    public MediaDownloadException(String message) {
        super(message);
    }

    public MediaDownloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaDownloadException(Throwable cause) {
        super(cause);
    }
}
