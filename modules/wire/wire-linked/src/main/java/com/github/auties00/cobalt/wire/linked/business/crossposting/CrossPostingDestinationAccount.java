package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One linked Meta destination account that may receive cross-posts.
 *
 * <p>A destination account is identified by its server-issued destination id and
 * by a single-letter destination kind marker (the WhatsApp client recognises
 * {@code "F"} for Facebook and {@code "I"} for Instagram). Each account carries
 * the per-surface settings ({@linkplain CrossPostingSurfaceSetting settings})
 * the server reports for it.
 *
 * <p>The destination-kind marker is exposed as a raw string because the full
 * server value set is not recoverable from the WhatsApp client.
 */
@ProtobufMessage(name = "CrossPostingDestinationAccount")
public final class CrossPostingDestinationAccount {
    /**
     * Server-issued destination identifier. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String destinationId;

    /**
     * Single-letter destination-kind marker. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String destinationKind;

    /**
     * Per-surface settings, in the order the server returned them. Never
     * {@code null}, possibly empty when the server returned none.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<CrossPostingSurfaceSetting> surfaceSettings;

    /**
     * Constructs a new {@code CrossPostingDestinationAccount}. A {@code null}
     * {@code surfaceSettings} is coerced to an empty list; the reference
     * scalar arguments may be {@code null} when the server omitted them.
     *
     * @param destinationId   the destination identifier, or {@code null}
     * @param destinationKind the destination-kind marker, or {@code null}
     * @param surfaceSettings the per-surface settings; {@code null} treated as
     *                        empty
     */
    CrossPostingDestinationAccount(String destinationId,
                                   String destinationKind,
                                   List<CrossPostingSurfaceSetting> surfaceSettings) {
        this.destinationId = destinationId;
        this.destinationKind = destinationKind;
        this.surfaceSettings = surfaceSettings == null ? List.of() : surfaceSettings;
    }

    /**
     * Returns the server-issued destination identifier.
     *
     * @return the destination identifier, or empty when the server omitted it
     */
    public Optional<String> destinationId() {
        return Optional.ofNullable(destinationId);
    }

    /**
     * Returns the single-letter destination-kind marker.
     *
     * @return the destination-kind marker, or empty when the server omitted it
     */
    public Optional<String> destinationKind() {
        return Optional.ofNullable(destinationKind);
    }

    /**
     * Returns the per-surface settings.
     *
     * @return an unmodifiable view of the per-surface settings; never
     *         {@code null}, possibly empty
     */
    public List<CrossPostingSurfaceSetting> surfaceSettings() {
        return Collections.unmodifiableList(surfaceSettings);
    }
}
