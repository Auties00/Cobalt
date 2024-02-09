package it.auties.whatsapp.api;

import it.auties.whatsapp.model.response.VerificationCodeResponse;

import java.util.Optional;

public record MobileRegistrationResult(Whatsapp whatsapp, Optional<VerificationCodeResponse> response) {

}
