package com.github.auties00.cobalt.wire.cloud.template;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The delivery mode of a WhatsApp Cloud API authentication-template one-time-passcode button.
 *
 * <p>An OTP button delivers a verification code to the recipient in one of three modes:
 * {@link #COPY_CODE} shows a button that copies the code to the clipboard, {@link #ONE_TAP} autofills
 * the code into the requesting app with a single tap, and {@link #ZERO_TAP} delivers the code directly
 * to the app without any user interaction. The {@link #UNKNOWN} constant guards against tokens this
 * client does not yet model.
 */
@ProtobufEnum
public enum CloudOtpType {
    /**
     * A delivery mode that this client does not recognise. Resolved for any token outside the modelled
     * set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The button copies the verification code to the clipboard when tapped.
     */
    COPY_CODE(1),

    /**
     * The button autofills the verification code into the requesting app with a single tap.
     */
    ONE_TAP(2),

    /**
     * The verification code is delivered directly to the requesting app without user interaction.
     */
    ZERO_TAP(3);

    /**
     * The protobuf-assigned numeric index for this delivery mode.
     */
    final int index;

    /**
     * Constructs a {@code CloudOtpType} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudOtpType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudOtpType} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "COPY_CODE"}, or {@code null}
     * @return the matching delivery mode, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudOtpType of(String input) {
        if (input == null) {
            return UNKNOWN;
        }
        for (var value : values()) {
            if (value != UNKNOWN && value.name().equalsIgnoreCase(input)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the WhatsApp wire token for this delivery mode.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this delivery mode.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
