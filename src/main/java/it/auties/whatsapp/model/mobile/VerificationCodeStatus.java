package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum VerificationCodeStatus {
    SUCCESS,
    ERROR;

    @JsonCreator
    public static VerificationCodeStatus of(String name) {
        return name.equalsIgnoreCase("ok")
                || name.equalsIgnoreCase("sent")
                || name.equalsIgnoreCase("verified") ? SUCCESS : ERROR;
    }
}
