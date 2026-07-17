package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;
import java.util.Optional;

/**
 * Coarse price band a WhatsApp Business profile advertises for its goods and services.
 *
 * <p>A merchant can tag their Business profile with a single price band so customers gauge the price
 * level at a glance. This enum carries that classifier alongside the wire token the server expects: the
 * band is rendered as the literal band name (for example {@code "BUDGET"}) when the profile edit is
 * submitted.
 */
@ProtobufEnum
public enum BusinessProfilePriceTier {
    /**
     * The budget price band, rendered on the wire as {@code "BUDGET"}.
     */
    BUDGET(0, "BUDGET"),

    /**
     * The everyday price band, rendered on the wire as {@code "EVERYDAY"}.
     */
    EVERYDAY(1, "EVERYDAY"),

    /**
     * The premium price band, rendered on the wire as {@code "PREMIUM"}.
     */
    PREMIUM(2, "PREMIUM"),

    /**
     * The luxury price band, rendered on the wire as {@code "LUXURY"}.
     */
    LUXURY(3, "LUXURY");

    /**
     * The protobuf wire-format index associated with this band.
     */
    final int index;

    /**
     * The wire-level string used to identify this band in the profile-edit payload.
     */
    final String wireValue;

    /**
     * Constructs a new {@code BusinessProfilePriceTier} bound to the supplied protobuf index and wire
     * literal.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    BusinessProfilePriceTier(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this band.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-level literal used to identify this band.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code BusinessProfilePriceTier} from its wire literal.
     *
     * <p>The lookup is lenient: a {@code null} or unrecognised token resolves to
     * {@link Optional#empty()} rather than raising.
     *
     * @param wireValue the wire literal to resolve, possibly {@code null}
     * @return the matching band, or empty when the literal is {@code null} or unrecognised
     */
    public static Optional<BusinessProfilePriceTier> ofWireValue(String wireValue) {
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
