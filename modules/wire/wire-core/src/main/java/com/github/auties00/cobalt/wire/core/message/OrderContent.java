package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Transport-agnostic contract for an inbound order message body.
 *
 * <p>Carries the order summary a Cloud webhook delivers: an accompanying message, the item count, the
 * total price in thousandths of the currency unit, the currency code, and the id of the source order
 * request. The Cloud webhook additionally carries a catalog id and per-line-item details; those are
 * exposed only by the Cloud-specific order type, not by this shared contract.
 */
public interface OrderContent extends MessageContent {
    /**
     * Returns the message accompanying the order, when present.
     *
     * @return an {@link Optional} holding the message, or empty when none
     */
    Optional<String> message();

    /**
     * Returns the number of items in the order.
     *
     * @return the item count, or empty when unset
     */
    OptionalInt itemCount();

    /**
     * Returns the total order price in thousandths of the currency unit.
     *
     * @return the total amount, or empty when unset
     */
    OptionalLong totalAmount1000();

    /**
     * Returns the ISO currency code of the order total.
     *
     * @return an {@link Optional} holding the currency code, or empty when unset
     */
    Optional<String> totalCurrencyCode();

    /**
     * Returns the id of the order-request message this order responds to.
     *
     * @return an {@link Optional} holding the request message id, or empty when unset
     */
    Optional<String> orderRequestMessageId();
}
