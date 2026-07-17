package com.github.auties00.cobalt.wire.linked.message;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;

import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Common abstraction over every kind of message envelope surfaced by
 * WhatsApp, whether it originates from an end-to-end encrypted chat or
 * a plaintext newsletter.
 *
 * <p>A {@code LinkedMessageInfo} wraps the actual {@link LinkedMessageContainer}
 * payload alongside the metadata needed to display, track, and act on
 * a message: its identifying key, its timestamp, its delivery status,
 * whether the user has starred it, and the per-recipient receipts it
 * has accumulated.
 *
 * <p>The hierarchy is sealed to the two valid variants:
 * <ul>
 *   <li>{@link ChatMessageInfo} for Signal-encrypted one-to-one and
 *       group chat messages</li>
 *   <li>{@link NewsletterMessageInfo} for plaintext messages broadcast
 *       through newsletters (channels)</li>
 * </ul>
 *
 * <p>Callers can use pattern matching to branch on the concrete type
 * when they need access to variant-specific metadata; when only the
 * shared fields matter, programming against this interface is enough.
 */
public sealed interface LinkedMessageInfo extends MessageInfo permits ChatMessageInfo, NewsletterMessageInfo {

    /**
     * Returns the key that uniquely identifies this message within its
     * conversation.
     *
     * <p>The key combines the stanza-level message identifier, the JID
     * of the chat or newsletter, the sender JID, and the
     * {@code fromMe} flag indicating the direction of the message.
     *
     * @return the non-{@code null} message key
     */
    MessageKey key();

    /**
     * Returns the decoded payload container of this message.
     *
     * <p>If the payload is missing (for example a stub system
     * notification or a message that could not be decoded), the
     * returned container is {@link LinkedMessageContainer#empty()} rather
     * than {@code null}.
     *
     * @return the non-{@code null} message container
     */
    LinkedMessageContainer message();

    /**
     * Returns the instant at which the message was sent or received,
     * if known.
     *
     * <p>The timestamp is derived from the {@code t} stanza attribute
     * or the {@code messageTimestamp} protobuf field.
     *
     * @return an {@link Optional} holding the timestamp, or empty if
     *         no timestamp is recorded
     */
    Optional<Instant> timestamp();

    /**
     * Returns the current delivery or acknowledgment state of this
     * message, if known.
     *
     * <p>Messages progress through the standard lifecycle
     * {@code PENDING -> SERVER_ACK -> DELIVERED -> READ -> PLAYED}
     * as acknowledgments arrive from the server and recipients.
     *
     * @return an {@link Optional} holding the current status, or empty
     *         if no status is recorded
     */
    Optional<MessageStatus> status();

    /**
     * Returns whether the user has starred (bookmarked) this message.
     *
     * @return {@code true} if starred, {@code false} otherwise
     */
    boolean starred();

    /**
     * Returns the per-recipient delivery and read receipts accumulated
     * for this message so far.
     *
     * <p>In one-to-one chats this list typically contains at most one
     * receipt; in group chats it can contain one entry per participant
     * device that has acknowledged the message.
     *
     * @return an unmodifiable list of receipts, never {@code null}
     */
    List<MessageReceipt> receipts();
}
