package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregates the voters of a single option of a newsletter poll.
 *
 * <p>Newsletter polls track the senders of every cast vote per option,
 * keyed by an opaque base64 hash of the option text. This type binds an
 * option hash to the voters who selected it, so that the poll-results
 * surface can render a per-option voter list.
 */
@ProtobufMessage
public final class NewsletterPollVoter {
    /**
     * The base64-encoded hash that identifies the poll option these
     * voters selected.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String optionHash;

    /**
     * The voters who selected the identified option.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    List<Jid> voters;

    /**
     * Constructs a new {@code NewsletterPollVoter}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param optionHash the option hash; may be {@code null} when the
     *                   relay reports voters across every option
     * @param voters     the voters; defaulted to an empty list when
     *                   {@code null}
     */
    NewsletterPollVoter(String optionHash, List<Jid> voters) {
        this.optionHash = optionHash;
        this.voters = voters == null ? List.of() : List.copyOf(voters);
    }

    /**
     * Returns the base64-encoded hash that identifies the poll option.
     *
     * @return an {@link Optional} carrying the option hash, or empty when
     *         the relay groups voters across every option
     */
    public Optional<String> optionHash() {
        return Optional.ofNullable(optionHash);
    }

    /**
     * Returns the voters that selected the identified option.
     *
     * @return an unmodifiable list of voter JIDs, never {@code null}
     */
    public List<Jid> voters() {
        return Collections.unmodifiableList(voters);
    }

    /**
     * Returns whether this voter group equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterPollVoter}
     *         carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterPollVoter that
                && Objects.equals(optionHash, that.optionHash)
                && Objects.equals(voters, that.voters);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(optionHash, voters);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "NewsletterPollVoter[optionHash=" + optionHash +
                ", voters=" + voters.size() + ']';
    }
}
