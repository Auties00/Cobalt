package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Discounted-price information attached to a {@link BusinessProduct}
 * during a promotional period.
 *
 * <p>WhatsApp Business catalog products may carry a sale price that
 * supersedes the regular {@linkplain BusinessProduct#price() price}
 * during the configured promotional window. The price is expressed as
 * an opaque string in the same currency as the regular price; the
 * optional start and end dates pin down the promotional window. When
 * the relay omits the start and end dates, the sale is interpreted as
 * having no scheduled window (always-on).
 */
@ProtobufMessage(name = "BusinessProductSalePrice")
public final class BusinessProductSalePrice {
    /**
     * Discounted price in the same opaque string form WhatsApp uses
     * for the regular price (typically a major-units integer string).
     * Always populated — the relay never publishes a sale block
     * without a price.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String price;

    /**
     * ISO-8601 start date of the promotional window. Empty when the
     * relay omitted the window — the sale is then treated as
     * always-on.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String startDate;

    /**
     * ISO-8601 end date of the promotional window. Empty when the
     * relay omitted the window — the sale is then treated as
     * always-on.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String endDate;

    /**
     * Constructs a new sale-price block. The {@code price} argument is
     * required; the dates may be {@code null} when the relay omitted
     * the promotional window.
     *
     * @param price     the discounted price string; never {@code null}
     * @param startDate the optional start date, or {@code null}
     * @param endDate   the optional end date, or {@code null}
     * @throws NullPointerException if {@code price} is {@code null}
     */
    BusinessProductSalePrice(String price, String startDate, String endDate) {
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Returns the discounted price string.
     *
     * @return the price; never {@code null}
     */
    public String price() {
        return price;
    }

    /**
     * Returns the ISO-8601 start date of the promotional window.
     *
     * @return an {@code Optional} containing the start date, or empty
     *         when the relay omitted the window
     */
    public Optional<String> startDate() {
        return Optional.ofNullable(startDate);
    }

    /**
     * Returns the ISO-8601 end date of the promotional window.
     *
     * @return an {@code Optional} containing the end date, or empty
     *         when the relay omitted the window
     */
    public Optional<String> endDate() {
        return Optional.ofNullable(endDate);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessProductSalePrice) obj;
        return Objects.equals(this.price, that.price)
                && Objects.equals(this.startDate, that.startDate)
                && Objects.equals(this.endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, startDate, endDate);
    }

    @Override
    public String toString() {
        return "BusinessProductSalePrice[" +
                "price=" + price + ", " +
                "startDate=" + startDate + ", " +
                "endDate=" + endDate + ']';
    }
}
