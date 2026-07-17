package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;
import java.util.Optional;

/**
 * Destination application of a WhatsApp cross-posting request.
 *
 * <p>WhatsApp lets a user re-share their WhatsApp status onto another Meta
 * social app the account is linked to: a status may be cross-posted to a
 * linked Facebook profile, a linked Instagram profile, or both at the same
 * time. This enum carries the two-state discriminator naming the destination
 * application alongside the wire token the WhatsApp client expects: the
 * discriminator is rendered as the literal {@code "F"} for Facebook and
 * {@code "I"} for Instagram in the cross-posting payload.
 */
@ProtobufEnum(name = "CrossPostingApplication")
public enum CrossPostingApplication {
    /**
     * The Facebook cross-posting destination, rendered on the wire as
     * {@code "F"}.
     */
    FACEBOOK(0, "F"),

    /**
     * The Instagram cross-posting destination, rendered on the wire as
     * {@code "I"}.
     */
    INSTAGRAM(1, "I");

    /**
     * The protobuf wire-format index associated with this application.
     */
    final int index;

    /**
     * The wire-level string used to identify this application in the
     * cross-posting payload.
     */
    final String wireValue;

    /**
     * Constructs a new {@code CrossPostingApplication} bound to the supplied
     * protobuf index and wire literal.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    CrossPostingApplication(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this
     * application.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-level literal used to identify this application in the
     * cross-posting payload.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code CrossPostingApplication} from its wire literal.
     *
     * <p>The lookup is lenient: a {@code null} or unrecognised token
     * resolves to {@link Optional#empty()} rather than raising.
     *
     * @param wireValue the wire literal to resolve, possibly {@code null}
     * @return the matching application, or empty when the literal is
     *         {@code null} or unrecognised
     */
    public static Optional<CrossPostingApplication> ofWireValue(String wireValue) {
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
