package it.auties.whatsapp.exception;

import it.auties.whatsapp.model.mobile.VerificationCodeResponse;

/**
 * This exception is thrown when a phone number cannot be registered by the Whatsapp API
 */
public class RegistrationException extends RuntimeException {
    private final VerificationCodeResponse erroneousResponse;
    private final String erroneousRawResponse;

    public RegistrationException(VerificationCodeResponse erroneousResponse, String erroneousRawResponse) {
        this.erroneousResponse = erroneousResponse;
        this.erroneousRawResponse = erroneousRawResponse;
    }

    public VerificationCodeResponse erroneousResponse() {
        return this.erroneousResponse;
    }

    public String erroneousRawResponse() {
        return this.erroneousRawResponse;
    }

    @Override
    public String getMessage() {
        return erroneousRawResponse;
    }
}
