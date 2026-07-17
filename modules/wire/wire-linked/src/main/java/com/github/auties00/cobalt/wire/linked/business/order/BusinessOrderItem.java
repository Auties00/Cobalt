package com.github.auties00.cobalt.wire.linked.business.order;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * A single line item belonging to a {@link BusinessOrder}.
 *
 * <p>Each line item describes one product entry in the order: the
 * server-issued catalog product identifier, the customer-facing display
 * name, the per-unit price (expressed in thousandths of the currency
 * unit, matching the WhatsApp Business pricing convention), the ISO
 * 4217 currency code, the ordered quantity, an optional product
 * thumbnail (id and remote URL pair) and the list of catalog variant
 * properties that pin down which variant was ordered.
 *
 * <p>Only the product identifier and display name are required; the
 * remaining scalar fields are nullable to mirror the WhatsApp Business
 * order detail relay's per-field nullability.
 */
@ProtobufMessage(name = "BusinessOrderItem")
public final class BusinessOrderItem {
    /**
     * The server-issued catalog product identifier this line refers to.
     * Always populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The customer-facing display name of the catalog product. Always
     * populated.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * The per-unit price expressed in thousandths of the currency unit
     * (e.g. {@code 12500} for {@code 12.500} of the unit). Empty when
     * the merchant relay omitted the price for this line.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    Long price;

    /**
     * The ISO 4217 currency code (for example {@code "USD"} or
     * {@code "EUR"}) that paired with {@link #price}. Empty when the
     * merchant relay omitted the currency.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String currency;

    /**
     * The number of units of this product ordered. Empty when the
     * merchant relay omitted the quantity.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    Integer quantity;

    /**
     * The opaque server identifier of the product thumbnail image. Empty
     * when the relay published no thumbnail for this line.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String thumbnailId;

    /**
     * The remote URL of the product thumbnail image. Empty when the
     * relay published no thumbnail for this line.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String thumbnailUrl;

    /**
     * The variant properties pinning down which catalog variant was
     * ordered (for example {@code [size: "L", colour: "blue"]}). Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    List<BusinessOrderItemProperty> properties;

    /**
     * Constructs a new {@code BusinessOrderItem} with the given field
     * values. The {@code id} and {@code name} arguments are required;
     * the remaining scalars may be {@code null} when the merchant relay
     * omitted them, and a {@code null} {@code properties} list is
     * coerced to an empty list.
     *
     * @param id           the catalog product identifier; never {@code null}
     * @param name         the product display name; never {@code null}
     * @param price        the per-unit price in thousandths, or {@code null}
     * @param currency     the ISO 4217 currency code, or {@code null}
     * @param quantity     the ordered quantity, or {@code null}
     * @param thumbnailId  the thumbnail identifier, or {@code null}
     * @param thumbnailUrl the thumbnail URL, or {@code null}
     * @param properties   the variant properties; {@code null} is treated as empty
     * @throws NullPointerException if {@code id} or {@code name} is {@code null}
     */
    BusinessOrderItem(String id,
                      String name,
                      Long price,
                      String currency,
                      Integer quantity,
                      String thumbnailId,
                      String thumbnailUrl,
                      List<BusinessOrderItemProperty> properties) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
        this.thumbnailId = thumbnailId;
        this.thumbnailUrl = thumbnailUrl;
        this.properties = properties == null ? List.of() : properties;
    }

    /**
     * Returns the server-issued catalog product identifier this line
     * refers to.
     *
     * @return the product id; never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the customer-facing display name of the catalog product.
     *
     * @return the product name; never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the per-unit price expressed in thousandths of the
     * currency unit.
     *
     * @return an {@code Optional} containing the price, or empty when
     *         the merchant relay omitted it
     */
    public Optional<Long> price() {
        return Optional.ofNullable(price);
    }

    /**
     * Returns the ISO 4217 currency code paired with the per-unit price.
     *
     * @return an {@code Optional} containing the currency, or empty when
     *         the merchant relay omitted it
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the number of units of this product ordered.
     *
     * @return an {@code Optional} containing the quantity, or empty when
     *         the merchant relay omitted it
     */
    public Optional<Integer> quantity() {
        return Optional.ofNullable(quantity);
    }

    /**
     * Returns the opaque server identifier of the product thumbnail image.
     *
     * @return an {@code Optional} containing the thumbnail id, or empty
     *         when the relay published no thumbnail
     */
    public Optional<String> thumbnailId() {
        return Optional.ofNullable(thumbnailId);
    }

    /**
     * Returns the remote URL of the product thumbnail image.
     *
     * @return an {@code Optional} containing the thumbnail URL, or empty
     *         when the relay published no thumbnail
     */
    public Optional<String> thumbnailUrl() {
        return Optional.ofNullable(thumbnailUrl);
    }

    /**
     * Returns the variant properties pinning down which catalog variant
     * was ordered.
     *
     * @return an unmodifiable view of the properties list; never
     *         {@code null}, possibly empty
     */
    public SequencedCollection<BusinessOrderItemProperty> properties() {
        return Collections.unmodifiableSequencedCollection(properties);
    }

    /**
     * Sets the server-issued catalog product identifier.
     *
     * @param id the product id to set
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public void setId(String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    /**
     * Sets the customer-facing display name of the catalog product.
     *
     * @param name the display name to set
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    /**
     * Sets the per-unit price expressed in thousandths of the currency
     * unit.
     *
     * @param price the price to set, or {@code null} to clear
     */
    public void setPrice(Long price) {
        this.price = price;
    }

    /**
     * Sets the ISO 4217 currency code paired with the per-unit price.
     *
     * @param currency the currency to set, or {@code null} to clear
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Sets the number of units of this product ordered.
     *
     * @param quantity the quantity to set, or {@code null} to clear
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * Sets the opaque server identifier of the product thumbnail image.
     *
     * @param thumbnailId the thumbnail id to set, or {@code null} to clear
     */
    public void setThumbnailId(String thumbnailId) {
        this.thumbnailId = thumbnailId;
    }

    /**
     * Sets the remote URL of the product thumbnail image.
     *
     * @param thumbnailUrl the URL to set, or {@code null} to clear
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Sets the variant properties pinning down which catalog variant
     * was ordered. A {@code null} list is coerced to an empty list.
     *
     * @param properties the properties to set, or {@code null} to clear
     */
    public void setProperties(List<BusinessOrderItemProperty> properties) {
        this.properties = properties == null ? List.of() : properties;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessOrderItem) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.price, that.price) &&
                Objects.equals(this.currency, that.currency) &&
                Objects.equals(this.quantity, that.quantity) &&
                Objects.equals(this.thumbnailId, that.thumbnailId) &&
                Objects.equals(this.thumbnailUrl, that.thumbnailUrl) &&
                Objects.equals(this.properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, currency, quantity,
                thumbnailId, thumbnailUrl, properties);
    }

    @Override
    public String toString() {
        return "BusinessOrderItem[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "price=" + price + ", " +
                "currency=" + currency + ", " +
                "quantity=" + quantity + ", " +
                "thumbnailId=" + thumbnailId + ", " +
                "thumbnailUrl=" + thumbnailUrl + ", " +
                "properties=" + properties + ']';
    }
}
