package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.api.ClientType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of verification that can be used to receive the OTP required for an {@link ClientType#MOBILE}
 */
@AllArgsConstructor
@Accessors(fluent = true)
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
    CALL("voice");

    @Getter
    private final String type;

    @JsonCreator
    public static VerificationCodeMethod of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.type().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
