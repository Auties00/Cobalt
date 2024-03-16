package it.auties.whatsapp.registration.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;

record FcmRegistrationResponse(
        @JsonProperty
        String token,
        @JsonProperty
        String pushSet
) {

}
