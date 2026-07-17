package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Per-status resolved cross-posting destination identity.
 *
 * <p>For each requested status the server resolves the targeted destination
 * ({@linkplain CrossPostingDestination destination}) into the cross-posting
 * destination identifier the WhatsApp client addresses the cross-post under.
 */
@ProtobufMessage(name = "CrossPostingDestinationResolution")
public final class CrossPostingDestinationResolution {
    /**
     * Targeted destination, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CrossPostingDestination destination;

    /**
     * Resolved cross-posting destination identifier. {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String destinationId;

    /**
     * Constructs a new {@code CrossPostingDestinationResolution}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param destination   the targeted destination, or {@code null}
     * @param destinationId the resolved destination identifier, or {@code null}
     */
    CrossPostingDestinationResolution(CrossPostingDestination destination, String destinationId) {
        this.destination = destination;
        this.destinationId = destinationId;
    }

    /**
     * Returns the targeted destination.
     *
     * @return the targeted destination, or empty when the server omitted it
     */
    public Optional<CrossPostingDestination> destination() {
        return Optional.ofNullable(destination);
    }

    /**
     * Returns the resolved cross-posting destination identifier.
     *
     * @return the destination identifier, or empty when the server omitted it
     */
    public Optional<String> destinationId() {
        return Optional.ofNullable(destinationId);
    }
}
