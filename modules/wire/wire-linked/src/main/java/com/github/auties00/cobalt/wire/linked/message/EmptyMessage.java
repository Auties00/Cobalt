package com.github.auties00.cobalt.wire.linked.message;

/**
 * Sentinel value returned when a {@link LinkedMessageContainer} holds no payload.
 *
 * <p>WhatsApp occasionally transmits containers that carry only side-channel
 * metadata (such as device-list info or sender-key distribution data) with
 * no actual message body. Rather than returning {@code null} from
 * {@link LinkedMessageContainer#content()}, Cobalt returns the shared
 * {@link #INSTANCE} of this class so callers can safely chain method calls
 * and pattern-match against {@link Message} without risk of a
 * {@code NullPointerException}.
 *
 * <p>Typical usage is to test for this sentinel via
 * {@link LinkedMessageContainer#isEmpty()} or an {@code instanceof} check before
 * processing the message content.
 */
public final class EmptyMessage implements Message {
    /**
     * The singleton instance returned whenever a container has no payload.
     *
     * <p>All empty containers share this same instance; reference equality
     * ({@code ==}) is sufficient and preferred over
     * {@link Object#equals(Object)} comparisons.
     */
    static final EmptyMessage INSTANCE = new EmptyMessage();

    /**
     * Constructs the singleton instance.
     *
     * <p>This constructor is package-private because {@link #INSTANCE}
     * is the only valid reference to an {@code EmptyMessage}; callers
     * must never instantiate additional copies.
     */
    EmptyMessage() {

    }
}
