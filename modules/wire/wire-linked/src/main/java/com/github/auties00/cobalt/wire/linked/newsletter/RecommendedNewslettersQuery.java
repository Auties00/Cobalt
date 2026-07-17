package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Input model for {@code queryRecommendedNewsletters}. Carries the optional
 * page size, the optional country-code filter, and the status-metadata flag
 * that shape the requested page of recommended WhatsApp Channels.
 *
 * <p>None of the fields are required; the limit bounds the result set, the
 * country codes narrow it to the given regions, and the status-metadata
 * flag toggles an optional sub-selection on the relay response.
 */
@ProtobufMessage
public final class RecommendedNewslettersQuery {
    /**
     * The optional page size bounding how many channels the relay returns.
     * {@code null} when the relay default applies.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    final Long limit;

    /**
     * The optional ISO country-code filter restricting the recommendations
     * to channels from the given regions. {@code null} when no country
     * filter is applied.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> countryCodes;

    /**
     * Whether the optional {@code status_metadata} sub-selection is
     * requested on the relay response.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean fetchStatusMetadata;

    /**
     * Constructs a new {@code RecommendedNewslettersQuery}.
     *
     * @param limit               the page size, or {@code null}
     * @param countryCodes        the ISO country-code filter, or {@code null}
     * @param fetchStatusMetadata whether to request the status-metadata
     *                            sub-selection
     */
    RecommendedNewslettersQuery(Long limit, List<String> countryCodes, boolean fetchStatusMetadata) {
        this.limit = limit;
        this.countryCodes = countryCodes;
        this.fetchStatusMetadata = fetchStatusMetadata;
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
     * Returns the ISO country-code filter.
     *
     * @return the country codes, or an empty {@link List} when no filter is
     *         applied; never {@code null}
     */
    public List<String> countryCodes() {
        return countryCodes == null ? List.of() : countryCodes;
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
        var that = (RecommendedNewslettersQuery) obj;
        return Objects.equals(limit, that.limit) &&
                Objects.equals(countryCodes, that.countryCodes) &&
                fetchStatusMetadata == that.fetchStatusMetadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, countryCodes, fetchStatusMetadata);
    }

    @Override
    public String toString() {
        return "RecommendedNewslettersQuery[" +
                "limit=" + limit + ", " +
                "countryCodes=" + countryCodes + ", " +
                "fetchStatusMetadata=" + fetchStatusMetadata + ']';
    }
}
