package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Creative of a Click-to-WhatsApp ad group.
 *
 * <p>Each ad group carries one creative describing what the ad renders. This model holds that
 * creative: its {@link #objectStorySpec() object story spec} and, when automatic creative optimisation
 * is enabled, its {@link #degreesOfFreedomSpec() degrees-of-freedom spec}.
 */
@ProtobufMessage(name = "AdGroupCreative")
public final class AdGroupCreative {
    /**
     * Object story spec describing the creative content. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ObjectStorySpec objectStorySpec;

    /**
     * Advantage-plus creative-optimisation settings of the creative. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final CreativeDegreesOfFreedomSpec degreesOfFreedomSpec;

    /**
     * Constructs a new {@code AdGroupCreative}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param objectStorySpec      the object story spec, or {@code null}
     * @param degreesOfFreedomSpec the creative-optimisation settings, or {@code null}
     */
    AdGroupCreative(ObjectStorySpec objectStorySpec, CreativeDegreesOfFreedomSpec degreesOfFreedomSpec) {
        this.objectStorySpec = objectStorySpec;
        this.degreesOfFreedomSpec = degreesOfFreedomSpec;
    }

    /**
     * Returns the object story spec describing the creative content.
     *
     * @return an {@link Optional} carrying the object story spec, or empty when unset
     */
    public Optional<ObjectStorySpec> objectStorySpec() {
        return Optional.ofNullable(objectStorySpec);
    }

    /**
     * Returns the advantage-plus creative-optimisation settings of the creative.
     *
     * @return an {@link Optional} carrying the creative-optimisation settings, or empty when unset
     */
    public Optional<CreativeDegreesOfFreedomSpec> degreesOfFreedomSpec() {
        return Optional.ofNullable(degreesOfFreedomSpec);
    }
}
