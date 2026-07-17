package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Input model for {@code queryNewsletterDirectoryList}. Carries the
 * directory slice to query along with the optional filters and pagination
 * controls that shape the requested page of the WhatsApp Channels explore
 * directory.
 *
 * <p>Only the {@link NewsletterDirectoryListView} slice is required; the
 * country-code and category filters narrow the result set, the limit and
 * cursor token page through it, and the status-metadata flag toggles an
 * optional sub-selection on the relay response.
 */
@ProtobufMessage
public final class NewsletterDirectoryListQuery {
    /**
     * The directory slice to query, selecting which ranking the relay
     * applies to the returned channels.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final NewsletterDirectoryListView view;

    /**
     * The optional ISO country-code filter restricting the directory to
     * channels from the given regions. {@code null} when no country filter
     * is applied.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> countryCodes;

    /**
     * The optional upper-case category wire-string filter restricting the
     * directory to channels in the given categories. {@code null} when no
     * category filter is applied.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> categories;

    /**
     * The optional page size bounding how many channels the relay returns.
     * {@code null} when the relay default applies.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final Long limit;

    /**
     * The optional forward pagination cursor identifying the page to fetch.
     * {@code null} on the first page.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String cursorToken;

    /**
     * Whether the optional {@code status_metadata} sub-selection is
     * requested on the relay response.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean fetchStatusMetadata;

    /**
     * Constructs a new {@code NewsletterDirectoryListQuery}.
     *
     * @param view                the directory slice to query; required
     * @param countryCodes        the ISO country-code filter, or {@code null}
     * @param categories          the category wire-string filter, or
     *                            {@code null}
     * @param limit               the page size, or {@code null}
     * @param cursorToken         the forward pagination cursor, or
     *                            {@code null}
     * @param fetchStatusMetadata whether to request the status-metadata
     *                            sub-selection
     * @throws NullPointerException if {@code view} is {@code null}
     */
    NewsletterDirectoryListQuery(NewsletterDirectoryListView view, List<String> countryCodes, List<String> categories, Long limit, String cursorToken, boolean fetchStatusMetadata) {
        this.view = Objects.requireNonNull(view, "view cannot be null");
        this.countryCodes = countryCodes;
        this.categories = categories;
        this.limit = limit;
        this.cursorToken = cursorToken;
        this.fetchStatusMetadata = fetchStatusMetadata;
    }

    /**
     * Returns the directory slice to query.
     *
     * @return the directory view, never {@code null}
     */
    public NewsletterDirectoryListView view() {
        return view;
    }

    /**
     * Returns the ISO country-code filter.
     *
     * @return the country codes, or an empty {@link List} when no filter is
     *         applied; never {@code null}
     */
    public List<String> countryCodes() {
        return countryCodes == null ? List.of() : countryCodes;
    }

    /**
     * Returns the upper-case category wire-string filter.
     *
     * @return the categories, or an empty {@link List} when no filter is
     *         applied; never {@code null}
     */
    public List<String> categories() {
        return categories == null ? List.of() : categories;
    }

    /**
     * Returns the optional page size.
     *
     * @return an {@link OptionalLong} holding the page size, or empty when
     *         the relay default applies
     */
    public OptionalLong limit() {
        return limit == null ? OptionalLong.empty() : OptionalLong.of(limit);
    }

    /**
     * Returns the optional forward pagination cursor.
     *
     * @return an {@link Optional} holding the cursor token, or empty on the
     *         first page
     */
    public Optional<String> cursorToken() {
        return Optional.ofNullable(cursorToken);
    }

    /**
     * Returns whether the optional status-metadata sub-selection is
     * requested.
     *
     * @return {@code true} when the status-metadata sub-selection is
     *         requested
     */
    public boolean fetchStatusMetadata() {
        return fetchStatusMetadata;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (NewsletterDirectoryListQuery) obj;
        return view == that.view &&
                Objects.equals(countryCodes, that.countryCodes) &&
                Objects.equals(categories, that.categories) &&
                Objects.equals(limit, that.limit) &&
                Objects.equals(cursorToken, that.cursorToken) &&
                fetchStatusMetadata == that.fetchStatusMetadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(view, countryCodes, categories, limit, cursorToken, fetchStatusMetadata);
    }

    @Override
    public String toString() {
        return "NewsletterDirectoryListQuery[" +
                "view=" + view + ", " +
                "countryCodes=" + countryCodes + ", " +
                "categories=" + categories + ", " +
                "limit=" + limit + ", " +
                "cursorToken=" + cursorToken + ", " +
                "fetchStatusMetadata=" + fetchStatusMetadata + ']';
    }
}
