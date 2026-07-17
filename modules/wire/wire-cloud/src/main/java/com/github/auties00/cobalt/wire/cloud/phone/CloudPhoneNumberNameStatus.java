package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The display-name review status of a WhatsApp Business phone number.
 *
 * <p>A requested display name moves through review: it is {@link #PENDING_REVIEW} until Meta evaluates
 * it, after which it is {@link #APPROVED}, {@link #DECLINED}, or {@link #EXPIRED}. Some names are
 * {@link #AVAILABLE_WITHOUT_REVIEW}, and a number with no display name has the status {@link #NONE}.
 * The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudPhoneNumberNameStatus {
    /**
     * A status that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The display name passed review.
     */
    APPROVED(1),

    /**
     * The display name may be used without review.
     */
    AVAILABLE_WITHOUT_REVIEW(2),

    /**
     * The display name was declined in review.
     */
    DECLINED(3),

    /**
     * The display name's approval has expired.
     */
    EXPIRED(4),

    /**
     * The display name is awaiting review.
     */
    PENDING_REVIEW(5),

    /**
     * No display name is set.
     */
    NONE(6);

    /**
     * The protobuf-assigned numeric index for this status.
     */
    final int index;

    /**
     * Constructs a {@code CloudPhoneNumberNameStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudPhoneNumberNameStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudPhoneNumberNameStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "APPROVED"}, or {@code null}
     * @return the matching status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudPhoneNumberNameStatus of(String input) {
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
