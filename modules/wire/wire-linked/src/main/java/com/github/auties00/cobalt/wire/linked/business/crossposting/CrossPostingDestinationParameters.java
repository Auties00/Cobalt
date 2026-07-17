package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Per-destination cross-posting eligibility parameters.
 *
 * <p>For each evaluated destination ({@linkplain CrossPostingDestination
 * destination}) the server returns the hashed-codec parameter blob the
 * WhatsApp client folds into its per-status cross-posting state map. The
 * parameter blob is delivered as a single GraphQL scalar and is exposed as a
 * raw string.
 */
@ProtobufMessage(name = "CrossPostingDestinationParameters")
public final class CrossPostingDestinationParameters {
    /**
     * Targeted destination, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CrossPostingDestination destination;

    /**
     * Hashed-codec parameter blob folded into the per-status state map.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String hashedCodecParameters;

    /**
     * Constructs a new {@code CrossPostingDestinationParameters}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param destination           the targeted destination, or {@code null}
     * @param hashedCodecParameters the hashed-codec parameter blob, or
     *                              {@code null}
     */
    CrossPostingDestinationParameters(CrossPostingDestination destination, String hashedCodecParameters) {
        this.destination = destination;
        this.hashedCodecParameters = hashedCodecParameters;
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
     * Returns the hashed-codec parameter blob.
     *
     * @return the parameter blob, or empty when the server omitted it
     */
    public Optional<String> hashedCodecParameters() {
        return Optional.ofNullable(hashedCodecParameters);
    }
}
