package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The platform registration status of a WhatsApp Business phone number.
 *
 * <p>This reports where the number sits in its connection lifecycle on the Cloud API platform: a
 * number is {@link #PENDING} while registration completes, {@link #CONNECTED} once it can send and
 * receive, {@link #DISCONNECTED} when its connection is severed, {@link #FLAGGED} when its quality is
 * under scrutiny, {@link #RESTRICTED} or {@link #RATE_LIMITED} when its sending is curtailed,
 * {@link #BANNED} when it is barred, {@link #MIGRATED} when it moved hosting, or {@link #DELETED} when
 * it is removed. The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudPhoneNumberStatus {
    /**
     * A status that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * Registration is in progress.
     */
    PENDING(1),

    /**
     * The number is connected and may send and receive.
     */
    CONNECTED(2),

    /**
     * The number's connection has been severed.
     */
    DISCONNECTED(3),

    /**
     * The number's quality is under scrutiny.
     */
    FLAGGED(4),

    /**
     * The number's sending is restricted.
     */
    RESTRICTED(5),

    /**
     * The number's sending is rate limited.
     */
    RATE_LIMITED(6),

    /**
     * The number is barred from the platform.
     */
    BANNED(7),

    /**
     * The number was migrated to a different hosting.
     */
    MIGRATED(8),

    /**
     * The number has been removed.
     */
    DELETED(9);

    /**
     * The protobuf-assigned numeric index for this status.
     */
    final int index;

    /**
     * Constructs a {@code CloudPhoneNumberStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudPhoneNumberStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudPhoneNumberStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "CONNECTED"}, or {@code null}
     * @return the matching status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudPhoneNumberStatus of(String input) {
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
