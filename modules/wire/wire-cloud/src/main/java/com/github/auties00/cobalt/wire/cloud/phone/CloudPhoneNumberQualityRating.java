package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The messaging quality rating of a WhatsApp Business phone number.
 *
 * <p>Meta tracks the quality of a number's outbound messaging on a three-step scale: {@link #GREEN}
 * for high quality, {@link #YELLOW} for medium quality, and {@link #RED} for low quality. A sustained
 * low rating can lead to messaging-limit downgrades. The {@link #UNKNOWN} constant guards against
 * tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudPhoneNumberQualityRating {
    /**
     * A quality rating that this client does not recognise. Resolved for any token outside the modelled
     * set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * High messaging quality.
     */
    GREEN(1),

    /**
     * Medium messaging quality.
     */
    YELLOW(2),

    /**
     * Low messaging quality.
     */
    RED(3);

    /**
     * The protobuf-assigned numeric index for this quality rating.
     */
    final int index;

    /**
     * Constructs a {@code CloudPhoneNumberQualityRating} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudPhoneNumberQualityRating(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudPhoneNumberQualityRating} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "GREEN"}, or {@code null}
     * @return the matching quality rating, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudPhoneNumberQualityRating of(String input) {
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
     * Returns the WhatsApp wire token for this quality rating.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this quality rating.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
