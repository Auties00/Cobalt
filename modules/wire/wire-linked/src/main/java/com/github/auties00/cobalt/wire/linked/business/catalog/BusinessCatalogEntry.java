package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Optional;

/**
 * Represents a single product listed in a WhatsApp Business catalog.
 *
 * <p>Business accounts on WhatsApp can maintain a catalog of products that
 * customers can browse directly within the chat interface. Each product
 * entry carries the core metadata needed to display and identify the item:
 * a unique identifier, a display name, a textual description, pricing
 * information, an image, and stock availability.
 *
 * <p>Product entries can be organized into {@link BusinessCatalog}
 * collections and shared in conversations as product messages. The
 * {@linkplain #reviewStatus() review status} indicates whether the product
 * has passed WhatsApp's compliance review and is visible to customers,
 * while the {@linkplain #hidden() hidden flag} allows business owners to
 * manually hide a product without deleting it.
 *
 * <p>Pricing is expressed as a long value in thousandths of the base
 * currency unit (for example, {@code 1500000} with currency {@code "USD"}
 * means $1,500.00). The currency is specified as an ISO 4217 code.
 */
@ProtobufMessage
public final class BusinessCatalogEntry {
    /**
     * The unique identifier for this product, assigned by the WhatsApp
     * catalog backend.
     *
     * <p>This corresponds to the {@code productId} field in the protobuf
     * {@code ProductSnapshot} structure used in product messages.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The URI of this product's primary image, hosted on the WhatsApp
     * media CDN.
     *
     * <p>Product images are encrypted and served through CDN URLs. This
     * field holds the reference to the main product image that is displayed
     * as the product thumbnail in the catalog and in product messages.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    URI encryptedImage;

    /**
     * The compliance review status of this product.
     *
     * <p>Products submitted to a WhatsApp Business catalog undergo a
     * compliance review before they become visible to customers. This field
     * indicates whether the product has been approved, is still pending
     * review, has been rejected, or has not yet been submitted for review.
     *
     * @see BusinessReviewStatus
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    BusinessReviewStatus reviewStatus;

    /**
     * The stock availability of this product.
     *
     * <p>Indicates whether the product is currently in stock and available
     * for purchase, out of stock, or in an unknown availability state.
     * Products that are out of stock are shown differently in the catalog
     * UI and cannot be added to a cart.
     *
     * @see BusinessItemAvailability
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    BusinessItemAvailability availability;

    /**
     * The display name of this product as shown to customers in the
     * catalog and in product messages.
     *
     * <p>This corresponds to the {@code title} field in the protobuf
     * {@code ProductSnapshot} and the {@code name} field in catalog
     * query responses.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String name;

    /**
     * The retailer-assigned identifier for this product.
     *
     * <p>This is a merchant-defined SKU or reference code that the business
     * owner assigns to the product for their own inventory tracking. It
     * corresponds to the {@code retailer_id} field in catalog responses and
     * the {@code retailerId} field in the protobuf {@code ProductSnapshot}.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String sellerId;

    /**
     * The shareable URI for this product's page.
     *
     * <p>This link can be sent to customers or shared externally. It
     * corresponds to the {@code url} field in the protobuf
     * {@code ProductSnapshot} and in catalog query responses.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    URI uri;

    /**
     * The description text for this product, providing additional details
     * about the item beyond its name.
     *
     * <p>This free-form text is set by the business owner and displayed
     * in the product detail view within the catalog.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String description;

    /**
     * The price of this product in thousandths of the base currency unit.
     *
     * <p>For example, a value of {@code 1500000} with a
     * {@linkplain #currency() currency} of {@code "USD"} represents
     * $1,500.00. A value of {@code 0} typically means the price has not
     * been set. This corresponds to the {@code priceAmount1000} field in
     * the protobuf {@code ProductSnapshot}.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64)
    long price;

    /**
     * The ISO 4217 currency code for this product's price, such as
     * {@code "USD"}, {@code "EUR"}, or {@code "BRL"}.
     *
     * <p>This determines how the {@linkplain #price() price} value is
     * formatted and displayed to customers. It corresponds to the
     * {@code currencyCode} field in the protobuf {@code ProductSnapshot}.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String currency;

    /**
     * Whether this product is hidden from customers.
     *
     * <p>Hidden products remain in the catalog database but are not
     * displayed in the storefront. Business owners can hide products
     * temporarily without deleting them, for example to take seasonal
     * items offline. A value of {@code true} means the product is not
     * visible to customers; {@code false} means it is visible (subject
     * to passing compliance review).
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    boolean hidden;

    /**
     * Constructs a new {@code BusinessCatalogEntry} with the specified
     * product metadata.
     *
     * @param id              the unique product identifier, or {@code null}
     *                        if not yet assigned
     * @param encryptedImage  the URI of the product's primary CDN image,
     *                        or {@code null} if no image is available
     * @param reviewStatus    the compliance review status, or {@code null}
     *                        if not yet reviewed
     * @param availability    the stock availability, or {@code null} if
     *                        unknown
     * @param name            the display name of the product, or
     *                        {@code null} if not set
     * @param sellerId        the retailer-assigned identifier, or
     *                        {@code null} if not set
     * @param uri             the shareable product page URI, or
     *                        {@code null} if not available
     * @param description     the product description text, or {@code null}
     *                        if not set
     * @param price           the price in thousandths of the base currency
     *                        unit
     * @param currency        the ISO 4217 currency code, or {@code null}
     *                        if not set
     * @param hidden          {@code true} if the product is hidden from
     *                        customers, {@code false} otherwise
     */
    BusinessCatalogEntry(String id, URI encryptedImage, BusinessReviewStatus reviewStatus, BusinessItemAvailability availability, String name, String sellerId, URI uri, String description, long price, String currency, boolean hidden) {
        this.id = id;
        this.encryptedImage = encryptedImage;
        this.reviewStatus = reviewStatus;
        this.availability = availability;
        this.name = name;
        this.sellerId = sellerId;
        this.uri = uri;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.hidden = hidden;
    }

