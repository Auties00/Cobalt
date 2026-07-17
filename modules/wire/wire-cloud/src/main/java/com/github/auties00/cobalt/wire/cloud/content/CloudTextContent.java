package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.TextContent;

import java.util.Optional;

/**
 * The Cloud transport's text content body.
 */
public final class CloudTextContent implements TextContent {
    /**
     * The text body, or {@code null} when unset.
     */
    private final String text;

    /**
     * Constructs a Cloud text body.
     *
     * @param text the text body, or {@code null} when unset
     */
    public CloudTextContent(String text) {
        this.text = text;
    }

    @Override
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }
}
