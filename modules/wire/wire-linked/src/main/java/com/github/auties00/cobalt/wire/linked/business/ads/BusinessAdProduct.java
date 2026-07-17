package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;
import java.util.Optional;

/**
 * Product kind a Click-to-WhatsApp ad-creation run boosts.
 *
 * <p>The ad-creation opening screen tags the run with the kind of object being promoted; the
 * discriminator is rendered as the literal product name (for example {@code "BOOSTED_MESSAGE"}) in the
 * creation-context input. {@link #BOOSTED_MESSAGE} is the only kind the Web client emits for the
 * ad-creation flow; the server-side enum may define further kinds the client does not exercise, which
 * can be appended here as they are observed.
 */
@ProtobufEnum
public enum BusinessAdProduct {
    /**
     * The boosted Click-to-WhatsApp message product, rendered on the wire as {@code "BOOSTED_MESSAGE"}.
     */
    BOOSTED_MESSAGE(0, "BOOSTED_MESSAGE");

    /**
     * The protobuf wire-format index associated with this product.
     */
    final int index;

    /**
     * The wire-level string used to identify this product in the creation-context input.
     */
    final String wireValue;

    /**
     * Constructs a new {@code BusinessAdProduct} bound to the supplied protobuf index and wire literal.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-level literal; never {@code null}
     */
    BusinessAdProduct(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this product.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-level literal used to identify this product.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code BusinessAdProduct} from its wire literal.
     *
     * <p>The lookup is lenient: a {@code null} or unrecognised token resolves to
     * {@link Optional#empty()} rather than raising.
     *
     * @param wireValue the wire literal to resolve, possibly {@code null}
     * @return the matching product, or empty when the literal is {@code null} or unrecognised
     */
    public static Optional<BusinessAdProduct> ofWireValue(String wireValue) {
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
