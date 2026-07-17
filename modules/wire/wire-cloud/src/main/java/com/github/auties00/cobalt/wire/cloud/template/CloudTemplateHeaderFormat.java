package com.github.auties00.cobalt.wire.cloud.template;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The format a WhatsApp Cloud API message-template header renders in.
 *
 * <p>A template may carry at most one header component, and the header's format selects what it shows:
 * a {@link #TEXT} header shows a short headline that may contain a single placeholder, while the
 * {@link #IMAGE}, {@link #VIDEO}, and {@link #DOCUMENT} headers show a media asset bound at send time and
 * the {@link #LOCATION} header shows a map pin bound at send time. The {@link #UNKNOWN} constant guards
 * against tokens this client does not yet model so that an unexpected value never fails decoding.
 */
@ProtobufEnum
public enum CloudTemplateHeaderFormat {
    /**
     * A format that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * A text header showing a short headline that may carry a single placeholder.
     */
    TEXT(1),

    /**
     * A media header showing an image bound at send time.
     */
    IMAGE(2),

    /**
     * A media header showing a video bound at send time.
     */
    VIDEO(3),

    /**
     * A media header showing a document bound at send time.
     */
    DOCUMENT(4),

    /**
     * A header showing a map pin bound at send time.
     */
    LOCATION(5);

    /**
     * The protobuf-assigned numeric index for this format.
     */
    final int index;

    /**
     * Constructs a {@code CloudTemplateHeaderFormat} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudTemplateHeaderFormat(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudTemplateHeaderFormat} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any unrecognised
     * or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on an unexpected
     * value.
     *
     * @param input the wire token, for example {@code "IMAGE"}, or {@code null}
     * @return the matching format, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudTemplateHeaderFormat of(String input) {
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
     * Returns the protobuf-assigned numeric index for this format.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
