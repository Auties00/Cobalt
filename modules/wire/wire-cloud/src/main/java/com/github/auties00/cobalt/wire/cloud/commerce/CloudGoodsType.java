package com.github.auties00.cobalt.wire.cloud.commerce;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The kind of goods an {@code order_details} payment message covers.
 *
 * <p>An order is for either {@link #DIGITAL} goods (downloads, subscriptions, in-app items) or
 * {@link #PHYSICAL} goods (shippable items). The {@link #UNKNOWN} constant guards against tokens this
 * client does not yet model.
 *
 * <p>The wire tokens are hyphenated; {@link #DIGITAL} serializes as {@code digital-goods} and
 * {@link #PHYSICAL} serializes as {@code physical-goods} through {@link #token()}.
 */
@ProtobufEnum
public enum CloudGoodsType {
    /**
     * A goods kind that this client does not recognise. Resolved for any token outside the modelled set
     * so that an unexpected value never fails decoding.
     */
    UNKNOWN(0, "unknown"),

    /**
     * Digital goods such as downloads, subscriptions, or in-app items.
     */
    DIGITAL(1, "digital-goods"),

    /**
     * Physical goods that are shipped to the recipient.
     */
    PHYSICAL(2, "physical-goods");

    /**
     * The protobuf-assigned numeric index for this goods kind.
     */
    final int index;

    /**
     * The hyphenated WhatsApp wire token for this goods kind.
     */
    final String token;

    /**
     * Constructs a {@code CloudGoodsType} with the specified protobuf index and wire token.
     *
     * @param index the protobuf enum index
     * @param token the hyphenated wire token
     */
    CloudGoodsType(@ProtobufEnumIndex int index, String token) {
        this.index = index;
        this.token = token;
    }

    /**
     * Returns the {@code CloudGoodsType} matching the given wire token.
     *
     * <p>The lookup matches both the constant name and the hyphenated wire token case-insensitively
     * against {@code input}; any unrecognised or {@code null} token resolves to {@link #UNKNOWN} so
     * that decoding never fails on an unexpected value.
     *
     * @param input the wire token, for example {@code "physical-goods"}, or {@code null}
     * @return the matching goods kind, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudGoodsType of(String input) {
        if (input == null) {
            return UNKNOWN;
        }
        for (var value : values()) {
            if (value != UNKNOWN && (value.name().equalsIgnoreCase(input) || value.token.equalsIgnoreCase(input))) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the hyphenated WhatsApp wire token for this goods kind.
     *
     * @return the wire token, {@code "digital-goods"} or {@code "physical-goods"}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the protobuf-assigned numeric index for this goods kind.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
