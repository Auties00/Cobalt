package it.auties.whatsapp.exception;

import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * This exception is thrown when a phone number cannot be registered by the Whatsapp API
 */
@AllArgsConstructor
@Getter
@Accessors(fluent = true)
public class RegistrationException extends RuntimeException{
    private final VerificationCodeResponse erroneousResponse;
    private final String erroneousRawResponse;


    @Override
    public String getMessage() {
        return erroneousRawResponse;
    }
}
