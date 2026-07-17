package com.github.auties00.cobalt.wire.linked.business.order;

/**
 * The payment status of a business order, as carried in the {@code payment_status} field of the
 * order-status and payment-status native-flow payloads a merchant sends to a customer.
 *
 * <p>Each constant maps to the lowercase wire token WhatsApp uses inside the order JSON; the token
 * is exposed through {@link #value()}.
 */
public enum OrderPaymentStatus {
    /**
     * Payment has not yet been captured.
     */
    PENDING("pending"),
    /**
     * Payment has been captured; the order is paid.
     */
    CAPTURED("captured"),
    /**
     * Payment failed.
     */
    FAILED("failed");

    /**
     * The lowercase wire token used inside the order JSON.
     */
    private final String value;

    /**
     * Constructs a constant bound to its wire token.
     *
     * @param value the lowercase wire token
     */
    OrderPaymentStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the lowercase wire token used inside the order JSON.
     *
     * @return the wire token
     */
    public String value() {
        return value;
    }

    /**
     * Derives the payment status implied by an order lifecycle status, mirroring WhatsApp's mapping:
     * a {@link OrderLifecycleStatus#COMPLETED} order is {@link #CAPTURED}, a
     * {@link OrderLifecycleStatus#CANCELED} or {@link OrderLifecycleStatus#FAILED} order is
     * {@link #FAILED}, and every other status is {@link #PENDING}.
     *
     * @param status the order lifecycle status
     * @return the implied payment status
     */
    public static OrderPaymentStatus fromLifecycle(OrderLifecycleStatus status) {
        return switch (status) {
            case COMPLETED -> CAPTURED;
            case CANCELED, FAILED -> FAILED;
            default -> PENDING;
        };
    }
}
