package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One server-registered media entry attached to a WhatsApp native ad on the
 * status surface.
 *
 * <p>Already-uploaded ad media (by advertising-platform id and image-or-video
 * kind) must be registered with the server before it can run on the status
 * surface. The server returns one entry per registered medium pairing the
 * resulting media identifier with its media kind. This model is one such
 * registration entry.
 *
 * <p>The {@linkplain #kind() kind} value set is server-driven; the WhatsApp
 * client treats it as opaque and keeps it as a {@link String}.
 */
@ProtobufMessage(name = "AdMediaRegistration")
public final class AdMediaRegistration {
    /**
     * Server-assigned media identifier, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Media kind discriminator, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String kind;

    /**
     * Constructs a new {@code AdMediaRegistration}. Any reference argument
     * may be {@code null} when the server omitted the corresponding field.
     *
     * @param id   the server-assigned media identifier, or {@code null}
     * @param kind the media kind discriminator, or {@code null}
     */
    AdMediaRegistration(String id, String kind) {
        this.id = id;
        this.kind = kind;
    }

    /**
     * Returns the server-assigned media identifier.
     *
     * @return the media identifier, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the media kind discriminator.
     *
     * @return the media kind, or empty when the server omitted it
     */
    public Optional<String> kind() {
        return Optional.ofNullable(kind);
    }
}
