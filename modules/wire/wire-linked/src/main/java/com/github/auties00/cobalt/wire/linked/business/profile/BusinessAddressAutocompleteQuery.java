package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the postal-address typeahead of the WhatsApp Business
 * profile editor.
 *
 * <p>When a merchant edits a Business profile and types the registered
 * place of operation, the address picker streams place suggestions ranked
 * by relevance to the partial text. This input carries the parameters that
 * select which suggestions the server returns: the {@link #center() map
 * center} to bias suggestions around, the partial-address {@link #query()
 * query text}, and the {@link #useCaseId() use-case identifier} scoping the
 * lookup.
 */
@ProtobufMessage(name = "BusinessAddressAutocompleteQuery")
public final class BusinessAddressAutocompleteQuery {
    /**
     * Map center the typeahead biases suggestions around. Unset omits the
     * field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final BusinessGeoPoint center;

    /**
     * Partial address text the merchant has typed so far. The server uses
     * this text to retrieve and rank place suggestions. Required by the
     * autocomplete backend; an unset value omits the field and yields an
     * empty suggestion list.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String query;

    /**
     * Use-case identifier scoping the address lookup on the server. Unset
     * omits the field.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String useCaseId;

    /**
     * Constructs a new {@code BusinessAddressAutocompleteQuery}. Every
     * argument may be {@code null} to omit the corresponding field from the
     * request.
     *
     * @param center    the map center to bias suggestions around, or {@code null}
     * @param query     the partial address text, or {@code null}
     * @param useCaseId the use-case identifier, or {@code null}
     */
    public BusinessAddressAutocompleteQuery(BusinessGeoPoint center, String query, String useCaseId) {
        this.center = center;
        this.query = query;
        this.useCaseId = useCaseId;
    }

    /**
     * Returns the map center the typeahead biases suggestions around.
     *
     * @return an {@link Optional} carrying the center, or empty when unset
     */
    public Optional<BusinessGeoPoint> center() {
        return Optional.ofNullable(center);
    }

    /**
     * Returns the partial address text the merchant has typed.
     *
     * @return an {@link Optional} carrying the query text, or empty when
     *         unset
     */
    public Optional<String> query() {
        return Optional.ofNullable(query);
    }

    /**
     * Returns the use-case identifier scoping the lookup.
     *
     * @return an {@link Optional} carrying the use-case identifier, or empty
     *         when unset
     */
    public Optional<String> useCaseId() {
        return Optional.ofNullable(useCaseId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAddressAutocompleteQuery) obj;
        return Objects.equals(center, that.center)
                && Objects.equals(query, that.query)
                && Objects.equals(useCaseId, that.useCaseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, query, useCaseId);
    }

    @Override
    public String toString() {
        return "BusinessAddressAutocompleteQuery[" +
                "center=" + center + ", " +
                "query=" + query + ", " +
                "useCaseId=" + useCaseId + ']';
    }
}
