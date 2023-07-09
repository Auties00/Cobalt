package it.auties.whatsapp.exception;

import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

/**
 * This exception is thrown when a phone number cannot be registered by the Whatsapp API
 */
public class RegistrationException extends RuntimeException{
    private final VerificationCodeResponse erroneousResponse;

    public RegistrationException(VerificationCodeResponse erroneousResponse){
        this.erroneousResponse = erroneousResponse;
    }

    public RegistrationException(String message) {
        super(message);
        this.erroneousResponse = null;
    }

    /**
     * Returns the response that caused the error, if available
     *
     * @return an optional
     */
    public Optional<VerificationCodeResponse> erroneousResponse() {
        return Optional.ofNullable(erroneousResponse);
    }

    @Override
    public String getMessage() {
        if(erroneousResponse != null){
            return erroneousResponse.toString();
        }

        return super.getMessage();
    }
}
