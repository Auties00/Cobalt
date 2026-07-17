package com.github.auties00.cobalt.wire.cloud.waba;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The ownership relationship of a WhatsApp Business Account to the querying business portfolio.
 *
 * <p>An account is owned {@link #DIRECT}ly by the portfolio, owned by the portfolio itself
 * ({@link #SELF}), or managed on behalf of a client by a {@link #RESELLER}. The {@link #UNKNOWN}
 * constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudWabaOwnershipType {
    /**
     * An ownership type that this client does not recognise. Resolved for any token outside the modelled
     * set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The account is owned directly by the querying portfolio.
     */
    DIRECT(1),

    /**
     * The account is owned by the portfolio itself.
     */
    SELF(2),

    /**
     * The account is managed on behalf of a client by a reseller.
     */
    RESELLER(3);

    /**
     * The protobuf-assigned numeric index for this ownership type.
     */
    final int index;

    /**
     * Constructs a {@code CloudWabaOwnershipType} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudWabaOwnershipType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudWabaOwnershipType} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "DIRECT"}, or {@code null}
     * @return the matching ownership type, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudWabaOwnershipType of(String input) {
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
     * Returns the WhatsApp wire token for this ownership type.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this ownership type.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
