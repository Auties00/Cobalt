package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One named group of products inside a WhatsApp Business catalog.
 *
 * <p>Merchants organize their catalog into collections (for example "Summer
 * Sale" or "Electronics") so customers browse a structured storefront rather
 * than a flat product list. Each collection has a server-assigned id, a
 * display name, the products it contains as full {@link BusinessProduct}
 * entries, and the moderation outcome WhatsApp's policy review last assigned
 * to it.
 *
 * <p>When a single collection is fetched on its own, its products can span
 * more than one page; {@link #afterCursor()} then carries the cursor for the
 * next page of products, and is empty when no further page exists or when the
 * collection arrived as part of a larger listing that does not page its
 * products individually.
 */
@ProtobufMessage
public final class BusinessProductCollection {
    /**
     * Server-assigned identifier of this collection. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Display name of this collection as shown to customers. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Products contained in this collection, in server order. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<BusinessProduct> products;

    /**
     * WhatsApp policy moderation outcome for this collection (for example
     * {@code "APPROVED"}, {@code "PENDING"}, {@code "REJECTED"}). Empty when
     * the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String moderationStatus;

    /**
     * Whether the collection currently qualifies for an enforcement appeal,
     * typically relevant only when {@link #moderationStatus()} is rejected.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean canAppeal;

    /**
     * Public storefront URL for this collection. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String commerceUrl;

    /**
     * Human-readable reason the collection was rejected in review. Empty when
     * the collection was not rejected or the server omitted the reason.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String rejectReason;

    /**
     * Cursor for the next page of this collection's products. Empty when no
     * further page exists or when products were not paged individually.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String afterCursor;

    /**
     * Constructs a new {@code BusinessProductCollection}. Every scalar may be
     * {@code null} when the server omitted it; a {@code null} product list is
     * coerced to an empty list.
     *
     * @param id               the collection identifier, or {@code null}
     * @param name             the collection display name, or {@code null}
     * @param products         the products in the collection; {@code null} treated as empty
     * @param moderationStatus the moderation outcome, or {@code null}
     * @param canAppeal        the appealable flag
     * @param commerceUrl      the public storefront URL, or {@code null}
     * @param rejectReason     the rejection reason, or {@code null}
     * @param afterCursor      the next-page cursor, or {@code null}
     */
    BusinessProductCollection(String id, String name, List<BusinessProduct> products,
                              String moderationStatus, boolean canAppeal, String commerceUrl,
                              String rejectReason, String afterCursor) {
        this.id = id;
        this.name = name;
        this.products = products == null ? List.of() : products;
        this.moderationStatus = moderationStatus;
        this.canAppeal = canAppeal;
        this.commerceUrl = commerceUrl;
        this.rejectReason = rejectReason;
        this.afterCursor = afterCursor;
    }

    /**
     * Returns the collection identifier.
     *
     * @return an {@code Optional} carrying the id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the collection display name.
     *
     * @return an {@code Optional} carrying the name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the products contained in this collection.
     *
     * @return an unmodifiable view of the products; never {@code null}, possibly empty
     */
    public List<BusinessProduct> products() {
        return Collections.unmodifiableList(products);
    }

    /**
     * Returns the WhatsApp policy moderation outcome for this collection.
     *
     * @return an {@code Optional} carrying the status, or empty when the server omitted it
     */
    public Optional<String> moderationStatus() {
        return Optional.ofNullable(moderationStatus);
    }

    /**
     * Returns whether the collection currently qualifies for an enforcement
     * appeal.
     *
     * @return {@code true} when an appeal is permitted
     */
    public boolean canAppeal() {
        return canAppeal;
    }

    /**
     * Returns the public storefront URL for this collection.
     *
     * @return an {@code Optional} carrying the URL, or empty when the server omitted it
     */
    public Optional<String> commerceUrl() {
        return Optional.ofNullable(commerceUrl);
    }

    /**
     * Returns the reason the collection was rejected in review.
     *
     * @return an {@code Optional} carrying the reason, or empty when not rejected or omitted
     */
    public Optional<String> rejectReason() {
        return Optional.ofNullable(rejectReason);
    }

    /**
     * Returns the cursor for the next page of this collection's products.
     *
     * @return an {@code Optional} carrying the cursor, or empty when no further page exists
     */
    public Optional<String> afterCursor() {
        return afterCursor == null || afterCursor.isEmpty() ? Optional.empty() : Optional.of(afterCursor);
    }
}
