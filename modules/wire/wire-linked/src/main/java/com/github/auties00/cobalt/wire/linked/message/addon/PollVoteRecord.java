package com.github.auties00.cobalt.wire.linked.message.addon;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.poll.PollVoteMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Represents a single vote cast on a poll, together with the delivery and
 * read tracking needed to display it in a chat.
 *
 * <p>A poll vote is always tied to its originating poll through a
 * {@link MessageKey} so that clients can match the vote to the correct poll
 * message. The {@link PollVoteMessage} payload carries the encrypted option
 * selections, while the sender and server timestamps describe when the vote
 * was produced and accepted. The {@code unread} flag lets clients highlight
 * new incoming votes in the chat until the user reviews them.
 */
@ProtobufMessage(name = "PollUpdate")
public final class PollVoteRecord {
    /**
     * The key of the poll message this vote applies to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey pollUpdateMessageKey;

    /**
     * The encrypted vote payload, describing which options the voter selected.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    PollVoteMessage vote;

    /**
     * The instant at which the voter produced this vote on the originating device.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant senderTimestampMs;

    /**
     * The instant at which the WhatsApp server acknowledged the vote.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant serverTimestampMs;

    /**
     * Whether this vote has not yet been seen by the local user viewing the poll.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    Boolean unread;


    /**
     * Constructs a new poll vote record with the provided fields.
     *
     * @param pollUpdateMessageKey the key of the poll message this vote applies to
     * @param vote the encrypted vote payload
     * @param senderTimestampMs when the voter produced the vote
     * @param serverTimestampMs when the server accepted the vote
     * @param unread whether the vote is still unread by the local user
     */
    PollVoteRecord(MessageKey pollUpdateMessageKey, PollVoteMessage vote, Instant senderTimestampMs, Instant serverTimestampMs, Boolean unread) {
        this.pollUpdateMessageKey = pollUpdateMessageKey;
        this.vote = vote;
        this.senderTimestampMs = senderTimestampMs;
        this.serverTimestampMs = serverTimestampMs;
        this.unread = unread;
    }

    /**
     * Returns the key of the poll message this vote applies to.
     *
     * @return an {@link Optional} containing the message key, or empty if not set
     */
    public Optional<MessageKey> pollUpdateMessageKey() {
        return Optional.ofNullable(pollUpdateMessageKey);
    }

    /**
     * Returns the encrypted vote payload.
     *
     * @return an {@link Optional} containing the vote message, or empty if not set
     */
    public Optional<PollVoteMessage> vote() {
        return Optional.ofNullable(vote);
    }

    /**
     * Returns the instant at which the voter produced this vote.
     *
     * @return an {@link Optional} containing the sender timestamp, or empty if not set
     */
    public Optional<Instant> senderTimestampMs() {
        return Optional.ofNullable(senderTimestampMs);
    }

    /**
     * Returns the instant at which the WhatsApp server acknowledged this vote.
     *
     * @return an {@link Optional} containing the server timestamp, or empty if not set
     */
    public Optional<Instant> serverTimestampMs() {
        return Optional.ofNullable(serverTimestampMs);
    }

    /**
     * Returns whether this vote is still unread by the local user.
     *
     * <p>Returns {@code false} both when the flag is explicitly unset and when
     * it is {@code false} on the wire.
     *
     * @return {@code true} if the vote has not yet been seen, {@code false} otherwise
     */
    public boolean unread() {
        return unread != null && unread;
    }

    /**
     * Sets the key of the poll message this vote applies to.
     *
     * @param pollUpdateMessageKey the new poll message key, or {@code null} to clear
     */
    public void setPollUpdateMessageKey(MessageKey pollUpdateMessageKey) {
        this.pollUpdateMessageKey = pollUpdateMessageKey;
    }

    /**
     * Sets the encrypted vote payload.
     *
     * @param vote the new vote message, or {@code null} to clear
     */
    public void setVote(PollVoteMessage vote) {
        this.vote = vote;
    }

    /**
     * Sets the instant at which the voter produced this vote.
     *
     * @param senderTimestampMs the new sender timestamp, or {@code null} to clear
     */
    public void setSenderTimestampMs(Instant senderTimestampMs) {
        this.senderTimestampMs = senderTimestampMs;
    }

    /**
     * Sets the instant at which the server acknowledged this vote.
     *
     * @param serverTimestampMs the new server timestamp, or {@code null} to clear
     */
    public void setServerTimestampMs(Instant serverTimestampMs) {
        this.serverTimestampMs = serverTimestampMs;
    }

    /**
     * Sets whether this vote is still unread by the local user.
     *
     * @param unread the new unread flag, or {@code null} to clear
     */
    public void setUnread(Boolean unread) {
        this.unread = unread;
    }
}
