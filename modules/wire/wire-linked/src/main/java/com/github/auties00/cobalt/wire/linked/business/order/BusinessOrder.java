package com.github.auties00.cobalt.wire.linked.business.order;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * The detail of a WhatsApp Business order returned by the merchant
 * order-detail relay.
 *
 * <p>Orders are produced when a customer checks out a cart of catalog
 * items in a WhatsApp Business chat. The merchant backend then surfaces
 * the order detail to the buyer (and to the merchant's own clients) so
 * the order summary can be rendered: the line items the customer bought,
 * the order subtotal and total, the currency the prices are denominated
 * in, and the wall-clock instant the order was created. All scalar fields
 * are nullable to mirror the per-field nullability of the GraphQL
 * projection emitted by the merchant relay; the items list is never
 * {@code null} but may be empty.
 */
@ProtobufMessage(name = "BusinessOrder")
public final class BusinessOrder {
    /**
     * The merchant-relay order identifier ({@code order_id}) that links this order to its
     * order-details message and subsequent status updates. Empty when the relay omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String referenceId;

    /**
     * The wall-clock instant the order was created on the merchant
     * backend. Empty when the relay omitted the {@code created_at}
     * field.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant createdAt;

    /**
     * The ISO 4217 currency code (for example {@code "USD"} or
     * {@code "EUR"}) the order's monetary fields are denominated in.
     * Empty when the relay omitted the currency.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String currency;

    /**
     * The order subtotal expressed in thousandths of the currency unit
     * (matching the WhatsApp Business pricing convention). Empty when
     * the relay omitted the subtotal.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    Long subtotal;

    /**
     * The order total expressed in thousandths of the currency unit.
     * Empty when the relay omitted the total.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    Long total;

    /**
     * The line items belonging to this order. Never {@code null};
     * possibly empty when the order has no items associated yet.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    List<BusinessOrderItem> items;

    /**
     * The opaque order access token returned by the merchant relay, used to query the order detail.
     * Empty when the relay omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String token;

    /**
     * Constructs a new {@code BusinessOrder} with the given field
     * values. All scalar arguments may be {@code null} when the merchant
     * relay omitted the corresponding field; a {@code null} {@code items}
     * list is coerced to an empty list.
     *
     * @param referenceId the merchant-relay order identifier, or {@code null}
     * @param createdAt   the order creation instant, or {@code null}
     * @param currency    the ISO 4217 currency code, or {@code null}
     * @param subtotal    the order subtotal in thousandths, or {@code null}
     * @param total       the order total in thousandths, or {@code null}
     * @param items       the line items; {@code null} is treated as empty
     * @param token       the order access token, or {@code null}
     */
    BusinessOrder(String referenceId,
                  Instant createdAt,
                  String currency,
                  Long subtotal,
                  Long total,
                  List<BusinessOrderItem> items,
                  String token) {
        this.referenceId = referenceId;
        this.createdAt = createdAt;
        this.currency = currency;
        this.subtotal = subtotal;
        this.total = total;
        this.items = items == null ? List.of() : items;
        this.token = token;
    }

    /**
     * Returns the merchant-relay order identifier ({@code order_id}).
     *
     * @return an {@code Optional} containing the reference id, or empty when the relay omitted it
     */
    public Optional<String> referenceId() {
        return Optional.ofNullable(referenceId);
    }

    /**
     * Returns the wall-clock instant the order was created on the
     * merchant backend.
     *
     * @return an {@code Optional} containing the creation instant, or
     *         empty when the relay omitted it
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns the ISO 4217 currency code the order's monetary fields
     * are denominated in.
     *
     * @return an {@code Optional} containing the currency, or empty when
     *         the relay omitted it
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the order subtotal expressed in thousandths of the
     * currency unit.
     *
     * @return an {@code Optional} containing the subtotal, or empty when
     *         the relay omitted it
     */
    public Optional<Long> subtotal() {
        return Optional.ofNullable(subtotal);
    }

    /**
     * Returns the order total expressed in thousandths of the currency
     * unit.
     *
     * @return an {@code Optional} containing the total, or empty when
     *         the relay omitted it
     */
    public Optional<Long> total() {
        return Optional.ofNullable(total);
    }

    /**
     * Returns the line items belonging to this order.
     *
     * @return an unmodifiable view of the items; never {@code null},
     *         possibly empty
     */
    public SequencedCollection<BusinessOrderItem> items() {
        return Collections.unmodifiableSequencedCollection(items);
    }

    /**
     * Returns the opaque order access token used to query the order detail.
     *
     * @return an {@code Optional} containing the token, or empty when the relay omitted it
     */
    public Optional<String> token() {
        return Optional.ofNullable(token);
    }

    /**
     * Sets the merchant-relay order identifier.
     *
     * @param referenceId the reference id to set, or {@code null} to clear
     */
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * Sets the wall-clock instant the order was created.
     *
     * @param createdAt the creation instant to set, or {@code null} to clear
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Sets the ISO 4217 currency code the order's monetary fields are
     * denominated in.
     *
     * @param currency the currency to set, or {@code null} to clear
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Sets the order subtotal expressed in thousandths of the currency
     * unit.
     *
     * @param subtotal the subtotal to set, or {@code null} to clear
     */
    public void setSubtotal(Long subtotal) {
        this.subtotal = subtotal;
    }

    /**
     * Sets the order total expressed in thousandths of the currency
     * unit.
     *
     * @param total the total to set, or {@code null} to clear
     */
    public void setTotal(Long total) {
        this.total = total;
    }

    /**
     * Sets the line items belonging to this order. A {@code null} list
     * is coerced to an empty list.
     *
     * @param items the items to set, or {@code null} to clear
     */
    public void setItems(List<BusinessOrderItem> items) {
        this.items = items == null ? List.of() : items;
    }

    /**
     * Sets the opaque order access token.
     *
     * @param token the token to set, or {@code null} to clear
     */
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessOrder) obj;
        return Objects.equals(this.referenceId, that.referenceId) &&
                Objects.equals(this.createdAt, that.createdAt) &&
                Objects.equals(this.currency, that.currency) &&
                Objects.equals(this.subtotal, that.subtotal) &&
                Objects.equals(this.total, that.total) &&
                Objects.equals(this.items, that.items) &&
                Objects.equals(this.token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceId, createdAt, currency, subtotal, total, items, token);
    }

    @Override
    public String toString() {
        return "BusinessOrder[" +
                "referenceId=" + referenceId + ", " +
                "createdAt=" + createdAt + ", " +
                "currency=" + currency + ", " +
                "subtotal=" + subtotal + ", " +
                "total=" + total + ", " +
                "items=" + items + ", " +
                "token=" + token + ']';
    }
}
