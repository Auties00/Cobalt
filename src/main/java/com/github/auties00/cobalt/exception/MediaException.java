package com.github.auties00.cobalt.exception;

public abstract sealed class MediaException extends RuntimeException permits MediaDownloadException, MediaProcessingException, MediaUploadException {
    public MediaException(String message) {
        super(message);
    }

    public MediaException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaException(Throwable cause) {
        super(cause);
    }
}
