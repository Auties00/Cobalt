package it.auties.whatsapp.exception;

/**
 * This exception is thrown when the read/write counter go out of sync for the current session
 */
public class SessionBadMacException extends RuntimeException {
    public SessionBadMacException() {
        super();
    }
}
