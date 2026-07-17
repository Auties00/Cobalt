package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The local-storage (data localization) status of a WhatsApp Business phone number.
 *
 * <p>A number stores message data per the {@link #DEFAULT} policy, or pins it to a country with
 * {@link #IN_COUNTRY_STORAGE_ENABLED}, or opts out of platform storage with {@link #NO_STORAGE_ENABLED}.
 * The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudLocalStorageStatus {
    /**
     * A status that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The default storage policy applies.
     */
    DEFAULT(1),

    /**
     * Message data is pinned to a specific country.
     */
    IN_COUNTRY_STORAGE_ENABLED(2),

    /**
     * Platform storage of message data is disabled.
     */
    NO_STORAGE_ENABLED(3);

    /**
     * The protobuf-assigned numeric index for this status.
     */
    final int index;

    /**
     * Constructs a {@code CloudLocalStorageStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudLocalStorageStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudLocalStorageStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "IN_COUNTRY_STORAGE_ENABLED"}, or {@code null}
     * @return the matching status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudLocalStorageStatus of(String input) {
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
