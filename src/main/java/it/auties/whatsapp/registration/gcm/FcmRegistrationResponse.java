package it.auties.whatsapp.registration.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FcmRegistrationResponse(
        @JsonProperty
        String token,
        @JsonProperty
        String pushSet
) {

}
