package com.github.auties00.cobalt.wire.linked.business.cart;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Promotional sale-price block attached to a {@link BusinessCartProduct}
 * when the merchant has configured a temporary discount on the catalogue
 * item.
 *
 * <p>WhatsApp Business merchants can mark catalogue products as on sale
 * for a bounded time window. When the cart is rebuilt, items currently
 * on sale carry both their regular price (on
 * {@link BusinessCartProduct#price()}) and this discounted price block,
 * so the client can show the strike-through original alongside the
 * promotional amount and optionally display the sale window.
 *
 * <p>The {@link #price()} field is always populated when this block is
 * present; the start and end dates are optional and may be omitted by
 * merchants who run open-ended sales.
 */
@ProtobufMessage
public final class BusinessCartProductSalePrice {
    /**
     * Discounted sale price as a stringified decimal amount in the cart's
     * currency, for example {@code "9.99"}. Always present when this sale
     * block is attached.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String price;

    /**
     * Sale-start date as a server-formatted string. Marks the moment the
     * promotional price became effective. Absent when the merchant runs
     * an open-ended sale with no defined start.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String startDate;

    /**
     * Sale-end date as a server-formatted string. Marks the moment the
     * promotional price expires. Absent when the merchant runs an
     * open-ended sale with no defined end.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String endDate;

    /**
     * Constructs a new {@code BusinessCartProductSalePrice} with the
     * specified discounted price and optional sale window. The price is
     * required; the dates may be {@code null} when the sale is open-ended.
     *
     * @param price     the discounted price as a stringified decimal; never {@code null}
     * @param startDate the sale-start date string, or {@code null}
     * @param endDate   the sale-end date string, or {@code null}
     */
    BusinessCartProductSalePrice(String price, String startDate, String endDate) {
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Returns the discounted sale price as a stringified decimal amount in
     * the cart's currency.
     *
     * @return the sale price; never {@code null}
     */
    public String price() {
        return price;
    }

    /**
     * Returns the sale-start date marking the moment the promotional price
     * became effective.
     *
     * @return an {@code Optional} containing the start-date string, or empty
     *         if the sale has no defined start
     */
    public Optional<String> startDate() {
        return Optional.ofNullable(startDate);
    }

    /**
     * Returns the sale-end date marking the moment the promotional price
     * expires.
     *
     * @return an {@code Optional} containing the end-date string, or empty if
     *         the sale has no defined end
     */
    public Optional<String> endDate() {
        return Optional.ofNullable(endDate);
    }

    /**
     * Sets the discounted sale price as a stringified decimal amount.
     *
     * @param price the price string to set; never {@code null}
     * @throws NullPointerException if {@code price} is {@code null}
     */
    public void setPrice(String price) {
        this.price = Objects.requireNonNull(price, "price cannot be null");
    }

    /**
     * Sets the sale-start date marking the moment the promotional price
     * became effective.
     *
     * @param startDate the start-date string to set, or {@code null} to clear
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /**
     * Sets the sale-end date marking the moment the promotional price expires.
     *
     * @param endDate the end-date string to set, or {@code null} to clear
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCartProductSalePrice) obj;
        return Objects.equals(this.price, that.price) &&
               Objects.equals(this.startDate, that.startDate) &&
               Objects.equals(this.endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, startDate, endDate);
    }

    @Override
    public String toString() {
        return "BusinessCartProductSalePrice[" +
               "price=" + price + ", " +
               "startDate=" + startDate + ", " +
               "endDate=" + endDate + ']';
    }
}
