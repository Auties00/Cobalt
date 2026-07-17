package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The hosting platform type of a WhatsApp Business phone number.
 *
 * <p>A number is hosted on the {@link #CLOUD_API} or runs on the legacy {@link #ON_PREMISE} client;
 * {@link #NOT_APPLICABLE} indicates the platform type is not reported. The {@link #UNKNOWN} constant
 * guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudPhoneNumberPlatformType {
    /**
     * A platform type that this client does not recognise. Resolved for any token outside the modelled
     * set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The number is hosted on the WhatsApp Cloud API.
     */
    CLOUD_API(1),

    /**
     * The number runs on the legacy on-premise client.
     */
    ON_PREMISE(2),

    /**
     * The platform type is not reported.
     */
    NOT_APPLICABLE(3);

    /**
     * The protobuf-assigned numeric index for this platform type.
     */
    final int index;

    /**
     * Constructs a {@code CloudPhoneNumberPlatformType} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudPhoneNumberPlatformType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudPhoneNumberPlatformType} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "CLOUD_API"}, or {@code null}
     * @return the matching platform type, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudPhoneNumberPlatformType of(String input) {
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
     * Returns the WhatsApp wire token for this platform type.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this platform type.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
