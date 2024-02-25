package it.auties.whatsapp.exception;


/**
 * An unchecked exception that is thrown when a hmac signature cannot be validated
 */
public class HmacValidationException extends SecurityException {
    public HmacValidationException(String location) {
        super(location);
    }
}
