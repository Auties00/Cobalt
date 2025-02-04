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
    NONE("", false),

    /**
     * An SMS containing the code will be sent to the associated phone number
     * If the SMS cannot be sent because the number is already registered, an OTP will be sent instead
     */
    SMS("sms", true),

    /**
     * An SMS containing the code will be sent to the associated phone number
     * If the SMS cannot be sent because the number is already registered, an OTP will be sent instead
     */
    SMS_NO_FALLBACK("sms", false),

    /**
     * A call will be received from the associated phone number
     * If the SMS cannot be sent because the number is already registered, an OTP will be sent instead
     */
    CALL("voice", true),

    /**
     * A call will be received from the associated phone number
     */
    CALL_NO_FALLBACK("voice", false),

    /**
     * A message containing the OTP will be sent via Whatsapp to an active device
     */
    WHATSAPP("wa_old", false);

    private final String data;
    private final boolean fallback;

    VerificationCodeMethod(String data, boolean fallback) {
        this.data = data;
        this.fallback = fallback;
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

    public boolean hasFallback() {
        return fallback;
    }
}
