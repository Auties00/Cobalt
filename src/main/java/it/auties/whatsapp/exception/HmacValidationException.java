package it.auties.whatsapp.exception;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An unchecked exception that is thrown when a hmac signature cannot be validated
 */
public class HmacValidationException extends SecurityException {
    public HmacValidationException(@NonNull String location) {
        super(location);
    }
}
