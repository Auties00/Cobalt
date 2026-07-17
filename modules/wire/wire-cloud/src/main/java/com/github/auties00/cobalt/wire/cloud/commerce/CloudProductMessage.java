package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Cloud API single-product interactive message.
 *
 * <p>Renders one catalog item as a Product Detail Page card. The product is identified by the pair
 * (catalog id, product retailer id); the body and footer captions are optional.
 */
public final class CloudProductMessage {
    /**
     * The Commerce Manager catalog id.
     */
    private final String catalogId;

    /**
     * The product SKU within the catalog.
     */
    private final String productRetailerId;

    /**
     * The body caption, or {@code null} for none.
     */
    private final String body;

    /**
     * The footer caption, or {@code null} for none.
     */
    private final String footer;

    /**
     * Constructs a new single-product message.
     *
     * @param catalogId         the Commerce Manager catalog id
     * @param productRetailerId the product SKU within the catalog
     * @param body              the optional body caption, or {@code null} for none
     * @param footer            the optional footer caption, or {@code null} for none
     * @throws NullPointerException if {@code catalogId} or {@code productRetailerId} is {@code null}
     */
    public CloudProductMessage(String catalogId, String productRetailerId, String body, String footer) {
        this.catalogId = Objects.requireNonNull(catalogId, "catalogId must not be null");
        this.productRetailerId = Objects.requireNonNull(productRetailerId, "productRetailerId must not be null");
        this.body = body;
        this.footer = footer;
    }

    /**
     * Returns the Commerce Manager catalog id.
     *
     * @return the catalog id
     */
    public String catalogId() {
        return catalogId;
    }

    /**
     * Returns the product SKU within the catalog.
     *
     * @return the product retailer id
     */
    public String productRetailerId() {
        return productRetailerId;
    }

    /**
     * Returns the body caption.
     *
     * @return an {@link Optional} carrying the body caption, or empty for none
     */
    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    /**
     * Returns the footer caption.
     *
     * @return an {@link Optional} carrying the footer caption, or empty for none
     */
    public Optional<String> footer() {
        return Optional.ofNullable(footer);
    }
}
