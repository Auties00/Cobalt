package com.github.auties00.cobalt.wire.cloud.flow;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The lifecycle status of a WhatsApp Cloud API Flow.
 *
 * <p>A Flow begins as a {@link #DRAFT} that the business edits, becomes {@link #PUBLISHED} once it is
 * released for sending, and may later be {@link #DEPRECATED} to retire it. A published Flow whose
 * endpoint repeatedly fails may be {@link #THROTTLED} or {@link #BLOCKED} by the platform. The
 * {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudFlowStatus {
    /**
     * A status that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The Flow is being edited and has not been released for sending.
     */
    DRAFT(1),

    /**
     * The Flow has been released and may be sent.
     */
    PUBLISHED(2),

    /**
     * The Flow has been retired and should no longer be sent.
     */
    DEPRECATED(3),

    /**
     * The Flow has been blocked by the platform, typically for repeated endpoint failures.
     */
    BLOCKED(4),

    /**
     * The Flow has been throttled by the platform, typically for elevated endpoint errors.
     */
    THROTTLED(5);

    /**
     * The protobuf-assigned numeric index for this status.
     */
    final int index;

    /**
     * Constructs a {@code CloudFlowStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudFlowStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudFlowStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "PUBLISHED"}, or {@code null}
     * @return the matching status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudFlowStatus of(String input) {
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
