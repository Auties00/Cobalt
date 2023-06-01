package it.auties.whatsapp.exception;

import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * This exception is thrown when a phone number cannot be registered by the Whatsapp API
 */
@AllArgsConstructor
@Accessors(fluent = true)
public class RegistrationException extends RuntimeException{
    /**
     * The response that caused the error
     */
    @Getter
    private final VerificationCodeResponse erroneousResponse;

    @Override
    public String getMessage() {
        return erroneousResponse.toString();
    }
}
