package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The dashboard a merchant sees when managing their WhatsApp Business
 * advertisements.
 *
 * <p>When a merchant opens the management view for "Click-to-WhatsApp" ads (paid
 * promotions that open a chat with the business when tapped), the server returns
 * the data the dashboard needs: the business page header, the most recent
 * unpublished draft, and a page of the merchant's running and past ads. This
 * model gathers the fields an embedder needs to render that dashboard and page
 * through the ad list.
 *
 * <p>{@link #pageName()} and {@link #pageVerified()} describe the promoted page;
 * {@link #latestDraftId()} is the identifier of the most recent unpublished
 * draft, when one exists; {@link #boostedAdIds()} lists the identifiers of the
 * ads on the current page; {@link #hasNextPage()} reports whether more ads
 * follow; and {@link #endCursor()} is the cursor to fetch the next page.
 */
@ProtobufMessage(name = "BusinessAdManagementScreen")
public final class BusinessAdManagementScreen {
    /**
     * Display name of the business page being managed. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String pageName;

    /**
     * Whether the managed business page is verified. Reported by the server;
     * {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean pageVerified;

    /**
     * Identifier of the most recent unpublished draft, when one exists. Empty
     * when the server omitted it or there is no draft.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String latestDraftId;

    /**
     * Identifiers of the ads on the current page, in the order the server
     * returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> boostedAdIds;

    /**
     * Whether more ads follow the current page. Reported by the server;
     * {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean hasNextPage;

    /**
     * Cursor to fetch the next page of ads. Empty when the server omitted it or
     * no further page exists.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String endCursor;

    /**
     * Constructs a new {@code BusinessAdManagementScreen}. A {@code null}
     * {@code boostedAdIds} is coerced to an empty list, and the other reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param pageName      the managed page name, or {@code null}
     * @param pageVerified  whether the managed page is verified
     * @param latestDraftId the latest draft identifier, or {@code null}
     * @param boostedAdIds  the current-page ad identifiers; {@code null} treated as empty
     * @param hasNextPage   whether more ads follow the current page
     * @param endCursor     the next-page cursor, or {@code null}
     */
    BusinessAdManagementScreen(String pageName,
                               boolean pageVerified,
                               String latestDraftId,
                               List<String> boostedAdIds,
                               boolean hasNextPage,
                               String endCursor) {
        this.pageName = pageName;
        this.pageVerified = pageVerified;
        this.latestDraftId = latestDraftId;
        this.boostedAdIds = boostedAdIds == null ? List.of() : boostedAdIds;
        this.hasNextPage = hasNextPage;
        this.endCursor = endCursor;
    }

    /**
     * Returns the display name of the business page being managed.
     *
     * @return the managed page name, or empty when the server omitted it
     */
    public Optional<String> pageName() {
        return Optional.ofNullable(pageName);
    }

    /**
     * Returns whether the managed business page is verified.
     *
     * @return {@code true} when the page is verified, {@code false} otherwise
     */
    public boolean pageVerified() {
        return pageVerified;
    }

    /**
     * Returns the identifier of the most recent unpublished draft.
     *
     * @return the latest draft id, or empty when the server omitted it or there
     *         is no draft
     */
    public Optional<String> latestDraftId() {
        return Optional.ofNullable(latestDraftId);
    }

    /**
     * Returns the identifiers of the ads on the current page.
     *
     * @return an unmodifiable view of the current-page ad ids; never
     *         {@code null}, possibly empty
     */
    public List<String> boostedAdIds() {
        return Collections.unmodifiableList(boostedAdIds);
    }

    /**
     * Returns whether more ads follow the current page.
     *
     * @return {@code true} when a further page exists, {@code false} otherwise
     */
    public boolean hasNextPage() {
        return hasNextPage;
    }

    /**
     * Returns the cursor to fetch the next page of ads.
     *
     * @return the next-page cursor, or empty when the server omitted it or no
     *         further page exists
     */
    public Optional<String> endCursor() {
        return Optional.ofNullable(endCursor);
    }
}
