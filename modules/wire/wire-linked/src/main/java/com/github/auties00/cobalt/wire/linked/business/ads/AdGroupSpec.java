package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Ad-group specification of a boosted Click-to-WhatsApp component.
 *
 * <p>Each ad group of a boosted component pairs a creative with its identifier. This model carries the
 * keys WhatsApp Web's ad-group builder populates: the {@link #creative() creative} (its object story
 * spec and creative-optimisation settings) and the ad group's {@link #id() identifier}.
 */
@ProtobufMessage(name = "AdGroupSpec")
public final class AdGroupSpec {
    /**
     * Creative of the ad group. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final AdGroupCreative creative;

    /**
     * Identifier of the ad group. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    /**
     * Constructs a new {@code AdGroupSpec}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param creative the ad-group creative, or {@code null}
     * @param id       the ad-group identifier, or {@code null}
     */
    AdGroupSpec(AdGroupCreative creative, String id) {
        this.creative = creative;
        this.id = id;
    }

    /**
     * Returns the creative of the ad group.
     *
     * @return an {@link Optional} carrying the creative, or empty when unset
     */
    public Optional<AdGroupCreative> creative() {
        return Optional.ofNullable(creative);
    }

    /**
     * Returns the identifier of the ad group.
     *
     * @return an {@link Optional} carrying the identifier, or empty when unset
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }
}