    /**
     * Returns the unique identifier for this product, assigned by the
     * WhatsApp catalog backend.
     *
     * @return an {@code Optional} containing the product identifier, or
     *         an empty {@code Optional} if the identifier has not been
     *         assigned
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the URI of this product's primary image on the WhatsApp
     * media CDN.
     *
     * @return an {@code Optional} containing the encrypted image URI, or
     *         an empty {@code Optional} if no image is available
     */
    public Optional<URI> encryptedImage() {
        return Optional.ofNullable(encryptedImage);
    }

    /**
     * Returns the compliance review status of this product.
     *
     * @return an {@code Optional} containing the
     *         {@link BusinessReviewStatus}, or an empty {@code Optional}
     *         if the review status has not been set
     */
    public Optional<BusinessReviewStatus> reviewStatus() {
        return Optional.ofNullable(reviewStatus);
    }

    /**
     * Returns the stock availability of this product.
     *
     * @return an {@code Optional} containing the
     *         {@link BusinessItemAvailability}, or an empty
     *         {@code Optional} if the availability has not been set
     */
    public Optional<BusinessItemAvailability> availability() {
        return Optional.ofNullable(availability);
    }

    /**
     * Returns the display name of this product.
     *
     * @return an {@code Optional} containing the product name, or an
     *         empty {@code Optional} if the name has not been set
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the retailer-assigned identifier for this product.
     *
     * @return an {@code Optional} containing the seller identifier, or
     *         an empty {@code Optional} if the identifier has not been set
     */
    public Optional<String> sellerId() {
        return Optional.ofNullable(sellerId);
    }

    /**
     * Returns the shareable URI for this product's page.
     *
     * @return an {@code Optional} containing the product URI, or an
     *         empty {@code Optional} if no URI is available
     */
    public Optional<URI> uri() {
        return Optional.ofNullable(uri);
    }

    /**
     * Returns the description text for this product.
     *
     * @return an {@code Optional} containing the product description, or
     *         an empty {@code Optional} if no description has been set
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the price of this product in thousandths of the base
     * currency unit.
     *
     * <p>For example, a value of {@code 1500000} with a
     * {@linkplain #currency() currency} of {@code "USD"} represents
     * $1,500.00.
     *
     * @return the price in thousandths of the base currency unit, or
     *         {@code 0} if the price has not been set
     */
    public long price() {
        return price;
    }

    /**
     * Returns the ISO 4217 currency code for this product's price.
     *
     * @return an {@code Optional} containing the currency code (such as
     *         {@code "USD"} or {@code "EUR"}), or an empty
     *         {@code Optional} if the currency has not been set
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns whether this product is hidden from customers.
     *
     * <p>Hidden products remain stored in the catalog but are not displayed
     * in the storefront. A product must be both non-hidden and approved by
     * compliance review to be visible.
     *
     * @return {@code true} if the product is hidden, {@code false}
     *         otherwise
     */
    public boolean hidden() {
        return hidden;
    }

    /**
     * Sets the unique product identifier.
     *
     * @param id the product identifier to set, or {@code null} to clear
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the URI of this product's primary CDN image.
     *
     * @param encryptedImage the encrypted image URI to set, or
     *                       {@code null} to clear
     */
    public void setEncryptedImage(URI encryptedImage) {
        this.encryptedImage = encryptedImage;
    }

    /**
     * Sets the compliance review status of this product.
     *
     * @param reviewStatus the review status to set, or {@code null} to
     *                     clear
     */
    public void setReviewStatus(BusinessReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    /**
     * Sets the stock availability of this product.
     *
     * @param availability the availability to set, or {@code null} to
     *                     clear
     */
    public void setAvailability(BusinessItemAvailability availability) {
        this.availability = availability;
    }

    /**
     * Sets the display name of this product.
     *
     * @param name the product name to set, or {@code null} to clear
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the retailer-assigned identifier for this product.
     *
     * @param sellerId the seller identifier to set, or {@code null} to
     *                 clear
     */
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    /**
     * Sets the shareable URI for this product's page.
     *
     * @param uri the product URI to set, or {@code null} to clear
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Sets the description text for this product.
     *
     * @param description the product description to set, or {@code null}
     *                    to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the price of this product in thousandths of the base currency
     * unit.
     *
     * @param price the price to set, in thousandths of the base currency
     *              unit
     */
    public void setPrice(long price) {
        this.price = price;
    }

    /**
     * Sets the ISO 4217 currency code for this product's price.
     *
     * @param currency the currency code to set (such as {@code "USD"} or
     *                 {@code "EUR"}), or {@code null} to clear
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Sets whether this product is hidden from customers.
     *
     * @param hidden {@code true} to hide the product from the storefront,
     *               {@code false} to make it visible
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
