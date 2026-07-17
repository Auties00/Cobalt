package com.github.auties00.cobalt.wire.core.message;

/**
 * Transport-agnostic message envelope.
 *
 * <p>Every message carried by either Cobalt transport is wrapped in an envelope that exposes its
 * inner {@link MessageContent}. The two transports back this contract with different concrete
 * envelopes: the Linked transport uses the protobuf {@code LinkedMessageContainer} (the WhatsApp
 * {@code Message} oneof with its many nesting layers), while the Cloud transport uses its own
 * envelope. This interface lets callers and the shared message-info contract name an envelope
 * without committing to either transport's concrete type.
 *
 * <p>The interface is non-sealed because its implementors live in the downstream transport modules,
 * which the module system does not permit a sealed hierarchy to span.
 */
public interface MessageContainer {
    /**
     * Returns the content carried by this envelope, unwrapping any transport-specific nesting.
     *
     * @return the inner content; never {@code null} (an empty-content sentinel is returned when the
     *         envelope carries no payload)
     */
    MessageContent content();
}
