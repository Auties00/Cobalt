package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Caller-friendly projection of a single product entry returned by the
 * WhatsApp Business product-list catalog query.
 *
 * <p>WhatsApp Business merchants publish their goods in a server-side
 * catalog tied to the merchant JID. Customers (or merchant-side admin
 * tools) can query the catalog by product id to obtain the full
 * description: human name, marketing copy, pricing, currency, stock
 * availability, image and video assets, optional sale-price block,
 * country-of-origin compliance disclosure, and the moderation outcome
 * the WhatsApp policy team last assigned to the product.
 *
 * <p>The relay can also surface a synthetic {@code "INVALID_PRODUCT"}
 * marker for ids that do not resolve to a live catalog entry — those
 * entries set {@link #invalid()} to {@code true} and leave the rest of
 * the fields empty so callers can render a placeholder. Each scalar
 * field is independently optional to mirror the per-field nullability
 * of the underlying wire schema.
 */
@ProtobufMessage(name = "BusinessProduct")
public final class BusinessProduct {
    /**
     * Server-issued catalog product identifier this entry refers to.
     * Always populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Whether the relay marked this entry as the synthetic
     * {@code "INVALID_PRODUCT"} placeholder. When {@code true} every
     * other field below is unset and callers should render a "product
     * unavailable" placeholder rather than the full detail card.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean invalid;

    /**
     * Customer-facing display name of the product. Empty when the
     * relay omitted the name (typically only happens for invalid
     * entries).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String name;

    /**
     * Free-form marketing copy describing the product. Empty when the
     * merchant did not configure a description.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String description;

    /**
     * External URL of the product page on the merchant's storefront.
     * Empty when the merchant did not configure a canonical URL.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final URI uri;

    /**
     * Retailer-side identifier (SKU, MPN, ...) the merchant uses
     * internally to refer to the product. Empty when the merchant did
     * not publish a retailer id.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String retailerId;

    /**
     * Stock availability of the product. Empty when the relay did not
     * publish an availability marker.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    final BusinessItemAvailability availability;

    /**
     * Maximum quantity a single customer can add to the cart. Defaults
     * to the WhatsApp catalog cart-item cap (99) when the relay omits
     * the per-product override.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT32)
    final int maxAvailable;

    /**
     * ISO 4217 currency code paired with {@link #price}. Empty when
     * the merchant did not configure pricing.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String currency;

    /**
     * Regular price as an opaque major-units string (e.g. "1499.99").
     * Empty when the merchant did not configure pricing.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final String price;

    /**
     * Whether the product is hidden from the merchant's storefront
     * grid. Hidden products remain queryable by id but do not appear
     * in catalog browsing.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    final boolean hidden;

    /**
     * Whether the product was sanctioned by Meta enforcement. Customer
     * surfaces typically replace sanctioned products with a takedown
     * placeholder.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    final boolean sanctioned;

    /**
     * Whether the product is part of a featured/checkmarked set
     * advertised on the merchant's storefront.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    final boolean checkmark;

    /**
     * WhatsApp policy moderation outcome (for example {@code "APPROVED"},
     * {@code "PENDING"}, {@code "REJECTED"}). Empty when the relay
     * omitted the moderation status.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    final String moderationStatus;

    /**
     * Whether the product currently qualifies for an enforcement
     * appeal — typically only relevant for products in
     * {@code "REJECTED"} moderation status.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
    final boolean canAppeal;

    /**
     * Image assets attached to the product, in wire order. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    final List<BusinessProductImage> images;

    /**
     * Video assets attached to the product, in wire order. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    final List<BusinessProductVideo> videos;

    /**
     * Discounted-price block superseding {@link #price} for the
     * configured promotional window. Empty when the merchant did not
     * configure a sale.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    final BusinessProductSalePrice salePrice;

    /**
     * Country-of-origin and importer disclosure for the product.
     * Empty when the merchant did not publish a compliance block.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.MESSAGE)
    final BusinessProductCompliance compliance;

    /**
     * Signed CDN URL the WhatsApp catalog returns for products that
     * route through a per-merchant signed-shimmed proxy. Empty when
     * the relay did not publish a signed URL.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.STRING)
    final URI signedShimmedUri;

    /**
     * Compliance category marker (for example {@code "Default"} or
     * {@code "CountryOriginExempt"}) the WhatsApp policy team applied
     * to the product. Empty when the relay omitted the marker.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.STRING)
    final String complianceCategory;

    /**
     * Constructs a new {@code BusinessProduct}. The {@code id}
     * argument is required; every other scalar may be {@code null}
     * when the relay omitted it. {@code null} list arguments are
     * coerced to empty lists.
     *
     * @param id                 the catalog product identifier; never {@code null}
     * @param invalid            the invalid-product placeholder flag
     * @param name               the optional display name, or {@code null}
     * @param description        the optional marketing copy, or {@code null}
     * @param uri                the optional product page URL, or {@code null}
     * @param retailerId         the optional retailer-side id, or {@code null}
     * @param availability       the optional availability marker, or {@code null}
     * @param maxAvailable       the per-product cart cap
     * @param currency           the optional currency code, or {@code null}
     * @param price              the optional price string, or {@code null}
     * @param hidden             the hidden-from-grid flag
     * @param sanctioned         the sanctioned flag
     * @param checkmark          the featured/checkmarked flag
     * @param moderationStatus   the optional WhatsApp moderation status, or {@code null}
     * @param canAppeal          the appealable flag
     * @param images             the image asset list; {@code null} treated as empty
     * @param videos             the video asset list; {@code null} treated as empty
     * @param salePrice          the optional sale-price block, or {@code null}
     * @param compliance         the optional compliance block, or {@code null}
     * @param signedShimmedUri   the optional signed CDN URL, or {@code null}
     * @param complianceCategory the optional compliance-category marker, or {@code null}
     * @throws NullPointerException if {@code id} is {@code null}
     */
    BusinessProduct(String id,
                    boolean invalid,
                    String name,
                    String description,
                    URI uri,
                    String retailerId,
                    BusinessItemAvailability availability,
                    int maxAvailable,
                    String currency,
                    String price,
                    boolean hidden,
                    boolean sanctioned,
                    boolean checkmark,
                    String moderationStatus,
                    boolean canAppeal,
                    List<BusinessProductImage> images,
                    List<BusinessProductVideo> videos,
                    BusinessProductSalePrice salePrice,
                    BusinessProductCompliance compliance,
                    URI signedShimmedUri,
                    String complianceCategory) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.invalid = invalid;
        this.name = name;
        this.description = description;
        this.uri = uri;
        this.retailerId = retailerId;
        this.availability = availability;
        this.maxAvailable = maxAvailable;
        this.currency = currency;
        this.price = price;
        this.hidden = hidden;
        this.sanctioned = sanctioned;
        this.checkmark = checkmark;
        this.moderationStatus = moderationStatus;
        this.canAppeal = canAppeal;
        this.images = images == null ? List.of() : images;
        this.videos = videos == null ? List.of() : videos;
        this.salePrice = salePrice;
        this.compliance = compliance;
        this.signedShimmedUri = signedShimmedUri;
        this.complianceCategory = complianceCategory;
    }

    /**
     * Returns the server-issued catalog product identifier.
     *
     * @return the product id; never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns whether this entry is the synthetic
     * {@code "INVALID_PRODUCT"} placeholder.
     *
     * @return {@code true} when the relay marked the entry invalid
     */
    public boolean invalid() {
        return invalid;
    }

    /**
     * Returns the customer-facing display name.
     *
     * @return an {@code Optional} containing the name, or empty
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the marketing-copy description.
     *
     * @return an {@code Optional} containing the description, or empty
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the external product page URL.
     *
     * @return an {@code Optional} containing the URL, or empty
     */
    public Optional<URI> uri() {
        return Optional.ofNullable(uri);
    }

    /**
     * Returns the retailer-side product identifier.
     *
     * @return an {@code Optional} containing the retailer id, or empty
     */
    public Optional<String> retailerId() {
        return Optional.ofNullable(retailerId);
    }

    /**
     * Returns the stock-availability marker.
     *
     * @return an {@code Optional} containing the availability, or
     *         empty
     */
    public Optional<BusinessItemAvailability> availability() {
        return Optional.ofNullable(availability);
    }

    /**
     * Returns the per-product cart-item cap.
     *
     * @return the maximum cart quantity
     */
    public int maxAvailable() {
        return maxAvailable;
    }

    /**
     * Returns the ISO 4217 currency code.
     *
     * @return an {@code Optional} containing the currency, or empty
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the regular price string.
     *
     * @return an {@code Optional} containing the price, or empty
     */
    public Optional<String> price() {
        return Optional.ofNullable(price);
    }

    /**
     * Returns whether the product is hidden from the storefront grid.
     *
     * @return {@code true} when hidden
     */
    public boolean hidden() {
        return hidden;
    }

    /**
     * Returns whether the product was sanctioned by Meta enforcement.
     *
     * @return {@code true} when sanctioned
     */
    public boolean sanctioned() {
        return sanctioned;
    }

    /**
     * Returns whether the product is part of a featured/checkmarked
     * set.
     *
     * @return {@code true} when featured
     */
    public boolean checkmark() {
        return checkmark;
    }

    /**
     * Returns the WhatsApp policy moderation outcome.
     *
     * @return an {@code Optional} containing the moderation status,
     *         or empty
     */
    public Optional<String> moderationStatus() {
        return Optional.ofNullable(moderationStatus);
    }

    /**
     * Returns whether the product currently qualifies for an
     * enforcement appeal.
     *
     * @return {@code true} when an appeal is permitted
     */
    public boolean canAppeal() {
        return canAppeal;
    }

    /**
     * Returns the image assets attached to the product.
     *
     * @return an unmodifiable view of the image list; never
     *         {@code null}, possibly empty
     */
    public List<BusinessProductImage> images() {
        return Collections.unmodifiableList(images);
    }

    /**
     * Returns the video assets attached to the product.
     *
     * @return an unmodifiable view of the video list; never
     *         {@code null}, possibly empty
     */
    public List<BusinessProductVideo> videos() {
        return Collections.unmodifiableList(videos);
    }

    /**
     * Returns the discounted-price block.
     *
     * @return an {@code Optional} containing the sale-price block,
     *         or empty
     */
    public Optional<BusinessProductSalePrice> salePrice() {
        return Optional.ofNullable(salePrice);
    }

    /**
     * Returns the country-of-origin and importer disclosure block.
     *
     * @return an {@code Optional} containing the compliance block,
     *         or empty
     */
    public Optional<BusinessProductCompliance> compliance() {
        return Optional.ofNullable(compliance);
    }

    /**
     * Returns the signed CDN URL.
     *
     * @return an {@code Optional} containing the URL, or empty
     */
    public Optional<URI> signedShimmedUri() {
        return Optional.ofNullable(signedShimmedUri);
    }

    /**
     * Returns the compliance-category marker.
     *
     * @return an {@code Optional} containing the marker, or empty
     */
    public Optional<String> complianceCategory() {
        return Optional.ofNullable(complianceCategory);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessProduct) obj;
        return this.invalid == that.invalid
                && this.maxAvailable == that.maxAvailable
                && this.hidden == that.hidden
                && this.sanctioned == that.sanctioned
                && this.checkmark == that.checkmark
                && this.canAppeal == that.canAppeal
                && Objects.equals(this.id, that.id)
                && Objects.equals(this.name, that.name)
                && Objects.equals(this.description, that.description)
                && Objects.equals(this.uri, that.uri)
                && Objects.equals(this.retailerId, that.retailerId)
                && this.availability == that.availability
                && Objects.equals(this.currency, that.currency)
                && Objects.equals(this.price, that.price)
                && Objects.equals(this.moderationStatus, that.moderationStatus)
                && Objects.equals(this.images, that.images)
                && Objects.equals(this.videos, that.videos)
                && Objects.equals(this.salePrice, that.salePrice)
                && Objects.equals(this.compliance, that.compliance)
                && Objects.equals(this.signedShimmedUri, that.signedShimmedUri)
                && Objects.equals(this.complianceCategory, that.complianceCategory);
    }

    @Override
    public int hashCode() {
        var h = Objects.hash(id, invalid, name, description, uri, retailerId,
                availability, maxAvailable, currency, price, hidden, sanctioned,
                checkmark, moderationStatus, canAppeal);
        return 31 * h + Objects.hash(images, videos, salePrice, compliance,
                signedShimmedUri, complianceCategory);
    }

    @Override
    public String toString() {
        return "BusinessProduct[" +
                "id=" + id + ", " +
                "invalid=" + invalid + ", " +
                "name=" + name + ", " +
                "price=" + price + ", " +
                "currency=" + currency + ']';
    }
}
