package it.auties.whatsapp.exception;

import it.auties.whatsapp.model.response.RegistrationResponse;

import java.util.Optional;

/**
 * This exception is thrown when a phone number cannot be registered by the Whatsapp API
 */
public class RegistrationException extends RuntimeException {
    private final RegistrationResponse erroneousResponse;

    public RegistrationException(RegistrationResponse erroneousResponse, String message) {
        super(message);
        this.erroneousResponse = erroneousResponse;
    }

    public Optional<RegistrationResponse> erroneousResponse() {
        return Optional.ofNullable(erroneousResponse);
    }
}
