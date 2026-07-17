package com.github.auties00.cobalt.wire.cloud.template;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The review status of a WhatsApp Cloud API message template.
 *
 * <p>Every message template moves through a review lifecycle: a freshly authored template is
 * {@link #PENDING} until Meta reviews it, after which it becomes {@link #APPROVED} or
 * {@link #REJECTED}. An approved template may later be {@link #PAUSED} or have its sending
 * {@link #LIMIT_EXCEEDED} or be {@link #DISABLED} for repeated quality issues, may be scheduled for
 * {@link #PENDING_DELETION}, or may sit {@link #IN_APPEAL} after a rejection is contested. The
 * {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudTemplateStatus {
    /**
     * A status that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The template passed review and may be used for sending.
     */
    APPROVED(1),

    /**
     * The template's rejection is being contested through an appeal.
     */
    IN_APPEAL(2),

    /**
     * The template is awaiting review.
     */
    PENDING(3),

    /**
     * The template failed review and may not be used for sending.
     */
    REJECTED(4),

    /**
     * The template is scheduled for deletion.
     */
    PENDING_DELETION(5),

    /**
     * The template was disabled, typically for repeated quality issues.
     */
    DISABLED(6),

    /**
     * The template is temporarily paused, typically after a quality drop.
     */
    PAUSED(7),

    /**
     * The template exceeded its sending limit and is temporarily blocked.
     */
    LIMIT_EXCEEDED(8);

    /**
     * The protobuf-assigned numeric index for this status.
     */
    final int index;

    /**
     * Constructs a {@code CloudTemplateStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudTemplateStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudTemplateStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "APPROVED"}, or {@code null}
     * @return the matching status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudTemplateStatus of(String input) {
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
