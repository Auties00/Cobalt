package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single editorial category surfaced on the newsletter
 * directory landing screen.
 *
 * <p>The directory landing screen is built around a small set of curated
 * categories ("News", "Sports", "Lifestyle", and so on); each category
 * is rendered as a row containing its title and a horizontal preview
 * carousel of featured channels. This type carries the wire-level
 * category identifier, the human-readable title shown on the row, and
 * the featured channels that populate the carousel.
 */
@ProtobufMessage
public final class NewsletterDirectoryCategory {
    /**
     * The wire-level category identifier (for example {@code "BUSINESS"}).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String identifier;

    /**
     * The human-readable category title shown on the directory row.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String title;

    /**
     * The featured newsletters surfaced on this category row.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<NewsletterDirectoryEntry> featured;

    /**
     * Constructs a new {@code NewsletterDirectoryCategory}. Invoked by
     * the generated protobuf deserializer and by the converters that
     * adapt wire responses into the domain model.
     *
     * @param identifier the wire-level category identifier, may be {@code null}
     * @param title      the human-readable category title, may be {@code null}
     * @param featured   the featured newsletters; defaulted to an empty
     *                   list when {@code null}
     */
    NewsletterDirectoryCategory(String identifier, String title, List<NewsletterDirectoryEntry> featured) {
        this.identifier = identifier;
        this.title = title;
        this.featured = featured == null ? List.of() : List.copyOf(featured);
    }

    /**
     * Returns the wire-level category identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         not reported
     */
    public Optional<String> identifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Returns the human-readable title shown on this category row.
     *
     * @return an {@link Optional} carrying the title, or empty when not
     *         reported
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the featured newsletters surfaced on this category row.
     *
     * @return an unmodifiable list of featured entries, never {@code null}
     */
    public List<NewsletterDirectoryEntry> featured() {
        return Collections.unmodifiableList(featured);
    }

    /**
     * Returns whether this category equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterDirectoryCategory} carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterDirectoryCategory that
                && Objects.equals(identifier, that.identifier)
                && Objects.equals(title, that.title)
                && Objects.equals(featured, that.featured);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(identifier, title, featured);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "NewsletterDirectoryCategory[identifier=" + identifier +
                ", title=" + title +
                ", featured=" + featured.size() + ']';
    }
}
