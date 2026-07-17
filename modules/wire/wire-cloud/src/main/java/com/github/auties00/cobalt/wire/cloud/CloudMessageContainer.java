package com.github.auties00.cobalt.wire.cloud;

import com.github.auties00.cobalt.wire.core.message.EmptyContent;
import com.github.auties00.cobalt.wire.core.message.MessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageContent;

/**
 * The Cloud transport's message envelope.
 *
 * <p>This is the Cloud-native counterpart to the Linked transport's {@code LinkedMessageContainer}.
 * It carries a single {@link MessageContent} decoded from, or to be encoded to, a Cloud webhook or
 * {@code /messages} JSON body. Unlike the Linked envelope it has no nesting layers (ephemeral,
 * view-once, edited, device-sent, and future-proof wrappers are Linked-socket concerns that the
 * Cloud wire never carries), so {@link #content()} returns the stored content directly.
 */
public final class CloudMessageContainer implements MessageContainer {
    /**
     * The content carried by this envelope; never {@code null}.
     */
    private final MessageContent content;

    /**
     * Constructs a Cloud envelope wrapping the given content.
     *
     * @param content the content to carry, or {@code null} to carry the empty sentinel
     */
    public CloudMessageContainer(MessageContent content) {
        this.content = content == null ? EmptyContent.of() : content;
    }

    /**
     * Returns a Cloud envelope wrapping the given content.
     *
     * @param content the content to carry, or {@code null} to carry the empty sentinel
     * @return a new Cloud envelope
     */
    public static CloudMessageContainer of(MessageContent content) {
        return new CloudMessageContainer(content);
    }

    /**
     * Returns an empty Cloud envelope carrying the {@link EmptyContent} sentinel.
     *
     * @return an empty Cloud envelope
     */
    public static CloudMessageContainer empty() {
        return new CloudMessageContainer(EmptyContent.of());
    }

    @Override
    public MessageContent content() {
        return content;
    }
}
