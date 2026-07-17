package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.Objects;
import java.util.Optional;

/**
 * A single line item of a Cloud API payment order.
 *
 * <p>Each item references a catalog product by its retailer id and carries the display name, unit
 * amount, ordered quantity, and an optional discounted sale amount.
 */
public final class CloudOrderItem {
    /**
     * The catalog retailer id of the product.
     */
    private final String retailerId;

    /**
     * The display name of the item.
     */
    private final String name;

    /**
     * The unit amount of the item.
     */
    private final CloudOrderAmount amount;

    /**
     * The ordered quantity.
     */
    private final int quantity;

    /**
     * The discounted sale amount, or {@code null} when the item is not on sale.
     */
    private final CloudOrderAmount saleAmount;

    /**
     * Constructs a new order item.
     *
     * @param retailerId the catalog retailer id
     * @param name       the display name
     * @param amount     the unit amount
     * @param quantity   the ordered quantity
     * @param saleAmount the discounted sale amount, or {@code null} when not on sale
     * @throws NullPointerException if {@code retailerId}, {@code name}, or {@code amount} is
     *                              {@code null}
     */
    public CloudOrderItem(String retailerId, String name, CloudOrderAmount amount, int quantity,
                          CloudOrderAmount saleAmount) {
        this.retailerId = Objects.requireNonNull(retailerId, "retailerId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.quantity = quantity;
        this.saleAmount = saleAmount;
    }

    /**
     * Returns the catalog retailer id of the product.
     *
     * @return the retailer id
     */
    public String retailerId() {
        return retailerId;
    }

    /**
     * Returns the display name of the item.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the unit amount of the item.
     *
     * @return the amount
     */
    public CloudOrderAmount amount() {
        return amount;
    }

    /**
     * Returns the ordered quantity.
     *
     * @return the quantity
     */
    public int quantity() {
        return quantity;
    }

    /**
     * Returns the discounted sale amount.
     *
     * @return an {@link Optional} carrying the sale amount, or empty when the item is not on sale
     */
    public Optional<CloudOrderAmount> saleAmount() {
        return Optional.ofNullable(saleAmount);
    }
}
