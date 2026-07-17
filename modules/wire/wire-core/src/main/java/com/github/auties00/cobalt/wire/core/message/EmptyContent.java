package com.github.auties00.cobalt.wire.core.message;

/**
 * The transport-agnostic empty-content sentinel.
 *
 * <p>A message envelope returns this singleton from its content accessor when it carries no payload,
 * so callers never observe {@code null}. It exists in {@code wire-core} so that the Cloud envelope
 * can signal empty content without referencing the Linked transport's own empty-message type.
 *
 * <p>The class carries no state and has exactly one instance, obtained through {@link #of()}.
 */
public final class EmptyContent implements MessageContent {
    /**
     * The single empty-content instance.
     */
    private static final EmptyContent INSTANCE = new EmptyContent();

    /**
     * Constructs the sentinel.
     */
    private EmptyContent() {
    }

    /**
     * Returns the empty-content sentinel.
     *
     * @return the single {@link EmptyContent} instance, never {@code null}
     */
    public static EmptyContent of() {
        return INSTANCE;
    }
}
