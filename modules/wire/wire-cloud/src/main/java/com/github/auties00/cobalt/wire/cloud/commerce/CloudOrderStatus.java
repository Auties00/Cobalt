package com.github.auties00.cobalt.wire.cloud.commerce;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The status of a WhatsApp Cloud API order.
 *
 * <p>A new order embedded in an {@code order_details} message is conventionally {@link #PENDING}.
 * Subsequent {@code order_status} messages advance it through {@link #PROCESSING},
 * {@link #PARTIALLY_SHIPPED}, {@link #SHIPPED}, and {@link #COMPLETED}, or mark it {@link #CANCELED}.
 * The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 *
 * <p>The wire tokens are lowercase; each constant maps to its lowercase token through {@link #token()},
 * for example {@link #PARTIALLY_SHIPPED} serializes as {@code partially_shipped}.
 */
@ProtobufEnum
public enum CloudOrderStatus {
    /**
     * A status that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0, "unknown"),

    /**
     * The order has been placed but not yet acted on.
     */
    PENDING(1, "pending"),

    /**
     * The order is being prepared for shipment.
     */
    PROCESSING(2, "processing"),

    /**
     * Some, but not all, of the order has shipped.
     */
    PARTIALLY_SHIPPED(3, "partially_shipped"),

    /**
     * The order has shipped in full.
     */
    SHIPPED(4, "shipped"),

    /**
     * The order has been fulfilled.
     */
    COMPLETED(5, "completed"),

    /**
     * The order has been canceled.
     */
    CANCELED(6, "canceled");

    /**
     * The protobuf-assigned numeric index for this status.
     */
    final int index;

    /**
     * The lowercase WhatsApp wire token for this status.
     */
    final String token;

    /**
     * Constructs a {@code CloudOrderStatus} with the specified protobuf index and wire token.
     *
     * @param index the protobuf enum index
     * @param token the lowercase wire token
     */
    CloudOrderStatus(@ProtobufEnumIndex int index, String token) {
        this.index = index;
        this.token = token;
    }

    /**
     * Returns the {@code CloudOrderStatus} matching the given wire token.
     *
     * <p>The lookup matches both the constant name and the lowercase wire token case-insensitively
     * against {@code input}; any unrecognised or {@code null} token resolves to {@link #UNKNOWN} so
     * that decoding never fails on an unexpected value.
     *
     * @param input the wire token, for example {@code "shipped"}, or {@code null}
     * @return the matching status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudOrderStatus of(String input) {
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
     * Returns the lowercase WhatsApp wire token for this status.
     *
     * @return the wire token, for example {@code "partially_shipped"}
     */
    public String token() {
        return token;
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
