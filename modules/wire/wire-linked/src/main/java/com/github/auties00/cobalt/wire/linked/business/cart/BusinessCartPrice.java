package com.github.auties00.cobalt.wire.linked.business.cart;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Cart-wide pricing summary attached to a {@link BusinessRefreshedCart}.
 *
 * <p>WhatsApp Business merchants who run a catalogue let customers add items
 * to an in-chat shopping cart that the merchant rebuilds server-side every
 * time the customer reopens it. The rebuilt cart includes a top-level price
 * block that aggregates the line items into a subtotal and total, expressed
 * in the merchant's currency. This class models that aggregate; the
 * per-item pricing lives on {@link BusinessCartProduct}.
 *
 * <p>All four fields are independently optional: a merchant may omit the
 * subtotal/total split, hide pricing entirely behind {@link #priceStatus()}
 * (for instance when the catalogue is configured for "price on request"),
 * or skip the currency code when no monetary amount is present.
 */
@ProtobufMessage
public final class BusinessCartPrice {
    /**
     * Cart subtotal as a stringified decimal amount in the cart's currency,
     * for example {@code "12.50"}. Represents the sum of line-item prices
     * before any cart-level adjustments such as taxes or shipping. Absent
     * when the merchant has not configured a subtotal/total split or when
     * pricing is hidden via {@link #priceStatus()}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String subtotal;

    /**
     * Cart total as a stringified decimal amount in the cart's currency,
     * for example {@code "14.99"}. Represents the final amount the customer
     * would pay if the cart were checked out as-is. Absent when the merchant
     * has not configured a total or when pricing is hidden via
     * {@link #priceStatus()}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String total;

    /**
     * ISO 4217 currency code applied to {@link #subtotal()} and
     * {@link #total()}, for example {@code "USD"} or {@code "EUR"}. Absent
     * when no monetary amount is present in this cart.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String currency;

    /**
     * Server-supplied price-status discriminator that overrides the numeric
     * price when present, for example {@code "PRICE_HIDDEN"} for catalogues
     * that withhold pricing until the customer contacts the merchant. The
     * client renders the status string verbatim in place of the price.
     * Absent when the cart shows a normal numeric price.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String priceStatus;

    /**
     * Constructs a new {@code BusinessCartPrice} with the specified pricing
     * components. All parameters are optional and may be {@code null} when
     * the corresponding component is not present in the cart.
     *
     * @param subtotal    the cart subtotal as a stringified decimal, or {@code null}
     * @param total       the cart total as a stringified decimal, or {@code null}
     * @param currency    the ISO 4217 currency code, or {@code null}
     * @param priceStatus the price-status discriminator, or {@code null}
     */
    BusinessCartPrice(String subtotal, String total, String currency, String priceStatus) {
        this.subtotal = subtotal;
        this.total = total;
        this.currency = currency;
        this.priceStatus = priceStatus;
    }

    /**
     * Returns the cart subtotal as a stringified decimal amount in the cart's
     * currency. Represents the sum of line-item prices before any cart-level
     * adjustments such as taxes or shipping.
     *
     * @return an {@code Optional} containing the subtotal string, or empty if
     *         the merchant did not configure a subtotal/total split
     */
    public Optional<String> subtotal() {
        return Optional.ofNullable(subtotal);
    }

    /**
     * Returns the cart total as a stringified decimal amount in the cart's
     * currency. Represents the final amount the customer would pay if the cart
     * were checked out as-is.
     *
     * @return an {@code Optional} containing the total string, or empty if no
     *         total is present
     */
    public Optional<String> total() {
        return Optional.ofNullable(total);
    }

    /**
     * Returns the ISO 4217 currency code applied to the subtotal and total
     * (for example {@code "USD"} or {@code "EUR"}).
     *
     * @return an {@code Optional} containing the currency code, or empty if no
     *         monetary amount is present
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the server-supplied price-status discriminator that overrides
     * the numeric price when present (for example {@code "PRICE_HIDDEN"}).
     *
     * @return an {@code Optional} containing the status string, or empty if
     *         the cart shows a normal numeric price
     */
    public Optional<String> priceStatus() {
        return Optional.ofNullable(priceStatus);
    }

    /**
     * Sets the cart subtotal as a stringified decimal amount.
     *
     * @param subtotal the subtotal string to set, or {@code null} to clear
     */
    public void setSubtotal(String subtotal) {
        this.subtotal = subtotal;
    }

    /**
     * Sets the cart total as a stringified decimal amount.
     *
     * @param total the total string to set, or {@code null} to clear
     */
    public void setTotal(String total) {
        this.total = total;
    }

    /**
     * Sets the ISO 4217 currency code applied to the subtotal and total.
     *
     * @param currency the currency code to set (for example {@code "USD"}),
     *                 or {@code null} to clear
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Sets the server-supplied price-status discriminator that overrides the
     * numeric price when present.
     *
     * @param priceStatus the price-status string to set, or {@code null} to clear
     */
    public void setPriceStatus(String priceStatus) {
        this.priceStatus = priceStatus;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCartPrice) obj;
        return Objects.equals(this.subtotal, that.subtotal) &&
               Objects.equals(this.total, that.total) &&
               Objects.equals(this.currency, that.currency) &&
               Objects.equals(this.priceStatus, that.priceStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subtotal, total, currency, priceStatus);
    }

    @Override
    public String toString() {
        return "BusinessCartPrice[" +
               "subtotal=" + subtotal + ", " +
               "total=" + total + ", " +
               "currency=" + currency + ", " +
               "priceStatus=" + priceStatus + ']';
    }
}
