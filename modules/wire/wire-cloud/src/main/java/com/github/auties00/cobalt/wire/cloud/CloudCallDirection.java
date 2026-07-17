package com.github.auties00.cobalt.wire.cloud;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The direction of a WhatsApp Cloud API call.
 *
 * <p>A call is {@link #USER_INITIATED} when a consumer places it to the business, or
 * {@link #BUSINESS_INITIATED} when the business places it to a consumer. The {@link #UNKNOWN} constant
 * guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudCallDirection {
    /**
     * A direction that this client does not recognise. Resolved for any token outside the modelled set
     * so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The consumer placed the call to the business.
     */
    USER_INITIATED(1),

    /**
     * The business placed the call to the consumer.
     */
    BUSINESS_INITIATED(2);

    /**
     * The protobuf-assigned numeric index for this direction.
     */
    final int index;

    /**
     * Constructs a {@code CloudCallDirection} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudCallDirection(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudCallDirection} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "USER_INITIATED"}, or {@code null}
     * @return the matching direction, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudCallDirection of(String input) {
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
     * Returns the WhatsApp wire token for this direction.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this direction.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
