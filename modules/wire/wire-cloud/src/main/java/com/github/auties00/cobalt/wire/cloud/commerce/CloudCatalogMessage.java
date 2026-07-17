package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Cloud API catalog interactive message.
 *
 * <p>Sends a button that opens the business's full connected catalog. The body caption is mandatory; an
 * optional thumbnail product retailer id selects which catalog item is shown on the message preview, and
 * the footer is optional.
 */
public final class CloudCatalogMessage {
    /**
     * The mandatory body caption.
     */
    private final String body;

    /**
     * The footer caption, or {@code null} for none.
     */
    private final String footer;

    /**
     * The preview thumbnail product SKU, or {@code null} for the catalog default.
     */
    private final String thumbnailProductRetailerId;

    /**
     * Constructs a new catalog message.
     *
     * @param body                       the mandatory body caption
     * @param footer                     the optional footer caption, or {@code null} for none
     * @param thumbnailProductRetailerId the optional preview thumbnail product SKU, or {@code null} for the catalog default
     * @throws NullPointerException if {@code body} is {@code null}
     */
    public CloudCatalogMessage(String body, String footer, String thumbnailProductRetailerId) {
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.footer = footer;
        this.thumbnailProductRetailerId = thumbnailProductRetailerId;
    }

    /**
     * Returns the mandatory body caption.
     *
     * @return the body caption
     */
    public String body() {
        return body;
    }

    /**
     * Returns the footer caption.
     *
     * @return an {@link Optional} carrying the footer caption, or empty for none
     */
    public Optional<String> footer() {
        return Optional.ofNullable(footer);
    }

    /**
     * Returns the preview thumbnail product SKU.
     *
     * @return an {@link Optional} carrying the thumbnail product retailer id, or empty for the catalog default
     */
    public Optional<String> thumbnailProductRetailerId() {
        return Optional.ofNullable(thumbnailProductRetailerId);
    }
}
