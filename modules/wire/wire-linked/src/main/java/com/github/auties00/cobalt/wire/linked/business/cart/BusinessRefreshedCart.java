package com.github.auties00.cobalt.wire.linked.business.cart;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * Server-rebuilt snapshot of a WhatsApp Business in-chat shopping cart
 * returned to the customer after a refresh request.
 *
 * <p>WhatsApp Business merchants who run a catalogue let customers browse
 * the catalogue inside a chat, drop products into a cart, and then send
 * the cart to the merchant to start a check-out conversation. The client
 * does not own the cart state in isolation: every time the customer
 * reopens the cart, it asks the merchant's server to rebuild the cart by
 * joining the locally stored product identifiers against the latest
 * catalogue, refreshing names, prices, media, sale promotions and stock
 * status. The reply is modelled by this class.
 *
 * <p>The reply pairs a cart-wide {@link BusinessCartPrice} summary with
 * the freshly rebuilt list of {@link BusinessCartProduct} entries, in
 * the order the server emits them. The product order is significant
 * because clients render the cart drawer in that traversal order; the
 * exposed list is unmodifiable so callers cannot accidentally mutate
 * the server response.
 */
@ProtobufMessage
public final class BusinessRefreshedCart {
    /**
     * Cart-wide pricing summary aggregating the line-item prices into a
     * subtotal and total in the merchant's currency, or carrying a
     * price-status discriminator when pricing is hidden. Always present.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    BusinessCartPrice price;

    /**
     * Per-line product entries in the order the merchant's server emits
     * them. The traversal order matches the order in which the cart
     * drawer renders the lines on the client. Defaults to an empty list
     * when the customer's cart contains no products.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<BusinessCartProduct> products;

    /**
     * Constructs a new {@code BusinessRefreshedCart} pairing the cart-wide
     * pricing summary with the rebuilt product list. The price summary is
     * required; a {@code null} product list is treated as an empty list.
     *
     * @param price    the cart-wide pricing summary; never {@code null}
     * @param products the per-line product entries in server order, or
     *                 {@code null} to indicate an empty cart
     */
    BusinessRefreshedCart(BusinessCartPrice price, List<BusinessCartProduct> products) {
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.products = products == null ? List.of() : products;
    }

    /**
     * Returns the cart-wide pricing summary aggregating line-item prices into
     * a subtotal and total in the merchant's currency.
     *
     * @return the price summary; never {@code null}
     */
    public BusinessCartPrice price() {
        return price;
    }

    /**
     * Returns the per-line product entries in the order the merchant's server
     * emits them, matching the traversal order used by the cart drawer.
     *
     * @return an unmodifiable view of the products; never {@code null},
     *         possibly empty
     */
    public List<BusinessCartProduct> products() {
        return products;
    }

    /**
     * Sets the cart-wide pricing summary.
     *
     * @param price the price summary to set; never {@code null}
     * @throws NullPointerException if {@code price} is {@code null}
     */
    public void setPrice(BusinessCartPrice price) {
        this.price = Objects.requireNonNull(price, "price cannot be null");
    }

    /**
     * Sets the per-line product entries in the order they should be rendered.
     *
     * @param products the products to set, or {@code null} to clear to an
     *                 empty list
     */
    public void setProducts(List<BusinessCartProduct> products) {
        this.products = products == null ? List.of() : products;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessRefreshedCart) obj;
        return Objects.equals(this.price, that.price) &&
               Objects.equals(this.products, that.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, products);
    }

    @Override
    public String toString() {
        return "BusinessRefreshedCart[" +
               "price=" + price + ", " +
               "products=" + products + ']';
    }
}
