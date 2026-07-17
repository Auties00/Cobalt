package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The message-throughput level of a WhatsApp Business phone number.
 *
 * <p>The level reports the sustained send rate the platform provisions for a number: {@link #STANDARD}
 * for the default rate and {@link #HIGH} for the upgraded rate; {@link #NOT_APPLICABLE} indicates the
 * number is not eligible for a throughput level. The {@link #UNKNOWN} constant guards against tokens
 * this client does not yet model.
 */
@ProtobufEnum
public enum CloudThroughputLevel {
    /**
     * A level that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The default send rate.
     */
    STANDARD(1),

    /**
     * The upgraded send rate.
     */
    HIGH(2),

    /**
     * The number is not eligible for a throughput level.
     */
    NOT_APPLICABLE(3);

    /**
     * The protobuf-assigned numeric index for this level.
     */
    final int index;

    /**
     * Constructs a {@code CloudThroughputLevel} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudThroughputLevel(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudThroughputLevel} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "STANDARD"}, or {@code null}
     * @return the matching level, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudThroughputLevel of(String input) {
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
     * Returns the WhatsApp wire token for this level.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this level.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
