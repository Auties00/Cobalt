package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One page of product collections from a WhatsApp Business catalog.
 *
 * <p>A merchant can group catalog products into several named collections, and
 * the server returns those collections one page at a time. Each page carries
 * the catalog's type discriminator, the collections on that page (each a
 * {@link BusinessProductCollection} with its own products), and the cursor
 * needed to fetch the next page.
 *
 * <p>To read the next page, pass {@link #afterCursor()} (when present) as the
 * {@code afterCursor} of the next read's
 * {@code CatalogFetchOptions}; an empty {@link #afterCursor()} means this is
 * the last page.
 */
@ProtobufMessage
public final class BusinessCatalogCollections {
    /**
     * Server-assigned type discriminator of the catalog these collections
     * belong to. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String catalogType;

    /**
     * Collections on this page, in server order. Never {@code null}, possibly
     * empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<BusinessProductCollection> collections;

    /**
     * Cursor that walks toward the next page of collections. Empty when this is
     * the last page.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String afterCursor;

    /**
     * Constructs a new {@code BusinessCatalogCollections}. The catalog type and
     * cursor may be {@code null} when the server omitted them; a {@code null}
     * collection list is coerced to an empty list.
     *
     * @param catalogType the catalog type discriminator, or {@code null}
     * @param collections the collections on this page; {@code null} treated as empty
     * @param afterCursor the next-page cursor, or {@code null}
     */
    BusinessCatalogCollections(String catalogType, List<BusinessProductCollection> collections, String afterCursor) {
        this.catalogType = catalogType;
        this.collections = collections == null ? List.of() : collections;
        this.afterCursor = afterCursor;
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
     * Returns the collections on this page.
     *
     * @return an unmodifiable view of the collections; never {@code null}, possibly empty
     */
    public List<BusinessProductCollection> collections() {
        return Collections.unmodifiableList(collections);
    }

    /**
     * Returns the cursor that walks toward the next page of collections.
     *
     * @return an {@code Optional} carrying the cursor, or empty when this is the last page
     */
    public Optional<String> afterCursor() {
        return afterCursor == null || afterCursor.isEmpty() ? Optional.empty() : Optional.of(afterCursor);
    }
}
