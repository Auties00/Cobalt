package it.auties.whatsapp.exception;

/**
 * This exception is thrown when a malformed jid is parsed
 */
public class MalformedJidException extends RuntimeException {
    public MalformedJidException(String message) {
        super(message);
    }
}
