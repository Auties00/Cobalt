package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Input model for {@code querySimilarNewsletters}. Carries the seed
 * newsletter whose neighbours are requested along with the optional page
 * size, country-code filter, and status-metadata flag that shape the
 * returned page of similar WhatsApp Channels.
 *
 * <p>Only the seed newsletter {@link Jid} is required; the limit bounds the
 * result set, the country codes narrow it to the given regions, and the
 * status-metadata flag toggles an optional sub-selection on the relay
 * response.
 */
@ProtobufMessage
public final class SimilarNewslettersQuery {
    /**
     * JID of the seed newsletter whose similar channels are requested.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid newsletter;

    /**
     * The optional page size bounding how many channels the relay returns.
     * {@code null} when the relay default applies.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final Long limit;

    /**
     * The optional ISO country-code filter restricting the result to
     * channels from the given regions. {@code null} when no country filter
     * is applied.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> countryCodes;

    /**
     * Whether the optional {@code status_metadata} sub-selection is
     * requested on the relay response.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean fetchStatusMetadata;

    /**
     * Constructs a new {@code SimilarNewslettersQuery}.
     *
     * @param newsletter          the seed newsletter JID; required
     * @param limit               the page size, or {@code null}
     * @param countryCodes        the ISO country-code filter, or {@code null}
     * @param fetchStatusMetadata whether to request the status-metadata
     *                            sub-selection
     * @throws NullPointerException if {@code newsletter} is {@code null}
     */
    SimilarNewslettersQuery(Jid newsletter, Long limit, List<String> countryCodes, boolean fetchStatusMetadata) {
        this.newsletter = Objects.requireNonNull(newsletter, "newsletter cannot be null");
        this.limit = limit;
        this.countryCodes = countryCodes;
        this.fetchStatusMetadata = fetchStatusMetadata;
    }

    /**
     * Returns the seed newsletter JID.
     *
     * @return the seed newsletter JID, never {@code null}
     */
    public Jid newsletter() {
        return newsletter;
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
        var that = (SimilarNewslettersQuery) obj;
        return Objects.equals(newsletter, that.newsletter) &&
                Objects.equals(limit, that.limit) &&
                Objects.equals(countryCodes, that.countryCodes) &&
                fetchStatusMetadata == that.fetchStatusMetadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(newsletter, limit, countryCodes, fetchStatusMetadata);
    }

    @Override
    public String toString() {
        return "SimilarNewslettersQuery[" +
                "newsletter=" + newsletter + ", " +
                "limit=" + limit + ", " +
                "countryCodes=" + countryCodes + ", " +
                "fetchStatusMetadata=" + fetchStatusMetadata + ']';
    }
}
