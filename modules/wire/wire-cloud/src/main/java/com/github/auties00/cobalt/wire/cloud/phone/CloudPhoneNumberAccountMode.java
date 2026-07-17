package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The account mode of a WhatsApp Business phone number.
 *
 * <p>A number runs in {@link #LIVE} mode for production messaging or {@link #SANDBOX} mode for testing
 * against the sandbox environment. The {@link #UNKNOWN} constant guards against tokens this client does
 * not yet model.
 */
@ProtobufEnum
public enum CloudPhoneNumberAccountMode {
    /**
     * An account mode that this client does not recognise. Resolved for any token outside the modelled
     * set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * Production messaging.
     */
    LIVE(1),

    /**
     * Testing against the sandbox environment.
     */
    SANDBOX(2);

    /**
     * The protobuf-assigned numeric index for this account mode.
     */
    final int index;

    /**
     * Constructs a {@code CloudPhoneNumberAccountMode} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudPhoneNumberAccountMode(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudPhoneNumberAccountMode} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "LIVE"}, or {@code null}
     * @return the matching account mode, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudPhoneNumberAccountMode of(String input) {
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
     * Returns the WhatsApp wire token for this account mode.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this account mode.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
