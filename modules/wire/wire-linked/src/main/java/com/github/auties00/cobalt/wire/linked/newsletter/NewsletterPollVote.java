package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an aggregated poll tally entry within a newsletter poll
 * message.
 *
 * <p>Each entry binds a poll option, identified by its 32-byte SHA-256
 * hash, to the total number of votes that option has received so far.
 * Because newsletter polls are broadcast rather than interactive, only
 * these aggregated tallies are delivered to subscribers: individual voter
 * identities are never exposed.
 */
@ProtobufMessage
public final class NewsletterPollVote {
    /**
     * The 32-byte hash that uniquely identifies the poll option this
     * tally refers to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] hash;

    /**
     * The total number of votes received so far for the identified option.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    long count;

    /**
     * Constructs a new {@code NewsletterPollVote} tallying the supplied
     * vote count for the option identified by the given hash.
     *
     * @param hash  the 32-byte option hash, must not be {@code null}
     * @param count the total vote count
     * @throws NullPointerException if {@code hash} is {@code null}
     */
    public NewsletterPollVote(byte[] hash, long count) {
        this.hash = Objects.requireNonNull(hash, "hash cannot be null");
        this.count = count;
    }

    /**
     * Returns the 32-byte hash that identifies the poll option.
     *
     * @return the option hash, never {@code null}
     */
    public byte[] hash() {
        return hash;
    }

    /**
     * Returns the total number of votes received so far for this option.
     *
     * @return the vote count
     */
    public long count() {
        return count;
    }

    /**
     * Sets the 32-byte hash that identifies the poll option.
     *
     * @param hash the new option hash, must not be {@code null}
     * @throws NullPointerException if {@code hash} is {@code null}
     */
    public void setHash(byte[] hash) {
        this.hash = Objects.requireNonNull(hash, "hash cannot be null");
    }

    /**
     * Sets the total vote count for this option.
     *
     * @param count the new vote count
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * Returns whether this tally equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterPollVote}
     *         with the same hash and count
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterPollVote that
                && Arrays.equals(hash, that.hash)
                && count == that.count;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this tally
     */
    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(hash) + Long.hashCode(count);
    }

    /**
     * Returns a debug-oriented string representation of this tally.
     *
     * @return a human-readable string listing the hash bytes and the count
     */
    @Override
    public String toString() {
        return "NewsletterPollVote[" +
                "hash=" + Arrays.toString(hash) +
                ", count=" + count +
                ']';
    }
}
