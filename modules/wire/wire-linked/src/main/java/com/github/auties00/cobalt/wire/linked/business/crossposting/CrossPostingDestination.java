package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Targeted destination of a cross-posting operation: the destination
 * application paired with its surface marker.
 *
 * <p>The {@linkplain #application() application} names which Meta app the
 * cross-post is targeted at (Facebook or Instagram); the surface marker
 * selects the surface within that application (notably {@code "STORY"}).
 * The surface marker is exposed as a raw string because its full server
 * value set is not recoverable from the WhatsApp client.
 */
@ProtobufMessage(name = "CrossPostingDestination")
public final class CrossPostingDestination {
    /**
     * Destination application. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final CrossPostingApplication application;

    /**
     * Cross-posting surface marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String surface;

    /**
     * Constructs a new {@code CrossPostingDestination}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param application the destination application, or {@code null}
     * @param surface     the cross-posting surface marker, or {@code null}
     */
    CrossPostingDestination(CrossPostingApplication application, String surface) {
        this.application = application;
        this.surface = surface;
    }

    /**
     * Returns the destination application.
     *
     * @return the destination application, or empty when the server omitted
     *         it
     */
    public Optional<CrossPostingApplication> application() {
        return Optional.ofNullable(application);
    }

    /**
     * Returns the cross-posting surface marker.
     *
     * @return the surface marker, or empty when the server omitted it
     */
    public Optional<String> surface() {
        return Optional.ofNullable(surface);
    }
}
