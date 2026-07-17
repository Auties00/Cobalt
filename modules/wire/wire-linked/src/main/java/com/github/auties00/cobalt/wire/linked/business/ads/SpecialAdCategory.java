package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;
import java.util.Optional;

/**
 * Special ad category a regulated Click-to-WhatsApp ad falls under.
 *
 * <p>Ads about housing, employment, credit, or social issues, elections and politics are subject to
 * additional targeting restrictions. This enum carries that classifier alongside the wire token the
 * server expects: the discriminator is rendered as the literal category name (for example
 * {@code "HOUSING"}) when the regulated-category tuning is submitted.
 */
@ProtobufEnum
public enum SpecialAdCategory {
    /**
     * The housing special ad category, rendered on the wire as {@code "HOUSING"}.
     */
    HOUSING(0, "HOUSING"),

    /**
     * The employment special ad category, rendered on the wire as {@code "EMPLOYMENT"}.
     */
    EMPLOYMENT(1, "EMPLOYMENT"),

    /**
     * The credit special ad category, rendered on the wire as {@code "CREDIT"}.
     */
    CREDIT(2, "CREDIT"),

    /**
     * The social issues, elections and politics special ad category, rendered on the wire as
     * {@code "ISSUES_ELECTIONS_POLITICS"}.
     */
    ISSUES_ELECTIONS_POLITICS(3, "ISSUES_ELECTIONS_POLITICS");

    /**
     * The protobuf wire-format index associated with this category.
     */
    final int index;

    /**
     * The wire-level string used to identify this category in the regulated-category payload.
     */
    final String wireValue;

    /**
     * Constructs a new {@code SpecialAdCategory} bound to the supplied protobuf index and wire literal.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    SpecialAdCategory(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this category.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-level literal used to identify this category.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code SpecialAdCategory} from its wire literal.
     *
     * <p>The lookup is lenient: a {@code null} or unrecognised token resolves to
     * {@link Optional#empty()} rather than raising.
     *
     * @param wireValue the wire literal to resolve, possibly {@code null}
     * @return the matching category, or empty when the literal is {@code null} or unrecognised
     */
    public static Optional<SpecialAdCategory> ofWireValue(String wireValue) {
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
