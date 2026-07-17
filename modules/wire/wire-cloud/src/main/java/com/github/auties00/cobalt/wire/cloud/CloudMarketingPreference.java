package com.github.auties00.cobalt.wire.cloud;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * A recipient's marketing-message preference, carried by a {@code user_preferences} webhook change.
 *
 * <p>A recipient can {@link #STOP} marketing messages from a business or {@link #RESUME} them. The
 * {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 *
 * <p>The wire tokens are lowercase; {@link #STOP} serializes as {@code stop} and {@link #RESUME}
 * serializes as {@code resume} through {@link #token()}.
 */
@ProtobufEnum
public enum CloudMarketingPreference {
    /**
     * A preference that this client does not recognise. Resolved for any token outside the modelled set
     * so that an unexpected value never fails decoding.
     */
    UNKNOWN(0, "unknown"),

    /**
     * The recipient opted out of marketing messages.
     */
    STOP(1, "stop"),

    /**
     * The recipient opted back into marketing messages.
     */
    RESUME(2, "resume");

    /**
     * The protobuf-assigned numeric index for this preference.
     */
    final int index;

    /**
     * The lowercase WhatsApp wire token for this preference.
     */
    final String token;

    /**
     * Constructs a {@code CloudMarketingPreference} with the specified protobuf index and wire token.
     *
     * @param index the protobuf enum index
     * @param token the lowercase wire token
     */
    CloudMarketingPreference(@ProtobufEnumIndex int index, String token) {
        this.index = index;
        this.token = token;
    }

    /**
     * Returns the {@code CloudMarketingPreference} matching the given wire token.
     *
     * <p>The lookup matches both the constant name and the lowercase wire token case-insensitively
     * against {@code input}; any unrecognised or {@code null} token resolves to {@link #UNKNOWN} so
     * that decoding never fails on an unexpected value.
     *
     * @param input the wire token, for example {@code "stop"}, or {@code null}
     * @return the matching preference, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudMarketingPreference of(String input) {
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
     * Returns the lowercase WhatsApp wire token for this preference.
     *
     * @return the wire token, {@code "stop"} or {@code "resume"}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the protobuf-assigned numeric index for this preference.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
