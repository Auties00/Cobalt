package it.auties.whatsapp.model.mobile;

import io.avaje.jsonb.Json;

public enum VerificationCodeStatus {
    SUCCESS,
    ERROR;

    @Json.Creator
    static VerificationCodeStatus of(String name) {
        return name.equalsIgnoreCase("ok")
                || name.equalsIgnoreCase("sent")
                || name.equalsIgnoreCase("verified") ? SUCCESS : ERROR;
    }
}
