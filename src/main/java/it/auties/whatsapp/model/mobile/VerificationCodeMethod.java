package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.api.ClientType;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of verification that can be used to receive the OTP required for an {@link ClientType#MOBILE}
 */
public enum VerificationCodeMethod {
    /**
     * No verification is needed
     */
    NONE(""),

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
    WHATSAPP("wa_old");

    private final String data;

    VerificationCodeMethod(String data) {
        this.data = data;
    }

    @JsonCreator
    public static VerificationCodeMethod of(String type) {
        return Arrays.stream(values())
                .filter(entry -> entry.data().equals(type))
                .findFirst()
                .orElse(NONE);
    }

    public String data() {
        return this.data;
    }
}
