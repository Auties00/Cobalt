package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;
import java.util.Optional;

/**
 * Media-kind classifier for a WhatsApp native ad creative.
 *
 * <p>Already-uploaded WhatsApp media registered against a native ad on the
 * status surface is identified as either a still image or a video. This
 * enum carries that two-state discriminator alongside the wire token the
 * server expects: the discriminator is rendered as the literal
 * {@code "IMAGE"} or {@code "VIDEO"} when the registration is submitted.
 */
@ProtobufEnum
public enum AdMediaType {
    /**
     * The native-ad image kind, rendered on the wire as {@code "IMAGE"}.
     */
    IMAGE(0, "IMAGE"),

    /**
     * The native-ad video kind, rendered on the wire as {@code "VIDEO"}.
     */
    VIDEO(1, "VIDEO");

    /**
     * The protobuf wire-format index associated with this media kind.
     */
    final int index;

    /**
     * The wire-level string used to identify this media kind in the
     * native-ad media-registration payload.
     */
    final String wireValue;

    /**
     * Constructs a new {@code AdMediaType} bound to the supplied protobuf
     * index and wire literal.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    AdMediaType(@ProtobufEnumIndex int index, String wireValue) {
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
     * Returns the wire-level literal used to identify this kind in the
     * native-ad media-registration payload.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves an {@code AdMediaType} from its wire literal.
     *
     * <p>The lookup is lenient: a {@code null} or unrecognised token
     * resolves to {@link Optional#empty()} rather than raising.
     *
     * @param wireValue the wire literal to resolve, possibly {@code null}
     * @return the matching kind, or empty when the literal is {@code null}
     *         or unrecognised
     */
    public static Optional<AdMediaType> ofWireValue(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (Objects.equals(value.wireValue, wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
