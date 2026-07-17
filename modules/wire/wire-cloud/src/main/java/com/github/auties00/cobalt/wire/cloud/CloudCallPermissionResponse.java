package com.github.auties00.cobalt.wire.cloud;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * A consumer's response to a WhatsApp Cloud API call-permission request.
 *
 * <p>A consumer can {@link #ACCEPT} or {@link #REJECT} a request to receive business-initiated calls.
 * The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 *
 * <p>The wire tokens are lowercase; {@link #ACCEPT} serializes as {@code accept} and {@link #REJECT}
 * serializes as {@code reject} through {@link #token()}.
 */
@ProtobufEnum
public enum CloudCallPermissionResponse {
    /**
     * A response that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0, "unknown"),

    /**
     * The consumer accepted the permission request.
     */
    ACCEPT(1, "accept"),

    /**
     * The consumer rejected the permission request.
     */
    REJECT(2, "reject");

    /**
     * The protobuf-assigned numeric index for this response.
     */
    final int index;

    /**
     * The lowercase WhatsApp wire token for this response.
     */
    final String token;

    /**
     * Constructs a {@code CloudCallPermissionResponse} with the specified protobuf index and wire token.
     *
     * @param index the protobuf enum index
     * @param token the lowercase wire token
     */
    CloudCallPermissionResponse(@ProtobufEnumIndex int index, String token) {
        this.index = index;
        this.token = token;
    }

    /**
     * Returns the {@code CloudCallPermissionResponse} matching the given wire token.
     *
     * <p>The lookup matches both the constant name and the lowercase wire token case-insensitively
     * against {@code input}; any unrecognised or {@code null} token resolves to {@link #UNKNOWN} so
     * that decoding never fails on an unexpected value.
     *
     * @param input the wire token, for example {@code "accept"}, or {@code null}
     * @return the matching response, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudCallPermissionResponse of(String input) {
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
     * Returns the lowercase WhatsApp wire token for this response.
     *
     * @return the wire token, {@code "accept"} or {@code "reject"}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the protobuf-assigned numeric index for this response.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
