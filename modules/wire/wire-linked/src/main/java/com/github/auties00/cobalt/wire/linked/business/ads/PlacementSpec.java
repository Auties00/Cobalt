package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * Placement configuration selecting where a Click-to-WhatsApp ad may be shown.
 *
 * <p>An ad's placement spec pins the publisher surfaces (for example Facebook feed, Instagram feed)
 * the ad is eligible to appear on. This model carries that single choice as the list of
 * {@link #publisherPlatforms() publisher platforms}.
 */
@ProtobufMessage(name = "PlacementSpec")
public final class PlacementSpec {
    /**
     * Publisher platforms the ad may appear on, in the order they are sent. Never {@code null},
     * possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final List<String> publisherPlatforms;

    /**
     * Constructs a new {@code PlacementSpec}. A {@code null} {@code publisherPlatforms} is coerced to
     * an empty list.
     *
     * @param publisherPlatforms the publisher platforms; {@code null} treated as empty
     */
    PlacementSpec(List<String> publisherPlatforms) {
        this.publisherPlatforms = publisherPlatforms == null ? List.of() : List.copyOf(publisherPlatforms);
    }

    /**
     * Returns the publisher platforms the ad may appear on.
     *
     * @return an unmodifiable view of the publisher platforms; never {@code null}, possibly empty
     */
    public List<String> publisherPlatforms() {
        return publisherPlatforms;
    }
}
