package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Write payload describing a product to add to or edit in a WhatsApp Business catalog.
 *
 * <p>The {@code product_info} object the catalog add/edit mutations carry is a conditionally populated
 * write tree. This model carries the fields WhatsApp Web's product-write builder populates: the
 * product's {@link #name() name}, {@link #description() description}, {@link #url() destination URL},
 * {@link #retailerId() retailer id}, {@link #currency() currency} and {@link #price() price}, the
 * {@link #salePrice() sale price}, the {@link #hidden() hidden} flag, the {@link #media() media}, the
 * {@link #complianceInfo() compliance information}, and the {@link #complianceCategory() compliance
 * category}.
 */
@ProtobufMessage(name = "CatalogProductInfo")
public final class CatalogProductInfo {
    /**
     * Display name of the product. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Free-form description of the product. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    /**
     * Destination URL the product links to. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String url;

    /**
     * Merchant-assigned retailer identifier of the product. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String retailerId;

    /**
     * ISO 4217 currency code the price is expressed in. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String currency;

    /**
     * Price of the product in the currency's minor units, encoded as a string. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String price;

    /**
     * Sale price of the product in the currency's minor units, encoded as a string. Empty when unset.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String salePrice;

    /**
     * Whether the product is hidden from the catalog.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    final boolean hidden;

    /**
     * Media (images and videos) attached to the product. Empty when unset.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    final CatalogMedia media;

    /**
     * Compliance information declared for the product. Empty when unset.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    final CatalogComplianceInfo complianceInfo;

    /**
     * Compliance category token of the product. Empty when unset.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    final String complianceCategory;

    /**
     * Constructs a new {@code CatalogProductInfo}. Every object argument may be {@code null} to leave
     * the corresponding field unset.
     *
     * @param name               the display name, or {@code null}
     * @param description        the description, or {@code null}
     * @param url                the destination URL, or {@code null}
     * @param retailerId         the retailer identifier, or {@code null}
     * @param currency           the currency code, or {@code null}
     * @param price              the price in minor units, or {@code null}
     * @param salePrice          the sale price in minor units, or {@code null}
     * @param hidden             whether the product is hidden
     * @param media              the product media, or {@code null}
     * @param complianceInfo     the compliance information, or {@code null}
     * @param complianceCategory the compliance category token, or {@code null}
     */
    public CatalogProductInfo(String name, String description, String url, String retailerId, String currency,
                              String price, String salePrice, boolean hidden, CatalogMedia media,
                              CatalogComplianceInfo complianceInfo, String complianceCategory) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.retailerId = retailerId;
        this.currency = currency;
        this.price = price;
        this.salePrice = salePrice;
        this.hidden = hidden;
        this.media = media;
        this.complianceInfo = complianceInfo;
        this.complianceCategory = complianceCategory;
    }

    /**
     * Returns the display name of the product.
     *
     * @return an {@link Optional} carrying the name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the free-form description of the product.
     *
     * @return an {@link Optional} carrying the description, or empty when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the destination URL the product links to.
     *
     * @return an {@link Optional} carrying the URL, or empty when unset
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the merchant-assigned retailer identifier of the product.
     *
     * @return an {@link Optional} carrying the retailer id, or empty when unset
     */
    public Optional<String> retailerId() {
        return Optional.ofNullable(retailerId);
    }

    /**
     * Returns the currency code the price is expressed in.
     *
     * @return an {@link Optional} carrying the currency, or empty when unset
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the price of the product in the currency's minor units.
     *
     * @return an {@link Optional} carrying the price, or empty when unset
     */
    public Optional<String> price() {
        return Optional.ofNullable(price);
    }

    /**
     * Returns the sale price of the product in the currency's minor units.
     *
     * @return an {@link Optional} carrying the sale price, or empty when unset
     */
    public Optional<String> salePrice() {
        return Optional.ofNullable(salePrice);
    }

    /**
     * Returns whether the product is hidden from the catalog.
     *
     * @return {@code true} when the product is hidden, {@code false} otherwise
     */
    public boolean hidden() {
        return hidden;
    }

    /**
     * Returns the media (images and videos) attached to the product.
     *
     * @return an {@link Optional} carrying the media, or empty when unset
     */
    public Optional<CatalogMedia> media() {
        return Optional.ofNullable(media);
    }

    /**
     * Returns the compliance information declared for the product.
     *
     * @return an {@link Optional} carrying the compliance information, or empty when unset
     */
    public Optional<CatalogComplianceInfo> complianceInfo() {
        return Optional.ofNullable(complianceInfo);
    }

    /**
     * Returns the compliance category token of the product.
     *
     * @return an {@link Optional} carrying the compliance category, or empty when unset
     */
    public Optional<String> complianceCategory() {
        return Optional.ofNullable(complianceCategory);
    }
}
