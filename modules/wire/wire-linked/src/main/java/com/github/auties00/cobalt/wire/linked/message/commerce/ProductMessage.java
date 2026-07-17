package com.github.auties00.cobalt.wire.linked.message.commerce;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A message that shares a single product from a WhatsApp Business catalog
 * with a chat recipient.
 *
 * <p>Product messages bundle a {@link ProductSnapshot} that captures the
 * product details (image, title, description, price, currency, retailer
 * identifier, and optional sale price) at the moment the message was sent,
 * the JID of the business that owns the catalog, an optional
 * {@link CatalogSnapshot} that shows a preview of the catalog the product
 * belongs to, and free-text body and footer strings rendered around the
 * product card.
 */
@ProtobufMessage(name = "Message.ProductMessage")
public final class ProductMessage implements ContextualMessage, InteractiveMessage.MediaSpec {
    /**
     * The snapshot of the shared product at the time the message was sent.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    ProductSnapshot product;

    /**
     * The JID of the business account that owns the catalog containing
     * the product.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid businessOwnerJid;

    /**
     * An optional snapshot of the catalog the product belongs to, shown
     * as a preview alongside the product card.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    CatalogSnapshot catalog;

    /**
     * The body text shown above the product card.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String body;

    /**
     * The footer text shown below the product card.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String footer;

    /**
     * Contextual metadata attached to this message.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new product message with every field set explicitly.
     *
     * <p>This constructor is package-private; callers should use the
     * generated {@code ProductMessageBuilder} to create instances.
     *
     * @param product the shared product snapshot
     * @param businessOwnerJid the JID of the selling business
     * @param catalog the catalog preview snapshot, or {@code null}
     * @param body the body text shown above the product card
     * @param footer the footer text shown below the product card
     * @param contextInfo the contextual metadata of the message
     */
    ProductMessage(ProductSnapshot product, Jid businessOwnerJid, CatalogSnapshot catalog, String body, String footer, ContextInfo contextInfo) {
        this.product = product;
        this.businessOwnerJid = businessOwnerJid;
        this.catalog = catalog;
        this.body = body;
        this.footer = footer;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the snapshot of the shared product.
     *
     * @return an {@link Optional} containing the product snapshot, or empty if not set
     */
    public Optional<ProductSnapshot> product() {
        return Optional.ofNullable(product);
    }

    /**
     * Returns the JID of the business account that owns the catalog.
     *
     * @return an {@link Optional} containing the business JID, or empty if not set
     */
    public Optional<Jid> businessOwnerJid() {
        return Optional.ofNullable(businessOwnerJid);
    }

    /**
     * Returns the preview snapshot of the catalog the product belongs to.
     *
     * @return an {@link Optional} containing the catalog snapshot, or empty if not set
     */
    public Optional<CatalogSnapshot> catalog() {
        return Optional.ofNullable(catalog);
    }

    /**
     * Returns the body text shown above the product card.
     *
     * @return an {@link Optional} containing the body text, or empty if not set
     */
    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    /**
     * Returns the footer text shown below the product card.
     *
     * @return an {@link Optional} containing the footer text, or empty if not set
     */
    public Optional<String> footer() {
        return Optional.ofNullable(footer);
    }

    /**
     * Returns the contextual metadata of this message.
     *
     * @return an {@link Optional} containing the context info, or empty if not set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Sets the snapshot of the shared product.
     *
     * @param product the product snapshot, or {@code null} to clear it
     */
    public void setProduct(ProductSnapshot product) {
        this.product = product;
    }

    /**
     * Sets the JID of the business account that owns the catalog.
     *
     * @param businessOwnerJid the business JID, or {@code null} to clear it
     */
    public void setBusinessOwnerJid(Jid businessOwnerJid) {
        this.businessOwnerJid = businessOwnerJid;
    }

    /**
     * Sets the preview snapshot of the catalog.
     *
     * @param catalog the catalog snapshot, or {@code null} to clear it
     */
    public void setCatalog(CatalogSnapshot catalog) {
        this.catalog = catalog;
    }

    /**
     * Sets the body text shown above the product card.
     *
     * @param body the body text, or {@code null} to clear it
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Sets the footer text shown below the product card.
     *
     * @param footer the footer text, or {@code null} to clear it
     */
    public void setFooter(String footer) {
        this.footer = footer;
    }

    /**
     * Sets the contextual metadata of this message.
     *
     * @param contextInfo the context info, or {@code null} to clear it
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * A lightweight preview of a WhatsApp Business catalog embedded in a
     * {@link ProductMessage}.
     *
     * <p>The snapshot captures the catalog's cover image, title and
     * description at the time the message was sent, giving recipients a
     * glimpse of the catalog without having to fetch it from the server.
     */
    @ProtobufMessage(name = "Message.ProductMessage.CatalogSnapshot")
    public static final class CatalogSnapshot {
        /**
         * The cover image of the catalog.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ImageMessage catalogImage;

        /**
         * The title of the catalog.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String title;

        /**
         * The description of the catalog.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String description;


        /**
         * Constructs a new catalog snapshot.
         *
         * <p>This constructor is package-private; callers should use the
         * generated builder.
         *
         * @param catalogImage the cover image of the catalog
         * @param title the title of the catalog
         * @param description the description of the catalog
         */
        CatalogSnapshot(ImageMessage catalogImage, String title, String description) {
            this.catalogImage = catalogImage;
            this.title = title;
            this.description = description;
        }

        /**
         * Returns the cover image of the catalog.
         *
         * @return an {@link Optional} containing the catalog image, or empty if not set
         */
        public Optional<ImageMessage> catalogImage() {
            return Optional.ofNullable(catalogImage);
        }

        /**
         * Returns the title of the catalog.
         *
         * @return an {@link Optional} containing the title, or empty if not set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the description of the catalog.
         *
         * @return an {@link Optional} containing the description, or empty if not set
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Sets the cover image of the catalog.
         *
         * @param catalogImage the catalog image, or {@code null} to clear it
         */
        public void setCatalogImage(ImageMessage catalogImage) {
            this.catalogImage = catalogImage;
    }

        /**
         * Sets the title of the catalog.
         *
         * @param title the title, or {@code null} to clear it
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Sets the description of the catalog.
         *
         * @param description the description, or {@code null} to clear it
         */
        public void setDescription(String description) {
            this.description = description;
    }
    }

    /**
     * A snapshot of a catalog product at the moment it is shared inside a
     * {@link ProductMessage}.
     *
     * <p>Embedding a snapshot (rather than just a product identifier)
     * ensures that the recipient sees the product details exactly as they
     * appeared when the sender shared them, even if the seller later edits
     * or removes the product from the catalog.
     *
     * <p>Prices are expressed in thousandths of the currency unit
     * identified by {@link #currencyCode}. For example, a price amount of
     * {@code 12345} with currency {@code USD} represents {@code 12.345}
     * US dollars.
     */
    @ProtobufMessage(name = "Message.ProductMessage.ProductSnapshot")
    public static final class ProductSnapshot {
        /**
         * The primary image of the product.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ImageMessage productImage;

        /**
         * The catalog-unique identifier of the product.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String productId;

        /**
         * The human-readable title of the product.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String title;

        /**
         * The human-readable description of the product.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String description;

        /**
         * The ISO 4217 currency code used to interpret
         * {@link #priceAmount1000} and {@link #salePriceAmount1000}.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String currencyCode;

        /**
         * The regular price of the product expressed in thousandths of
         * the currency unit.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.INT64)
        Long priceAmount1000;

        /**
         * The retailer-assigned identifier of the product, which may
         * differ from {@link #productId}.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String retailerId;

        /**
         * A URL pointing to the product page on the retailer's website.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        String url;

        /**
         * The total number of images associated with the product in the
         * catalog.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
        Integer productImageCount;

        /**
         * The identifier of the first image of the product.
         */
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        String firstImageId;

        /**
         * The discounted sale price of the product expressed in
         * thousandths of the currency unit, when the product is on sale.
         */
        @ProtobufProperty(index = 12, type = ProtobufType.INT64)
        Long salePriceAmount1000;

        /**
         * A pre-signed URL used to access the product resource on the
         * retailer's website without additional authentication.
         */
        @ProtobufProperty(index = 13, type = ProtobufType.STRING)
        String signedUrl;


        /**
         * Constructs a new product snapshot with every field set
         * explicitly.
         *
         * <p>This constructor is package-private; callers should use the
         * generated builder.
         *
         * @param productImage the primary product image
         * @param productId the catalog-unique product identifier
         * @param title the product title
         * @param description the product description
         * @param currencyCode the ISO 4217 currency code
         * @param priceAmount1000 the regular price in thousandths of the currency unit
         * @param retailerId the retailer-assigned product identifier
         * @param url the product page URL
         * @param productImageCount the total number of product images
         * @param firstImageId the identifier of the first product image
         * @param salePriceAmount1000 the sale price in thousandths of the currency unit
         * @param signedUrl the pre-signed product URL
         */
        ProductSnapshot(ImageMessage productImage, String productId, String title, String description, String currencyCode, Long priceAmount1000, String retailerId, String url, Integer productImageCount, String firstImageId, Long salePriceAmount1000, String signedUrl) {
            this.productImage = productImage;
            this.productId = productId;
            this.title = title;
            this.description = description;
            this.currencyCode = currencyCode;
            this.priceAmount1000 = priceAmount1000;
            this.retailerId = retailerId;
            this.url = url;
            this.productImageCount = productImageCount;
            this.firstImageId = firstImageId;
            this.salePriceAmount1000 = salePriceAmount1000;
            this.signedUrl = signedUrl;
        }

        /**
         * Returns the primary image of the product.
         *
         * @return an {@link Optional} containing the product image, or empty if not set
         */
        public Optional<ImageMessage> productImage() {
            return Optional.ofNullable(productImage);
        }

        /**
         * Returns the catalog-unique identifier of the product.
         *
         * @return an {@link Optional} containing the product identifier, or empty if not set
         */
        public Optional<String> productId() {
            return Optional.ofNullable(productId);
        }

        /**
         * Returns the human-readable title of the product.
         *
         * @return an {@link Optional} containing the title, or empty if not set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the human-readable description of the product.
         *
         * @return an {@link Optional} containing the description, or empty if not set
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Returns the ISO 4217 currency code used to interpret the price
         * amounts.
         *
         * @return an {@link Optional} containing the currency code, or empty if not set
         */
        public Optional<String> currencyCode() {
            return Optional.ofNullable(currencyCode);
        }

        /**
         * Returns the regular price of the product expressed in
         * thousandths of the currency unit.
         *
         * @return an {@link OptionalLong} containing the price in thousandths, or empty if not set
         */
        public OptionalLong priceAmount1000() {
            return priceAmount1000 == null ? OptionalLong.empty() : OptionalLong.of(priceAmount1000);
        }

        /**
         * Returns the retailer-assigned identifier of the product.
         *
         * @return an {@link Optional} containing the retailer identifier, or empty if not set
         */
        public Optional<String> retailerId() {
            return Optional.ofNullable(retailerId);
        }

        /**
         * Returns the URL pointing to the product page on the retailer's
         * website.
         *
         * @return an {@link Optional} containing the URL, or empty if not set
         */
        public Optional<String> url() {
            return Optional.ofNullable(url);
        }

        /**
         * Returns the total number of images associated with the product
         * in the catalog.
         *
         * @return an {@link OptionalInt} containing the image count, or empty if not set
         */
        public OptionalInt productImageCount() {
            return productImageCount == null ? OptionalInt.empty() : OptionalInt.of(productImageCount);
        }

        /**
         * Returns the identifier of the first image of the product.
         *
         * @return an {@link Optional} containing the first image identifier, or empty if not set
         */
        public Optional<String> firstImageId() {
            return Optional.ofNullable(firstImageId);
        }

        /**
         * Returns the discounted sale price of the product expressed in
         * thousandths of the currency unit.
         *
         * @return an {@link OptionalLong} containing the sale price in thousandths, or empty if not set
         */
        public OptionalLong salePriceAmount1000() {
            return salePriceAmount1000 == null ? OptionalLong.empty() : OptionalLong.of(salePriceAmount1000);
        }

        /**
         * Returns the pre-signed URL for the product resource.
         *
         * @return an {@link Optional} containing the signed URL, or empty if not set
         */
        public Optional<String> signedUrl() {
            return Optional.ofNullable(signedUrl);
        }

        /**
         * Sets the primary image of the product.
         *
         * @param productImage the product image, or {@code null} to clear it
         */
        public void setProductImage(ImageMessage productImage) {
            this.productImage = productImage;
    }

        /**
         * Sets the catalog-unique identifier of the product.
         *
         * @param productId the product identifier, or {@code null} to clear it
         */
        public void setProductId(String productId) {
            this.productId = productId;
    }

        /**
         * Sets the human-readable title of the product.
         *
         * @param title the title, or {@code null} to clear it
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Sets the human-readable description of the product.
         *
         * @param description the description, or {@code null} to clear it
         */
        public void setDescription(String description) {
            this.description = description;
    }

        /**
         * Sets the ISO 4217 currency code used to interpret the price
         * amounts.
         *
         * @param currencyCode the currency code, or {@code null} to clear it
         */
        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
    }

        /**
         * Sets the regular price of the product in thousandths of the
         * currency unit.
         *
         * @param priceAmount1000 the price in thousandths, or {@code null} to clear it
         */
        public void setPriceAmount1000(Long priceAmount1000) {
            this.priceAmount1000 = priceAmount1000;
    }

        /**
         * Sets the retailer-assigned identifier of the product.
         *
         * @param retailerId the retailer identifier, or {@code null} to clear it
         */
        public void setRetailerId(String retailerId) {
            this.retailerId = retailerId;
    }

        /**
         * Sets the URL pointing to the product page.
         *
         * @param url the URL, or {@code null} to clear it
         */
        public void setUrl(String url) {
            this.url = url;
    }

        /**
         * Sets the total number of product images in the catalog.
         *
         * @param productImageCount the image count, or {@code null} to clear it
         */
        public void setProductImageCount(Integer productImageCount) {
            this.productImageCount = productImageCount;
    }

        /**
         * Sets the identifier of the first image of the product.
         *
         * @param firstImageId the first image identifier, or {@code null} to clear it
         */
        public void setFirstImageId(String firstImageId) {
            this.firstImageId = firstImageId;
    }

        /**
         * Sets the discounted sale price of the product in thousandths of
         * the currency unit.
         *
         * @param salePriceAmount1000 the sale price in thousandths, or {@code null} to clear it
         */
        public void setSalePriceAmount1000(Long salePriceAmount1000) {
            this.salePriceAmount1000 = salePriceAmount1000;
    }

        /**
         * Sets the pre-signed URL for the product resource.
         *
         * @param signedUrl the signed URL, or {@code null} to clear it
         */
        public void setSignedUrl(String signedUrl) {
            this.signedUrl = signedUrl;
    }
    }
}
