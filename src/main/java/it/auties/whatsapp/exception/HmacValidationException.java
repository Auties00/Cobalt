package it.auties.whatsapp.exception;

import lombok.NonNull;

public class HmacValidationException extends SecurityException {
    public HmacValidationException(@NonNull String location) {
        super(location);
    }
}
