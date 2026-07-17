package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;

/**
 * Transport-agnostic contract for a plain or extended text message body.
 *
 * <p>The Cloud representation of text is just the body string (with an optional link preview flag the
 * encoder derives). The Linked transport's extended-text type additionally carries fonts, colors,
 * thumbnails, canonical URL metadata, and context info that the Cloud wire never delivers; those stay
 * on the Linked type and are not part of this contract.
 */
public interface TextContent extends MessageContent {
    /**
     * Returns the text body of this message.
     *
     * @return an {@link Optional} holding the body, or empty when unset
     */
    Optional<String> text();
}
