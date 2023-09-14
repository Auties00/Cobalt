package it.auties.whatsapp.model.mobile;

public enum VerificationCodeStatus {
    OK,
    SENT,
    ERROR;

    public boolean isSuccessful() {
        return this == OK || this == SENT;
    }
}
