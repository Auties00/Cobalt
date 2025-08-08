package it.auties.whatsapp.exception;

/**
 * This exception is thrown when someone else logs in using the same keys the current session is using
 */
public class SessionConflictException extends RuntimeException {
    public SessionConflictException() {
        super();
    }
}
