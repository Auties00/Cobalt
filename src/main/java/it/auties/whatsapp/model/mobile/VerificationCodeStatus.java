package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum VerificationCodeStatus {
    OK,
    SENT,
    ERROR;

    @JsonCreator
    public static VerificationCodeStatus of(String name) {
        return Arrays.stream(values()).filter(entry -> entry.name().equalsIgnoreCase(name)).findFirst().orElse(ERROR);
    }

    public boolean isSuccessful() {
        return this == OK || this == SENT;
    }
}
