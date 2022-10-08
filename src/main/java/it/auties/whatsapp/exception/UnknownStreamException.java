package it.auties.whatsapp.exception;

import lombok.NonNull;

public class UnknownStreamException extends RuntimeException {
    public UnknownStreamException(@NonNull String reason) {
        super(reason);
    }
}
