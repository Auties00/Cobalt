package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the connected account's own contributions on a single
 * newsletter message — the emoji reaction (if any) and the poll vote
 * (if any) that the user placed on that specific message.
 *
 * <p>Newsletter contributions are tracked per-account by the relay
 * to power the "my reactions" and "my votes" affordances on the
 * newsletter detail screen, so that the UI can highlight the user's
 * own selections without having to scan the aggregated tallies.
 *
 * <p>Either of {@link #reaction()} or {@link #pollVote()} may be
 * present, depending on what the user has done with the message.
 */
@ProtobufMessage
public final class NewsletterMyAddOn {
    /**
     * The server-assigned monotonic id of the newsletter message
     * this entry refers to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    long serverId;

    /**
     * The user's own reaction on the message, when one has been
     * placed.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    Reaction reaction;

    /**
     * The user's own poll vote on the message, when the message is a
     * poll and the user has cast a vote.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    PollVote pollVote;

    /**
     * Constructs a new {@code NewsletterMyAddOn}. Invoked by the
     * generated protobuf deserializer and by the converters that
     * adapt wire responses into the domain model.
     *
     * @param serverId the server-assigned monotonic id
     * @param reaction the optional own reaction; may be {@code null}
     * @param pollVote the optional own poll vote; may be
     *                 {@code null}
     */
    NewsletterMyAddOn(long serverId, Reaction reaction, PollVote pollVote) {
        this.serverId = serverId;
        this.reaction = reaction;
        this.pollVote = pollVote;
    }

    /**
     * Returns the server-assigned monotonic id of the newsletter
     * message this entry refers to.
     *
     * @return the server id
     */
    public long serverId() {
        return serverId;
    }

    /**
     * Returns the user's own reaction on the message.
     *
     * @return an {@link Optional} carrying the reaction, or empty
     *         when no reaction has been placed
     */
    public Optional<Reaction> reaction() {
        return Optional.ofNullable(reaction);
    }

    /**
     * Returns the user's own poll vote on the message.
     *
     * @return an {@link Optional} carrying the poll vote, or empty
     *         when the message is not a poll or the user has not
     *         voted
     */
    public Optional<PollVote> pollVote() {
        return Optional.ofNullable(pollVote);
    }

    /**
     * Returns whether this entry equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterMyAddOn} carrying equal field values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterMyAddOn that
                && serverId == that.serverId
                && Objects.equals(reaction, that.reaction)
                && Objects.equals(pollVote, that.pollVote);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(serverId, reaction, pollVote);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string listing every field
     */
    @Override
    public String toString() {
        return "NewsletterMyAddOn[" +
                "serverId=" + serverId +
                ", reaction=" + reaction +
                ", pollVote=" + pollVote +
                ']';
    }

    /**
     * The user's own emoji reaction on a newsletter message.
     */
    @ProtobufMessage
    public static final class Reaction {
        /**
         * The emoji content of the reaction.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String code;

        /**
         * The moment at which the reaction was placed.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
        Instant timestamp;

        /**
         * Constructs a new {@code Reaction}. Invoked by the generated
         * protobuf deserializer and by the converters that adapt wire
         * responses into the domain model.
         *
         * @param code      the emoji code; must not be {@code null}
         * @param timestamp the reaction time; may be {@code null}
         * @throws NullPointerException if {@code code} is
         *                              {@code null}
         */
        Reaction(String code, Instant timestamp) {
            this.code = Objects.requireNonNull(code, "code cannot be null");
            this.timestamp = timestamp;
        }

        /**
         * Returns the emoji content of the reaction.
         *
         * @return the emoji code, never {@code null}
         */
        public String code() {
            return code;
        }

        /**
         * Returns the moment at which the reaction was placed.
         *
         * @return an {@link Optional} carrying the reaction time, or
         *         empty when the relay omitted it
         */
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Returns whether this reaction equals the supplied object.
         *
         * @param o the object to compare against
         * @return {@code true} if {@code o} is a {@code Reaction}
         *         carrying equal field values
         */
        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Reaction that
                    && Objects.equals(code, that.code)
                    && Objects.equals(timestamp, that.timestamp);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(code, timestamp);
        }

        /**
         * Returns a debug-oriented string representation.
         *
         * @return a human-readable string listing every field
         */
        @Override
        public String toString() {
            return "Reaction[code=" + code + ", timestamp=" + timestamp + ']';
        }
    }

    /**
     * The user's own poll vote on a newsletter poll message.
     *
     * <p>Each entry in {@link #optionIds()} is the opaque 32-byte
     * option identifier the relay assigned to a poll option; the
     * caller cross-references these against the option ids of the
     * poll message to determine which options were selected.
     */
    @ProtobufMessage
    public static final class PollVote {
        /**
         * The moment at which the vote was cast.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
        Instant timestamp;

        /**
         * The opaque 32-byte option identifiers selected by the user.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        List<byte[]> optionIds;

        /**
         * Constructs a new {@code PollVote}. Invoked by the generated
         * protobuf deserializer and by the converters that adapt wire
         * responses into the domain model.
         *
         * @param timestamp the vote time; may be {@code null}
         * @param optionIds the selected option identifiers; defaulted
         *                  to an empty list when {@code null}
         */
        PollVote(Instant timestamp, List<byte[]> optionIds) {
            this.timestamp = timestamp;
            this.optionIds = optionIds == null ? List.of() : List.copyOf(optionIds);
        }

        /**
         * Returns the moment at which the vote was cast.
         *
         * @return an {@link Optional} carrying the vote time, or
         *         empty when the relay omitted it
         */
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Returns the opaque 32-byte option identifiers selected by
         * the user.
         *
         * @return an unmodifiable list of option identifiers, never
         *         {@code null}
         */
        public List<byte[]> optionIds() {
            return Collections.unmodifiableList(optionIds);
        }

        /**
         * Returns whether this vote equals the supplied object.
         *
         * @param o the object to compare against
         * @return {@code true} if {@code o} is a {@code PollVote}
         *         carrying equal field values
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof PollVote that)) return false;
            if (!Objects.equals(timestamp, that.timestamp)) return false;
            if (optionIds.size() != that.optionIds.size()) return false;
            for (var i = 0; i < optionIds.size(); i++) {
                if (!Arrays.equals(optionIds.get(i), that.optionIds.get(i))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            var result = Objects.hashCode(timestamp);
            for (var optionId : optionIds) {
                result = 31 * result + Arrays.hashCode(optionId);
            }
            return result;
        }

        /**
         * Returns a debug-oriented string representation.
         *
         * @return a human-readable string listing every field
         */
        @Override
        public String toString() {
            return "PollVote[timestamp=" + timestamp + ", optionIds=" + optionIds.size() + ']';
        }
    }
}
