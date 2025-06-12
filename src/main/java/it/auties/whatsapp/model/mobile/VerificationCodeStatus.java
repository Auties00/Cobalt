package it.auties.whatsapp.model.mobile;

public enum VerificationCodeStatus {
    SUCCESS,
    ERROR;

    public static VerificationCodeStatus of(String name) {
        return name.equalsIgnoreCase("ok")
                || name.equalsIgnoreCase("sent")
                || name.equalsIgnoreCase("verified") ? SUCCESS : ERROR;
    }
}
