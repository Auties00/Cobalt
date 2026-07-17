package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.OrderContent;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * The Cloud transport's inbound order content body.
 */
public final class CloudOrderContent implements OrderContent {
    /**
     * The accompanying message, or {@code null} when none.
     */
    private final String message;

    /**
     * The number of items, or {@code null} when unset.
     */
    private final Integer itemCount;

    /**
     * The total price in thousandths of the currency unit, or {@code null} when unset.
     */
    private final Long totalAmount1000;

    /**
     * The ISO currency code, or {@code null} when unset.
     */
    private final String totalCurrencyCode;

    /**
     * The id of the source order-request message, or {@code null} when unset.
     */
    private final String orderRequestMessageId;

    /**
     * Constructs a Cloud order body.
     *
     * @param message               the accompanying message, or {@code null} when none
     * @param itemCount             the number of items, or {@code null} when unset
     * @param totalAmount1000       the total price in thousandths of the currency unit, or {@code null} when unset
     * @param totalCurrencyCode     the ISO currency code, or {@code null} when unset
     * @param orderRequestMessageId the id of the source order-request message, or {@code null} when unset
     */
    public CloudOrderContent(String message, Integer itemCount, Long totalAmount1000, String totalCurrencyCode, String orderRequestMessageId) {
        this.message = message;
        this.itemCount = itemCount;
        this.totalAmount1000 = totalAmount1000;
        this.totalCurrencyCode = totalCurrencyCode;
        this.orderRequestMessageId = orderRequestMessageId;
    }

    @Override
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    @Override
    public OptionalInt itemCount() {
        return itemCount == null ? OptionalInt.empty() : OptionalInt.of(itemCount);
    }

    @Override
    public OptionalLong totalAmount1000() {
        return totalAmount1000 == null ? OptionalLong.empty() : OptionalLong.of(totalAmount1000);
    }

    @Override
    public Optional<String> totalCurrencyCode() {
        return Optional.ofNullable(totalCurrencyCode);
    }

    @Override
    public Optional<String> orderRequestMessageId() {
        return Optional.ofNullable(orderRequestMessageId);
    }
}
