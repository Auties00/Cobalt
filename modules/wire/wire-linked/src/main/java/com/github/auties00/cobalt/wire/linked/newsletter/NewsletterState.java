package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Carries the lifecycle state of a newsletter as reported by the server.
 *
 * <p>The state is modelled as a lowercase string drawn from the set
 * {@code "active"}, {@code "suspended"}, and {@code "geosuspended"}. A
 * {@code "suspended"} state means the newsletter has been administratively
 * suspended, while {@code "geosuspended"} means the newsletter is blocked in
 * one or more countries.
 *
 * <p>A dedicated {@linkplain #unknown() unknown state} singleton is provided
 * for situations where the server has not reported any state yet.
 */
@ProtobufMessage
public final class NewsletterState {
    /**
     * Shared singleton returned by {@link #unknown()} when the state is not
     * known.
     */
    private static final NewsletterState UNKNOWN = new NewsletterState(null);

    /**
     * The raw state string, lowercased. May be {@code null} to represent the
     * unknown state.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String type;

    /**
     * Constructs a new {@code NewsletterState} carrying the supplied raw
     * state string. Invoked by the generated protobuf deserializer.
     *
     * @param type the raw state string, may be {@code null}
     */
    NewsletterState(String type) {
        this.type = type;
    }

    /**
     * Returns the shared singleton representing an unknown state.
     *
     * @return the unknown state singleton, never {@code null}
     */
    public static NewsletterState unknown() {
        return UNKNOWN;
    }

    /**
     * Returns the raw state string.
     *
     * @return an {@link Optional} holding the state string, or empty when
     *         the state is unknown
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the raw state string.
     *
     * @param type the new state string, or {@code null} to mark the state
     *             as unknown
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns whether this state equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterState}
     *         carrying the same raw state string
     */
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof NewsletterState that
                            && Objects.equals(type, that.type);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this state
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }
}
