package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * Aggregated response of the WhatsApp business-category typeahead query, used by the
 * profile-edit flow to show the user the possible verticals they can pick for their
 * business (such as "Restaurant", "Retail", or "Medical Services").
 *
 * <p>The response pairs the matched {@link BusinessCategoryHit}s with the identifier
 * of the synthetic "not a business" placeholder. WhatsApp returns the placeholder id
 * as a separate top-level field so the client can match it against the
 * {@linkplain BusinessCategoryHit#id() id} of one of the hits in order to render the
 * matching entry as a special "this is not a business" option in the picker UI.
 */
@ProtobufMessage
public final class BusinessCategoryTypeahead {
    /**
     * The category hits matching the typeahead query, in the order returned by the
     * server. The server preserves its own ranking, so clients should display the
     * entries in the order received.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BusinessCategoryHit> categories;

    /**
     * The identifier of the synthetic "not a business" placeholder hit when one was
     * returned by the server, or {@code null} when the response did not include a
     * placeholder. Clients match this id against the hits in {@link #categories} to
     * locate and specially render the placeholder entry.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String notABizId;

    /**
     * Constructs a new business-category typeahead response with the specified hits
     * and not-a-business placeholder identifier.
     *
     * @param categories the matched category hits
     * @param notABizId  the not-a-business placeholder id, or {@code null} when none
     *                   was returned
     */
    BusinessCategoryTypeahead(List<BusinessCategoryHit> categories, String notABizId) {
        this.categories = categories;
        this.notABizId = notABizId;
    }

    /**
     * Returns the category hits matching the typeahead query in server order.
     *
     * @return a non-{@code null}, unmodifiable view of the hits, possibly empty
     */
    public SequencedCollection<BusinessCategoryHit> categories() {
        return categories == null ? List.of() : Collections.unmodifiableSequencedCollection(categories);
    }

    /**
     * Sets the category hits matching the typeahead query.
     *
     * @param categories the hits, or {@code null} for an empty list
     */
    public void setCategories(List<BusinessCategoryHit> categories) {
        this.categories = categories;
    }

    /**
     * Returns the identifier of the synthetic "not a business" placeholder hit, if
     * one was returned by the server.
     *
     * @return an {@link Optional} containing the placeholder id, or
     *         {@link Optional#empty()} when none was returned
     */
    public Optional<String> notABizId() {
        return Optional.ofNullable(notABizId);
    }

    /**
     * Sets the identifier of the synthetic "not a business" placeholder hit.
     *
     * @param notABizId the placeholder id, or {@code null} to clear
     */
    public void setNotABizId(String notABizId) {
        this.notABizId = notABizId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCategoryTypeahead) obj;
        return Objects.equals(this.categories, that.categories) &&
               Objects.equals(this.notABizId, that.notABizId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categories, notABizId);
    }

    @Override
    public String toString() {
        return "BusinessCategoryTypeahead[" +
               "categories=" + categories + ", " +
               "notABizId=" + notABizId + ']';
    }
}
