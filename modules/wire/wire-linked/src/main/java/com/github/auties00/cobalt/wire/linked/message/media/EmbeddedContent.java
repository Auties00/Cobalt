package com.github.auties00.cobalt.wire.linked.message.media;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A wrapper around content that can be embedded inside another message.
 *
 * <p>Embedded content is typically attached to link previews or status updates to
 * carry either a reference to another chat message or a piece of music metadata.
 * At most one of {@link EmbeddedMessage} and {@link EmbeddedMusic} is populated at a
 * time; {@link #content()} exposes whichever variant is present through the sealed
 * {@link EmbeddedContentVariant} interface.
 */
@ProtobufMessage(name = "EmbeddedContent")
public final class EmbeddedContent {
    /**
     * Reference to another chat message embedded as part of this content, or
     * {@code null} if no message is embedded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    EmbeddedMessage embeddedMessage;

    /**
     * Music metadata embedded as part of this content, or {@code null} if no music
     * is embedded.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    EmbeddedMusic embeddedMusic;


    /**
     * Constructs a new embedded content wrapper.
     *
     * @param embeddedMessage an embedded reference to another message, or {@code null}
     * @param embeddedMusic   embedded music metadata, or {@code null}
     */
    EmbeddedContent(EmbeddedMessage embeddedMessage, EmbeddedMusic embeddedMusic) {
        this.embeddedMessage = embeddedMessage;
        this.embeddedMusic = embeddedMusic;
    }

    /**
     * Returns the embedded variant populated in this wrapper.
     *
     * <p>If an embedded message is set it takes precedence, otherwise the embedded
     * music is returned. If neither is present, an empty {@link Optional} is returned.
     *
     * @return the embedded content variant, or empty if none is set
     */
    public Optional<? extends EmbeddedContentVariant> content() {
        if (embeddedMessage != null) return Optional.of(embeddedMessage);
        if (embeddedMusic != null) return Optional.of(embeddedMusic);
        return Optional.empty();
    }

    /**
     * Updates the embedded message variant.
     *
     * @param embeddedMessage the new embedded message, or {@code null} to clear
     */
    public void setEmbeddedMessage(EmbeddedMessage embeddedMessage) {
        this.embeddedMessage = embeddedMessage;
    }

    /**
     * Updates the embedded music variant.
     *
     * @param embeddedMusic the new embedded music, or {@code null} to clear
     */
    public void setEmbeddedMusic(EmbeddedMusic embeddedMusic) {
        this.embeddedMusic = embeddedMusic;
    }
}
