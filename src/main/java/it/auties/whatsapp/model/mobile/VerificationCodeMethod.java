package it.auties.whatsapp.model.mobile;

import it.auties.whatsapp.api.ClientType;

/**
 * The constants of this enumerated type describe the various types of verification that can be used to receive the OTP required for an {@link ClientType#MOBILE}
 */
public enum VerificationCodeMethod {
    /**
     * Do not ask for a new verification code as you already have one
     */
    NONE("none"),

    /**
     * An SMS containing the code will be sent to the associated phone number
     */
    SMS("sms"),

    /**
     * A call will be received from the associated phone number
     */
    CALL("voice"),

    /**
     * A message containing the OTP will be sent via Whatsapp to an active device
     */
    WHATSAPP("email_otp");

    private final String type;

    VerificationCodeMethod(String type) {
        this.type = type;
    }

    public String type() {
        return this.type;
    }
}
