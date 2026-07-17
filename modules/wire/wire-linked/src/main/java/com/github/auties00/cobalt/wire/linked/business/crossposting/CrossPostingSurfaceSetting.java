package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One per-surface cross-posting setting on a
 * {@link CrossPostingDestinationAccount}.
 *
 * <p>Each cross-posting surface (notably the {@code "STORY"} surface) carries
 * an in-app eligibility marker the WhatsApp client uses to decide whether the
 * cross-posting toggle for that surface is enabled. Both markers are exposed as
 * raw strings because their server value sets are not recoverable from the
 * WhatsApp client.
 */
@ProtobufMessage(name = "CrossPostingSurfaceSetting")
public final class CrossPostingSurfaceSetting {
    /**
     * Server-defined in-app cross-posting eligibility marker. {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String eligibility;

    /**
     * Server-defined cross-posting surface marker. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String surface;

    /**
     * Constructs a new {@code CrossPostingSurfaceSetting}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param eligibility the in-app eligibility marker, or {@code null}
     * @param surface     the cross-posting surface marker, or {@code null}
     */
    CrossPostingSurfaceSetting(String eligibility, String surface) {
        this.eligibility = eligibility;
        this.surface = surface;
    }

    /**
     * Returns the in-app cross-posting eligibility marker.
     *
     * @return the eligibility marker, or empty when the server omitted it
     */
    public Optional<String> eligibility() {
        return Optional.ofNullable(eligibility);
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
