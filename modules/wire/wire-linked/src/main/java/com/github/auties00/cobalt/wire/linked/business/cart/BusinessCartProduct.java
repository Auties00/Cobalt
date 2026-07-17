package com.github.auties00.cobalt.wire.linked.business.cart;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Single line item in a WhatsApp Business shopping cart, representing one
 * catalogue product the customer has added.
 *
 * <p>Whenever the customer reopens an in-chat cart, the merchant's server
 * rebuilds every line by joining the customer's selected product
 * identifiers against the latest catalogue state, then returns the
 * refreshed entries on a {@link BusinessRefreshedCart}. Each entry
 * carries the stable product identifier, the freshly resolved name and
 * price, optional thumbnail media, the per-line quantity cap, an optional
 * promotional sale price, and an optional server status (for instance to
 * mark a product as sold out without removing it from the cart).
 *
 * <p>The {@link #id()} field is always populated; every other field is
 * optional and reflects whether the merchant configured the corresponding
 * attribute on the catalogue product.
 */
@ProtobufMessage
public final class BusinessCartProduct {
    /**
     * Stable catalogue identifier for this product. The client uses this to
     * correlate the line with its locally tracked cart entry across refreshes
     * and to issue follow-up updates such as quantity changes. Always present.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * Display name of the catalogue product as configured by the merchant.
     * Absent when the merchant has not set a name on the catalogue entry.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * Per-unit price as a stringified decimal amount in the cart's currency,
     * for example {@code "4.99"}. Absent when the merchant has hidden pricing
     * or not configured a price for this catalogue item. When a sale is
     * active, this remains the regular price and {@link #salePrice()} carries
     * the discounted amount.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String price;

    /**
     * ISO 4217 currency code applied to {@link #price()} (for example
     * {@code "USD"}). Absent when no per-line price is present.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String currency;

    /**
     * Visual media associated with the catalogue product, currently a
     * thumbnail image rendered next to the line in the cart drawer. Absent
     * when the merchant has not attached any media to the catalogue entry.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    BusinessCartProductMedia media;

    /**
     * Maximum quantity the customer may add for this single line, enforced
     * by the merchant's catalogue stock or per-item purchase limits.
     * Defaults to {@code 99} (the cart-wide item ceiling) when the merchant
     * does not configure a tighter cap, in which case callers should treat
     * the value as "no per-item limit beyond the global maximum".
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT32)
    int maxAvailable;

    /**
     * Promotional sale price attached when the merchant has configured a
     * temporary discount on this product. Carries the discounted amount and
     * the optional sale window. Absent when the product is not currently
     * on sale.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    BusinessCartProductSalePrice salePrice;

    /**
     * Server-side status string overlaid on the line when the catalogue
     * product is in a special state, for example {@code "SOLD_OUT"} for
     * out-of-stock items the customer should still see in the cart. The
     * client renders the status verbatim. Absent when the line is in its
     * normal purchasable state.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String status;

    /**
     * Constructs a new {@code BusinessCartProduct} with the specified product
     * fields. Only {@link #id} is required; the remaining fields are optional
     * and may be {@code null} when the merchant has not configured the
     * corresponding catalogue attribute.
     *
     * @param id           the stable catalogue identifier; never {@code null}
     * @param name         the display name, or {@code null}
     * @param price        the per-unit price string, or {@code null}
     * @param currency     the ISO 4217 currency code, or {@code null}
     * @param media        the product media, or {@code null}
     * @param maxAvailable the per-line quantity cap; expected non-negative,
     *                     defaults to {@code 99} when the merchant does not
     *                     configure a tighter limit
     * @param salePrice    the promotional sale price, or {@code null}
     * @param status       the server-side status string, or {@code null}
     */
    BusinessCartProduct(String id,
                        String name,
                        String price,
                        String currency,
                        BusinessCartProductMedia media,
                        int maxAvailable,
                        BusinessCartProductSalePrice salePrice,
                        String status) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.media = media;
        this.maxAvailable = maxAvailable;
        this.salePrice = salePrice;
        this.status = status;
    }

    /**
     * Returns the stable catalogue identifier the client uses to correlate
     * this line with its locally tracked cart entry across refreshes.
     *
     * @return the product identifier; never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the display name of the catalogue product as configured by the
     * merchant.
     *
     * @return an {@code Optional} containing the name, or empty if the merchant
     *         did not set a name
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the per-unit price as a stringified decimal amount in the cart's
     * currency.
     *
     * @return an {@code Optional} containing the price string, or empty if no
     *         price is configured
     */
    public Optional<String> price() {
        return Optional.ofNullable(price);
    }

    /**
     * Returns the ISO 4217 currency code applied to {@link #price()}.
     *
     * @return an {@code Optional} containing the currency code, or empty if no
     *         per-line price is present
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the visual media associated with the catalogue product, currently
     * a thumbnail image rendered next to the line in the cart drawer.
     *
     * @return an {@code Optional} containing the media block, or empty if the
     *         merchant did not attach any media
     */
    public Optional<BusinessCartProductMedia> media() {
        return Optional.ofNullable(media);
    }

    /**
     * Returns the maximum quantity the customer may add for this single line,
     * defaulting to {@code 99} when the merchant does not configure a tighter
     * cap.
     *
     * @return the per-line quantity cap
     */
    public int maxAvailable() {
        return maxAvailable;
    }

    /**
     * Returns the promotional sale price attached when the merchant has
     * configured a temporary discount on this product.
     *
     * @return an {@code Optional} containing the sale-price block, or empty if
     *         the product is not on sale
     */
    public Optional<BusinessCartProductSalePrice> salePrice() {
        return Optional.ofNullable(salePrice);
    }

    /**
     * Returns the server-side status string overlaid on the line when the
     * catalogue product is in a special state (for example
     * {@code "SOLD_OUT"}).
     *
     * @return an {@code Optional} containing the status string, or empty if
     *         the line is in its normal purchasable state
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Sets the stable catalogue identifier for this product line.
     *
     * @param id the identifier to set; never {@code null}
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public void setId(String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    /**
     * Sets the display name of the catalogue product.
     *
     * @param name the name to set, or {@code null} to clear
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the per-unit price as a stringified decimal amount.
     *
     * @param price the price string to set, or {@code null} to clear
     */
    public void setPrice(String price) {
        this.price = price;
    }

    /**
     * Sets the ISO 4217 currency code applied to the per-unit price.
     *
     * @param currency the currency code to set, or {@code null} to clear
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Sets the visual media associated with the catalogue product.
     *
     * @param media the media block to set, or {@code null} to clear
     */
    public void setMedia(BusinessCartProductMedia media) {
        this.media = media;
    }

    /**
     * Sets the maximum quantity the customer may add for this single line.
     *
     * @param maxAvailable the per-line quantity cap to set
     */
    public void setMaxAvailable(int maxAvailable) {
        this.maxAvailable = maxAvailable;
    }

    /**
     * Sets the promotional sale-price block.
     *
     * @param salePrice the sale-price block to set, or {@code null} to clear
     */
    public void setSalePrice(BusinessCartProductSalePrice salePrice) {
        this.salePrice = salePrice;
    }

    /**
     * Sets the server-side status string overlaid on the line.
     *
     * @param status the status string to set, or {@code null} to clear
     */
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCartProduct) obj;
        return this.maxAvailable == that.maxAvailable &&
               Objects.equals(this.id, that.id) &&
               Objects.equals(this.name, that.name) &&
               Objects.equals(this.price, that.price) &&
               Objects.equals(this.currency, that.currency) &&
               Objects.equals(this.media, that.media) &&
               Objects.equals(this.salePrice, that.salePrice) &&
               Objects.equals(this.status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, currency, media, maxAvailable, salePrice, status);
    }

    @Override
    public String toString() {
        return "BusinessCartProduct[" +
               "id=" + id + ", " +
               "name=" + name + ", " +
               "price=" + price + ", " +
               "currency=" + currency + ", " +
               "media=" + media + ", " +
               "maxAvailable=" + maxAvailable + ", " +
               "salePrice=" + salePrice + ", " +
               "status=" + status + ']';
    }
}
