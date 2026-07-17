package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Creative-features spec of a Click-to-WhatsApp ad group's degrees-of-freedom settings.
 *
 * <p>When automatic creative optimisation enrols an ad group, its degrees-of-freedom spec carries a
 * creative-features spec describing the automated creative enhancements the platform may apply. This
 * model holds that spec: its {@link #productExtensions() product extensions} attachment.
 */
@ProtobufMessage(name = "CreativeFeaturesSpec")
public final class CreativeFeaturesSpec {
    /**
     * Product-extensions attachment of the creative-features spec. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CreativeFeatureAttachment productExtensions;

    /**
     * Constructs a new {@code CreativeFeaturesSpec}.
     *
     * @param productExtensions the product-extensions attachment, or {@code null} to leave it unset
     */
    CreativeFeaturesSpec(CreativeFeatureAttachment productExtensions) {
        this.productExtensions = productExtensions;
    }

    /**
     * Returns the product-extensions attachment of the creative-features spec.
     *
     * @return an {@link Optional} carrying the product-extensions attachment, or empty when unset
     */
    public Optional<CreativeFeatureAttachment> productExtensions() {
        return Optional.ofNullable(productExtensions);
    }
}
