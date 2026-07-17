package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The messaging limit tier of a WhatsApp Business phone number.
 *
 * <p>The tier caps how many unique customers a number may start business-initiated conversations with
 * in a rolling 24-hour window, scaling from {@link #TIER_50} up through {@link #TIER_250},
 * {@link #TIER_1K}, {@link #TIER_10K}, and {@link #TIER_100K} to {@link #TIER_UNLIMITED}. The
 * {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudMessagingLimitTier {
    /**
     * A tier that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * Up to 50 unique customers per rolling 24-hour window.
     */
    TIER_50(1),

    /**
     * Up to 250 unique customers per rolling 24-hour window.
     */
    TIER_250(2),

    /**
     * Up to 1,000 unique customers per rolling 24-hour window.
     */
    TIER_1K(3),

    /**
     * Up to 10,000 unique customers per rolling 24-hour window.
     */
    TIER_10K(4),

    /**
     * Up to 100,000 unique customers per rolling 24-hour window.
     */
    TIER_100K(5),

    /**
     * No cap on unique customers per rolling 24-hour window.
     */
    TIER_UNLIMITED(6);

    /**
     * The protobuf-assigned numeric index for this tier.
     */
    final int index;

    /**
     * Constructs a {@code CloudMessagingLimitTier} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudMessagingLimitTier(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudMessagingLimitTier} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "TIER_1K"}, or {@code null}
     * @return the matching tier, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudMessagingLimitTier of(String input) {
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
     * Returns the WhatsApp wire token for this tier.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this tier.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
