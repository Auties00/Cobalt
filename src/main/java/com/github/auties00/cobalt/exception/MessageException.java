package com.github.auties00.cobalt.exception;

public final class MessageException extends RuntimeException{
    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
