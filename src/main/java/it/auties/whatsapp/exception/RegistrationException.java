package it.auties.whatsapp.exception;

import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * This exception is thrown when a phone number cannot be registered by the Whatsapp API
 */
public class RegistrationException extends RuntimeException {
    @Nullable
    private final VerificationCodeResponse erroneousResponse;

    public RegistrationException(@Nullable VerificationCodeResponse erroneousResponse, String message) {
        super(message);
        this.erroneousResponse = erroneousResponse;
    }

    public Optional<VerificationCodeResponse> erroneousResponse() {
        return Optional.ofNullable(erroneousResponse);
    }
}
