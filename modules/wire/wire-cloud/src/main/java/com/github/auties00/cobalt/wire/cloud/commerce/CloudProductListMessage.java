package com.github.auties00.cobalt.wire.cloud.commerce;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Cloud API multi-product interactive message.
 *
 * <p>Renders a grouped list of catalog items drawn from a single catalog. A mandatory text header and
 * body accompany one or more {@link Section sections}, each holding the retailer ids of the products it
 * lists; the footer is optional.
 */
public final class CloudProductListMessage {
    /**
     * The Commerce Manager catalog id.
     */
    private final String catalogId;

    /**
     * The mandatory text header.
     */
    private final String header;

    /**
     * The mandatory body caption.
     */
    private final String body;

    /**
     * The footer caption, or {@code null} for none.
     */
    private final String footer;

    /**
     * The product sections, at least one.
     */
    private final List<Section> sections;

    /**
     * Constructs a new multi-product message.
     *
     * @param catalogId the Commerce Manager catalog id
     * @param header    the mandatory text header
     * @param body      the mandatory body caption
     * @param footer    the optional footer caption, or {@code null} for none
     * @param sections  the product sections, at least one
     * @throws NullPointerException     if {@code catalogId}, {@code header}, {@code body} or {@code sections} is {@code null}
     * @throws IllegalArgumentException if {@code sections} is empty
     */
    public CloudProductListMessage(String catalogId, String header, String body, String footer, List<Section> sections) {
        this.catalogId = Objects.requireNonNull(catalogId, "catalogId must not be null");
        this.header = Objects.requireNonNull(header, "header must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.footer = footer;
        Objects.requireNonNull(sections, "sections must not be null");
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("sections must not be empty");
        }
        this.sections = List.copyOf(sections);
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
     * Returns the mandatory text header.
     *
     * @return the header
     */
    public String header() {
        return header;
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
     * Returns the product sections.
     *
     * @return an unmodifiable list of sections, at least one
     */
    public List<Section> sections() {
        return sections;
    }

    /**
     * A titled group of product retailer ids within a multi-product message.
     */
    public static final class Section {
        /**
         * The section title.
         */
        private final String title;

        /**
         * The product SKUs in this section, at least one.
         */
        private final List<String> productRetailerIds;

        /**
         * Constructs a new section.
         *
         * @param title              the section title
         * @param productRetailerIds the product SKUs in this section, at least one
         * @throws NullPointerException     if {@code title} or {@code productRetailerIds} is {@code null}
         * @throws IllegalArgumentException if {@code productRetailerIds} is empty
         */
        public Section(String title, List<String> productRetailerIds) {
            this.title = Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(productRetailerIds, "productRetailerIds must not be null");
            if (productRetailerIds.isEmpty()) {
                throw new IllegalArgumentException("productRetailerIds must not be empty");
            }
            this.productRetailerIds = List.copyOf(productRetailerIds);
        }

        /**
         * Returns the section title.
         *
         * @return the title
         */
        public String title() {
            return title;
        }

        /**
         * Returns the product SKUs in this section.
         *
         * @return an unmodifiable list of product retailer ids, at least one
         */
        public List<String> productRetailerIds() {
            return productRetailerIds;
        }
    }
}
