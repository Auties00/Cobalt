package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One product a WhatsApp Business AI agent knows about and can describe to
 * customers.
 *
 * <p>Alongside its frequently-asked-question and website knowledge, the
 * merchant's auto-reply assistant can hold structured product entries it can
 * surface in a conversation: each carries the product's title, a
 * description, a price, and a set of images. This model is one such product
 * entry.
 */
@ProtobufMessage(name = "BusinessAiProductInfo")
public final class BusinessAiProductInfo {
    /**
     * Catalog identifier of the product. This is a product-catalog
     * identifier, not a WhatsApp address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String productId;

    /**
     * Display title of the product. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String title;

    /**
     * Free-form description of the product. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String description;

    /**
     * Human-readable price of the product, as the merchant entered it (for
     * example {@code "19.99 EUR"}). Empty when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String price;

    /**
     * Images attached to the product, in the order the server returned
     * them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final List<BusinessAiProductImage> images;

    /**
     * Constructs a new {@code BusinessAiProductInfo}. A {@code null}
     * {@code images} is coerced to an empty list; every other argument may
     * be {@code null} when the server omitted it.
     *
     * @param productId   the catalog product identifier, or {@code null}
     * @param title       the display title, or {@code null}
     * @param description the description, or {@code null}
     * @param price       the human-readable price, or {@code null}
     * @param images      the product images; {@code null} treated as empty
     */
    BusinessAiProductInfo(String productId, String title, String description, String price, List<BusinessAiProductImage> images) {
        this.productId = productId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.images = images == null ? List.of() : images;
    }

    /**
     * Returns the catalog identifier of the product.
     *
     * @return the product id, or empty when the server omitted it
     */
    public Optional<String> productId() {
        return Optional.ofNullable(productId);
    }

    /**
     * Returns the display title of the product.
     *
     * @return the title, or empty when the server omitted it
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the free-form description of the product.
     *
     * @return the description, or empty when the server omitted it
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the human-readable price of the product.
     *
     * @return the price, or empty when the server omitted it
     */
    public Optional<String> price() {
        return Optional.ofNullable(price);
    }

    /**
     * Returns the images attached to the product.
     *
     * @return an unmodifiable view of the product images; never
     *         {@code null}, possibly empty
     */
    public List<BusinessAiProductImage> images() {
        return Collections.unmodifiableList(images);
    }
}
