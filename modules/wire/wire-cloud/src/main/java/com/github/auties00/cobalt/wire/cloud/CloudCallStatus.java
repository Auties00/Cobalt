package com.github.auties00.cobalt.wire.cloud;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The lifecycle status of a business-initiated WhatsApp Cloud API call.
 *
 * <p>A call the business places reports its progress as {@link #RINGING} while it rings the consumer,
 * {@link #ACCEPTED} when the consumer answers, or {@link #REJECTED} when the consumer declines. The
 * {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudCallStatus {
    /**
     * A status that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The call is ringing the consumer.
     */
    RINGING(1),

    /**
     * The consumer answered the call.
     */
    ACCEPTED(2),

    /**
     * The consumer declined the call.
     */
    REJECTED(3);

    /**
     * The protobuf-assigned numeric index for this status.
     */
    final int index;

    /**
     * Constructs a {@code CloudCallStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudCallStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudCallStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "RINGING"}, or {@code null}
     * @return the matching status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudCallStatus of(String input) {
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
