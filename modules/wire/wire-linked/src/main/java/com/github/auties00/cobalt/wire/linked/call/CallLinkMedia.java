package com.github.auties00.cobalt.wire.linked.call;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Optional;

/**
 * Media kind associated with a WhatsApp call link.
 *
 * <p>A call link is a sharable URL that, when followed, drops the user
 * directly into a multi-party voice or video call session. The media kind
 * is fixed at creation time and dictates whether participants join with
 * camera tracks enabled by default. The value is communicated to the relay
 * through the {@code media} attribute of the call-link payloads.
 */
@ProtobufEnum
public enum CallLinkMedia {
    /**
     * The call link is configured for voice-only calls; camera tracks are
     * disabled by default for joining participants. Wire value
     * {@code "audio"}.
     */
    AUDIO(0, "audio"),

    /**
     * The call link is configured for video calls; camera tracks are
     * enabled by default for joining participants. Wire value
     * {@code "video"}.
     */
    VIDEO(1, "video");

    /**
     * The protobuf wire-format index associated with this media kind.
     */
    final int index;

    /**
     * The literal wire-level string carried by the {@code media}
     * attribute.
     */
    final String wireValue;

    /**
     * Constructs a new {@code CallLinkMedia} with the supplied protobuf
     * index and wire string.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    CallLinkMedia(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this kind.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-level string carried by the {@code media}
     * attribute.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code CallLinkMedia} from the wire literal.
     *
     * @param wireValue the wire literal, possibly {@code null}
     * @return an {@link Optional} containing the matching constant, or
     *         empty when the literal is {@code null} or unrecognised
     */
    public static Optional<CallLinkMedia> ofWire(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
