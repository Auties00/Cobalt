package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One page of products from a WhatsApp Business catalog.
 *
 * <p>A merchant's catalog can hold more products than fit in a single
 * response, so the server returns them one page at a time. Each page carries
 * the catalog's own identity, the products on that page as full
 * {@link BusinessProduct} entries, and the cursors needed to walk forward or
 * backward through the remaining pages.
 *
 * <p>To read the next page, pass {@link #afterCursor()} (when present) as the
 * {@code afterCursor} of the next read's
 * {@code CatalogFetchOptions}; an empty {@link #afterCursor()} means this is
 * the last page. The {@link #beforeCursor()} cursor walks toward earlier pages
 * the same way.
 */
@ProtobufMessage
public final class BusinessCatalogPage {
    /**
     * Server-assigned identifier of the catalog this page belongs to. Empty
     * when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String catalogId;

    /**
     * Server-assigned type discriminator of the catalog (for example the
     * marker distinguishing a merchant's own catalog from a shop-linked one).
     * Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String catalogType;

    /**
     * Display name of the catalog. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String catalogName;

    /**
     * Products on this page, in server order. Never {@code null}, possibly
     * empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final List<BusinessProduct> products;

    /**
     * Cursor that walks toward the previous page. Empty when this is the first
     * page.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String beforeCursor;

    /**
     * Cursor that walks toward the next page. Empty when this is the last page.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String afterCursor;

    /**
     * Constructs a new {@code BusinessCatalogPage}. Every scalar may be
     * {@code null} when the server omitted it; a {@code null} product list is
     * coerced to an empty list.
     *
     * @param catalogId    the catalog identifier, or {@code null}
     * @param catalogType  the catalog type discriminator, or {@code null}
     * @param catalogName  the catalog display name, or {@code null}
     * @param products     the products on this page; {@code null} treated as empty
     * @param beforeCursor the previous-page cursor, or {@code null}
     * @param afterCursor  the next-page cursor, or {@code null}
     */
    BusinessCatalogPage(String catalogId, String catalogType, String catalogName,
                        List<BusinessProduct> products, String beforeCursor, String afterCursor) {
        this.catalogId = catalogId;
        this.catalogType = catalogType;
        this.catalogName = catalogName;
        this.products = products == null ? List.of() : products;
        this.beforeCursor = beforeCursor;
        this.afterCursor = afterCursor;
    }

    /**
     * Returns the catalog identifier.
     *
     * @return an {@code Optional} carrying the id, or empty when the server omitted it
     */
    public Optional<String> catalogId() {
        return Optional.ofNullable(catalogId);
    }

    /**
     * Returns the catalog type discriminator.
     *
     * @return an {@code Optional} carrying the type, or empty when the server omitted it
     */
    public Optional<String> catalogType() {
        return Optional.ofNullable(catalogType);
    }

    /**
     * Returns the catalog display name.
     *
     * @return an {@code Optional} carrying the name, or empty when the server omitted it
     */
    public Optional<String> catalogName() {
        return Optional.ofNullable(catalogName);
    }

    /**
     * Returns the products on this page.
     *
     * @return an unmodifiable view of the products; never {@code null}, possibly empty
     */
    public List<BusinessProduct> products() {
        return Collections.unmodifiableList(products);
    }

    /**
     * Returns the cursor that walks toward the previous page.
     *
     * @return an {@code Optional} carrying the cursor, or empty when this is the first page
     */
    public Optional<String> beforeCursor() {
        return beforeCursor == null || beforeCursor.isEmpty() ? Optional.empty() : Optional.of(beforeCursor);
    }

    /**
     * Returns the cursor that walks toward the next page.
     *
     * @return an {@code Optional} carrying the cursor, or empty when this is the last page
     */
    public Optional<String> afterCursor() {
        return afterCursor == null || afterCursor.isEmpty() ? Optional.empty() : Optional.of(afterCursor);
    }
}
