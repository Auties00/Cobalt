package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The code-verification status of a WhatsApp Business phone number.
 *
 * <p>A number is {@link #VERIFIED} once its ownership has been confirmed through the verification-code
 * ceremony, and {@link #NOT_VERIFIED} until then. The {@link #UNKNOWN} constant guards against tokens
 * this client does not yet model.
 */
@ProtobufEnum
public enum CloudPhoneNumberCodeVerificationStatus {
    /**
     * A status that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The number's ownership has been confirmed.
     */
    VERIFIED(1),

    /**
     * The number's ownership has not been confirmed.
     */
    NOT_VERIFIED(2);

    /**
     * The protobuf-assigned numeric index for this status.
     */
    final int index;

    /**
     * Constructs a {@code CloudPhoneNumberCodeVerificationStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudPhoneNumberCodeVerificationStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudPhoneNumberCodeVerificationStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "VERIFIED"}, or {@code null}
     * @return the matching status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudPhoneNumberCodeVerificationStatus of(String input) {
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
     * Returns the WhatsApp wire token for this status.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this status.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
