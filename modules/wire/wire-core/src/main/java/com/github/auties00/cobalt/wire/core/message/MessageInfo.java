package com.github.auties00.cobalt.wire.core.message;

import java.time.Instant;
import java.util.Optional;

/**
 * Transport-agnostic view of a received or sent message and its shared metadata.
 *
 * <p>Both transports surface incoming and outgoing messages, but the concrete envelopes differ:
 * the Linked transport uses {@code LinkedMessageInfo} (backed by the WhatsApp Web {@code
 * WebMessageInfo} and newsletter protobufs), while the Cloud transport uses its own message-info
 * type. This interface carries only the metadata common to both planes, so client-surface
 * operations and listeners can name a message without committing to either transport.
 *
 * <p>Transport-specific metadata (starred state, per-recipient receipts, and other Web-only fields)
 * lives on the Linked variant, not here. The interface is non-sealed because its implementors reside
 * in the downstream transport modules, which the module system does not permit a sealed hierarchy to
 * span.
 */
public interface MessageInfo {
    /**
     * Returns the key that uniquely identifies this message within its conversation.
     *
     * @return the non-{@code null} message key
     */
    MessageKey key();

    /**
     * Returns the envelope carrying this message's content.
     *
     * @return the non-{@code null} message envelope
     */
    MessageContainer message();

    /**
     * Returns the instant at which the message was sent or received, if known.
     *
     * @return an {@link Optional} holding the timestamp, or empty when none is recorded
     */
    Optional<Instant> timestamp();

    /**
     * Returns the current delivery or acknowledgment state of this message, if known.
     *
     * @return an {@link Optional} holding the status, or empty when none is recorded
     */
    Optional<MessageStatus> status();
}
