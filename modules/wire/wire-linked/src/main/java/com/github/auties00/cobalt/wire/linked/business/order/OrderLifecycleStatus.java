package com.github.auties00.cobalt.wire.linked.business.order;

/**
 * The lifecycle status of a business order, as carried in the {@code order.status} field of the
 * order-status native-flow payload a merchant sends to a customer.
 *
 * <p>Each constant maps to the lowercase wire token WhatsApp uses inside the order-status JSON; the
 * token is exposed through {@link #value()}.
 */
public enum OrderLifecycleStatus {
    /**
     * The order is awaiting merchant action.
     */
    PENDING("pending"),
    /**
     * The merchant is processing the order.
     */
    PROCESSING("processing"),
    /**
     * Part of the order has shipped.
     */
    PARTIALLY_SHIPPED("partially_shipped"),
    /**
     * The order has shipped in full.
     */
    SHIPPED("shipped"),
    /**
     * The order is complete.
     */
    COMPLETED("completed"),
    /**
     * The order was canceled.
     */
    CANCELED("canceled"),
    /**
     * Payment has been requested from the customer.
     */
    PAYMENT_REQUESTED("payment_requested"),
    /**
     * The merchant is preparing the order for shipment.
     */
    PREPARING_TO_SHIP("preparing_to_ship"),
    /**
     * The order has been delivered.
     */
    DELIVERED("delivered"),
    /**
     * The order has been confirmed by the merchant.
     */
    CONFIRMED("confirmed"),
    /**
     * The order is delayed.
     */
    DELAYED("delayed"),
    /**
     * The order is out for delivery.
     */
    OUT_FOR_DELIVERY("out_for_delivery"),
    /**
     * The order failed.
     */
    FAILED("failed"),
    /**
     * The order was refunded.
     */
    REFUNDED("refunded");

    /**
     * The lowercase wire token used inside the order-status JSON.
     */
    private final String value;

    /**
     * Constructs a constant bound to its wire token.
     *
     * @param value the lowercase wire token
     */
    OrderLifecycleStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the lowercase wire token used inside the order-status JSON.
     *
     * @return the wire token
     */
    public String value() {
        return value;
    }
}
