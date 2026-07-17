package com.github.auties00.cobalt.wire.cloud.waba;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The account review status of a WhatsApp Business Account.
 *
 * <p>An account is {@link #PENDING} review until Meta evaluates it, after which it becomes
 * {@link #APPROVED} or {@link #REJECTED}. A previously approved account whose review lapses is
 * {@link #DEPRECATED}. The {@link #UNKNOWN} constant guards against tokens this client does not yet
 * model.
 */
@ProtobufEnum
public enum CloudWabaReviewStatus {
    /**
     * A review status that this client does not recognise. Resolved for any token outside the modelled
     * set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The account is awaiting review.
     */
    PENDING(1),

    /**
     * The account passed review.
     */
    APPROVED(2),

    /**
     * The account failed review.
     */
    REJECTED(3),

    /**
     * The account's prior approval has lapsed.
     */
    DEPRECATED(4);

    /**
     * The protobuf-assigned numeric index for this review status.
     */
    final int index;

    /**
     * Constructs a {@code CloudWabaReviewStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudWabaReviewStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudWabaReviewStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "APPROVED"}, or {@code null}
     * @return the matching review status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudWabaReviewStatus of(String input) {
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
     * Returns the WhatsApp wire token for this review status.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this review status.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
