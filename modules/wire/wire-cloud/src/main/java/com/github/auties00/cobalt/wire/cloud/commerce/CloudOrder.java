package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The order payload of a Cloud API {@code order_details} payment message.
 *
 * <p>This model carries the catalog reference, the line items, and the monetary breakdown (subtotal
 * and the optional tax, shipping, and discount) that the consumer reviews before paying. The subtotal
 * is the sum of the item amounts; each optional adjustment carries an amount and an optional
 * description grouped under {@link CloudOrderAdjustment}.
 */
public final class CloudOrder {
    /**
     * The embedded order status, conventionally {@link CloudOrderStatus#PENDING} for a new order.
     */
    private final CloudOrderStatus status;

    /**
     * The catalog id the items belong to, or {@code null} when omitted.
     */
    private final String catalogId;

    /**
     * The line items of the order.
     */
    private final List<CloudOrderItem> items;

    /**
     * The order subtotal.
     */
    private final CloudOrderAmount subtotal;

    /**
     * The tax adjustment, or {@code null} when not charged.
     */
    private final CloudOrderAdjustment tax;

    /**
     * The shipping adjustment, or {@code null} when not charged.
     */
    private final CloudOrderAdjustment shipping;

    /**
     * The discount adjustment, or {@code null} when none applies.
     */
    private final CloudOrderAdjustment discount;

    /**
     * Constructs a new order.
     *
     * @param status    the embedded order status
     * @param catalogId the catalog id, or {@code null}
     * @param items     the line items
     * @param subtotal  the order subtotal
     * @param tax       the tax adjustment, or {@code null}
     * @param shipping  the shipping adjustment, or {@code null}
     * @param discount  the discount adjustment, or {@code null}
     * @throws NullPointerException if {@code status}, {@code items}, or {@code subtotal} is
     *                              {@code null}
     */
    public CloudOrder(CloudOrderStatus status, String catalogId, List<CloudOrderItem> items, CloudOrderAmount subtotal,
                      CloudOrderAdjustment tax, CloudOrderAdjustment shipping, CloudOrderAdjustment discount) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.catalogId = catalogId;
        this.items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        this.subtotal = Objects.requireNonNull(subtotal, "subtotal must not be null");
        this.tax = tax;
        this.shipping = shipping;
        this.discount = discount;
    }

    /**
     * Returns the embedded order status.
     *
     * @return the {@link CloudOrderStatus}
     */
    public CloudOrderStatus status() {
        return status;
    }

    /**
     * Returns the catalog id the items belong to.
     *
     * @return an {@link Optional} carrying the catalog id, or empty when omitted
     */
    public Optional<String> catalogId() {
        return Optional.ofNullable(catalogId);
    }

    /**
     * Returns the line items of the order.
     *
     * @return an unmodifiable list of items
     */
    public List<CloudOrderItem> items() {
        return items;
    }

    /**
     * Returns the order subtotal.
     *
     * @return the subtotal
     */
    public CloudOrderAmount subtotal() {
        return subtotal;
    }

    /**
     * Returns the tax adjustment.
     *
     * @return an {@link Optional} carrying the tax {@link CloudOrderAdjustment}, or empty when not
     *         charged
     */
    public Optional<CloudOrderAdjustment> tax() {
        return Optional.ofNullable(tax);
    }

    /**
     * Returns the shipping adjustment.
     *
     * @return an {@link Optional} carrying the shipping {@link CloudOrderAdjustment}, or empty when not
     *         charged
     */
    public Optional<CloudOrderAdjustment> shipping() {
        return Optional.ofNullable(shipping);
    }

    /**
     * Returns the discount adjustment.
     *
     * @return an {@link Optional} carrying the discount {@link CloudOrderAdjustment}, or empty when none
     *         applies
     */
    public Optional<CloudOrderAdjustment> discount() {
        return Optional.ofNullable(discount);
    }

    /**
     * A monetary adjustment of a {@link CloudOrder}, such as tax, shipping, or a discount.
     *
     * <p>An adjustment pairs an amount with an optional human-readable description shown to the
     * consumer alongside the line items.
     */
    public static final class CloudOrderAdjustment {
        /**
         * The adjustment amount.
         */
        private final CloudOrderAmount amount;

        /**
         * The adjustment description, or {@code null} when none.
         */
        private final String description;

        /**
         * Constructs a new order adjustment.
         *
         * @param amount      the adjustment amount
         * @param description the adjustment description, or {@code null} when none
         * @throws NullPointerException if {@code amount} is {@code null}
         */
        public CloudOrderAdjustment(CloudOrderAmount amount, String description) {
            this.amount = Objects.requireNonNull(amount, "amount must not be null");
            this.description = description;
        }

        /**
         * Returns the adjustment amount.
         *
         * @return the amount
         */
        public CloudOrderAmount amount() {
            return amount;
        }

        /**
         * Returns the adjustment description.
         *
         * @return an {@link Optional} carrying the description, or empty when none
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }
    }
}
