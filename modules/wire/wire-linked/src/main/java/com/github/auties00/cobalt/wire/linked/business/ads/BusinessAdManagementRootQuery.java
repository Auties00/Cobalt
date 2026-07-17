package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Input model for the management-dashboard query of the Click-to-WhatsApp
 * ad flow.
 *
 * <p>A Click-to-WhatsApp ad is a paid promotion that opens a chat with the
 * business when tapped. The management dashboard surfaces the linked
 * page, the most recent unpublished draft, and a paged list of the
 * merchant's running and past ads. This input carries the parameters the
 * server uses to resolve those values.
 *
 * <p>The {@link #primaryPageId() primary page id} drives the linked-page
 * lookup and filters the boosted-ads list; the
 * {@link #secondaryPageId() secondary page id} narrows the boosted-ads
 * list further when both a Facebook page and a WhatsApp Business page are
 * linked. The {@link #draftPageId() draft page id} selects the page whose
 * most recent unpublished draft is read. The
 * {@link #pageSize() page size} and {@link #afterCursor() cursor} window
 * the boosted-ads list.
 */
@ProtobufMessage(name = "BusinessAdManagementRootQuery")
public final class BusinessAdManagementRootQuery {
    /**
     * Primary page identifier. The wire-level variable name is
     * {@code page_id_1}; in WhatsApp Web it is resolved from the
     * merchant's linked Facebook page id when present, falling back to
     * the linked WhatsApp Business page id otherwise. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String primaryPageId;

    /**
     * Secondary page identifier. The wire-level variable name is
     * {@code page_id_2}; in WhatsApp Web it is set to the linked
     * WhatsApp Business page id only when a Facebook page is also linked,
     * so it narrows the boosted-ads list to ads tied to both pages.
     * Unset omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String secondaryPageId;

    /**
     * Identifier of the page whose most recent unpublished draft is
     * read. Unset omits the variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String draftPageId;

    /**
     * Maximum number of boosted-ad entries to return in the forward
     * page. Mirrors the standard forward-pagination count. Unset omits
     * the variable.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    final Integer pageSize;

    /**
     * Opaque forward-pagination cursor to fetch the next page of
     * boosted ads from. Mirrors the standard forward-pagination cursor.
     * Unset omits the variable.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String afterCursor;

    /**
     * Constructs a new {@code BusinessAdManagementRootQuery}. Every
     * argument may be {@code null} to omit the corresponding variable from
     * the request.
     *
     * @param primaryPageId   the primary page identifier, or {@code null}
     * @param secondaryPageId the secondary page identifier, or
     *                        {@code null}
     * @param draftPageId     the draft page identifier, or {@code null}
     * @param pageSize        the forward-pagination count, or {@code null}
     * @param afterCursor     the forward-pagination cursor, or {@code null}
     */
    public BusinessAdManagementRootQuery(String primaryPageId, String secondaryPageId, String draftPageId,
                                         Integer pageSize, String afterCursor) {
        this.primaryPageId = primaryPageId;
        this.secondaryPageId = secondaryPageId;
        this.draftPageId = draftPageId;
        this.pageSize = pageSize;
        this.afterCursor = afterCursor;
    }

    /**
     * Returns the primary page identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> primaryPageId() {
        return Optional.ofNullable(primaryPageId);
    }

    /**
     * Returns the secondary page identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> secondaryPageId() {
        return Optional.ofNullable(secondaryPageId);
    }

    /**
     * Returns the draft page identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> draftPageId() {
        return Optional.ofNullable(draftPageId);
    }

    /**
     * Returns the forward-pagination page size.
     *
     * @return an {@link OptionalInt} carrying the size, or empty when
     *         unset
     */
    public OptionalInt pageSize() {
        return pageSize == null ? OptionalInt.empty() : OptionalInt.of(pageSize);
    }

    /**
     * Returns the forward-pagination cursor.
     *
     * @return an {@link Optional} carrying the cursor, or empty when
     *         unset
     */
    public Optional<String> afterCursor() {
        return Optional.ofNullable(afterCursor);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAdManagementRootQuery) obj;
        return Objects.equals(primaryPageId, that.primaryPageId)
                && Objects.equals(secondaryPageId, that.secondaryPageId)
                && Objects.equals(draftPageId, that.draftPageId)
                && Objects.equals(pageSize, that.pageSize)
                && Objects.equals(afterCursor, that.afterCursor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryPageId, secondaryPageId, draftPageId, pageSize,
                afterCursor);
    }

    @Override
    public String toString() {
        return "BusinessAdManagementRootQuery[" +
                "primaryPageId=" + primaryPageId + ", " +
                "secondaryPageId=" + secondaryPageId + ", " +
                "draftPageId=" + draftPageId + ", " +
                "pageSize=" + pageSize + ", " +
                "afterCursor=" + afterCursor + ']';
    }
}
