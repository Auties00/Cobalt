package com.github.auties00.cobalt.wire.linked.message.media;

/**
 * A sealed interface representing the possible variants of {@link EmbeddedContent}.
 *
 * <p>An embedded content payload is either a reference to another chat message via
 * {@link EmbeddedMessage} or a piece of music metadata via {@link EmbeddedMusic}.
 * Consumers can switch exhaustively over these permitted implementations.
 */
public sealed interface EmbeddedContentVariant permits EmbeddedMessage, EmbeddedMusic {
}
