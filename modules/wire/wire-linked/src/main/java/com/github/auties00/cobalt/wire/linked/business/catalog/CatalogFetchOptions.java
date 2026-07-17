package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Tuning options shared by every WhatsApp Business catalog read.
 *
 * <p>When an app fetches a merchant's catalog, a single product, a page of
 * products, or one of the merchant's product collections, it can tune how
 * the server pages the result and how it renders the product imagery. This
 * model bundles all of those knobs in one place so every catalog read method
 * takes the same options object rather than a long list of loose arguments.
 *
 * <p>Every option is independently optional. An option left unset is simply
 * omitted from the request, which lets the server apply its own default. Build
 * an instance with the generated {@code CatalogFetchOptionsBuilder}; pass
 * {@link #empty()} to accept every server default. The options break down into
 * three groups:
 * <ul>
 *   <li><b>Pagination:</b> {@link #afterCursor()} continues a previous page and
 *       {@link #limit()} caps how many entries one page returns.</li>
 *   <li><b>Image rendering:</b> {@link #imageWidth()} and {@link #imageHeight()}
 *       request product thumbnails at a target pixel size, while
 *       {@link #variantThumbnailWidth()} and {@link #variantThumbnailHeight()}
 *       size the per-variant swatch thumbnails.</li>
 *   <li><b>Projection and routing:</b> {@link #collectionId()} restricts a
 *       catalog page to one collection, {@link #variantInfoFields()} selects
 *       which variant detail blocks to include, {@link #fetchComplianceInfo()}
 *       toggles the regulatory disclosure block, {@link #allowShopSource()}
 *       widens the result to shop-sourced products, {@link #platform()} tags the
 *       requesting platform, and {@link #catalogSessionId()} and
 *       {@link #directConnectionEncryptedInfo()} carry browse-session and direct
 *       merchant-connection context.</li>
 * </ul>
 */
@ProtobufMessage
public final class CatalogFetchOptions {
    /**
     * Shared instance carrying no options, so the server applies every default.
     */
    private static final CatalogFetchOptions EMPTY = new CatalogFetchOptions(
            null, null, null, null, null, null, null, null, null, null, null, null, null);

    /**
     * Opaque cursor that continues paging from where a previous read left off.
     * Unset starts from the first page.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String afterCursor;

    /**
     * Maximum number of entries one page returns. Unset lets the server pick a
     * default page size.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final Integer limit;

    /**
     * Requested width, in pixels, of the rendered product thumbnails. Unset
     * lets the server pick a default size.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final Integer imageWidth;

    /**
     * Requested height, in pixels, of the rendered product thumbnails. Unset
     * lets the server pick a default size.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final Integer imageHeight;

    /**
     * Identifier of a single collection to restrict a catalog page to. Unset
     * returns products across the whole catalog.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String collectionId;

    /**
     * Whether to widen the result to include products sourced from a linked
     * shop. Unset leaves the decision to the server.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final Boolean allowShopSource;

    /**
     * Opaque encrypted blob that routes the read through the merchant's direct
     * connection. Unset uses the standard relay path.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String directConnectionEncryptedInfo;

    /**
     * Comma-separated selector naming which variant detail blocks to project
     * (for example {@code "listing_details,types,availability,variant_properties"}).
     * Unset omits the variant projection.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String variantInfoFields;

    /**
     * Requested height, in pixels, of the per-variant swatch thumbnails. Unset
     * lets the server pick a default size.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT32)
    final Integer variantThumbnailHeight;

    /**
     * Requested width, in pixels, of the per-variant swatch thumbnails. Unset
     * lets the server pick a default size.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT32)
    final Integer variantThumbnailWidth;

    /**
     * Opaque identifier correlating a browse session across catalog reads.
     * Unset starts an anonymous read.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    final String catalogSessionId;

    /**
     * Whether to include the regulatory compliance disclosure block on each
     * product. Unset leaves the decision to the server.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    final Boolean fetchComplianceInfo;

    /**
     * Label naming the platform issuing the read. Unset omits the tag.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    final String platform;

    /**
     * Constructs a new {@code CatalogFetchOptions}. Every argument is optional
     * and may be {@code null} to accept the matching server default.
     *
     * @param afterCursor                   the pagination continuation cursor, or {@code null}
     * @param limit                         the maximum entries per page, or {@code null}
     * @param imageWidth                    the product thumbnail width in pixels, or {@code null}
     * @param imageHeight                   the product thumbnail height in pixels, or {@code null}
     * @param collectionId                  the single-collection filter id, or {@code null}
     * @param allowShopSource               the shop-source inclusion flag, or {@code null}
     * @param directConnectionEncryptedInfo the direct-connection routing blob, or {@code null}
     * @param variantInfoFields             the variant projection selector, or {@code null}
     * @param variantThumbnailHeight        the variant thumbnail height in pixels, or {@code null}
     * @param variantThumbnailWidth         the variant thumbnail width in pixels, or {@code null}
     * @param catalogSessionId              the browse-session id, or {@code null}
     * @param fetchComplianceInfo           the compliance-disclosure flag, or {@code null}
     * @param platform                      the requesting-platform label, or {@code null}
     */
    CatalogFetchOptions(String afterCursor, Integer limit, Integer imageWidth, Integer imageHeight,
                        String collectionId, Boolean allowShopSource, String directConnectionEncryptedInfo,
                        String variantInfoFields, Integer variantThumbnailHeight, Integer variantThumbnailWidth,
                        String catalogSessionId, Boolean fetchComplianceInfo, String platform) {
        this.afterCursor = afterCursor;
        this.limit = limit;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.collectionId = collectionId;
        this.allowShopSource = allowShopSource;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
        this.variantInfoFields = variantInfoFields;
        this.variantThumbnailHeight = variantThumbnailHeight;
        this.variantThumbnailWidth = variantThumbnailWidth;
        this.catalogSessionId = catalogSessionId;
        this.fetchComplianceInfo = fetchComplianceInfo;
        this.platform = platform;
    }

    /**
     * Returns a shared instance carrying no options.
     *
     * <p>Passing the returned value to a catalog read accepts every server
     * default for pagination, rendering, and projection.
     *
     * @return the empty options instance, never {@code null}
     */
    public static CatalogFetchOptions empty() {
        return EMPTY;
    }

    /**
     * Returns the pagination continuation cursor.
     *
     * @return an {@code Optional} carrying the cursor, or empty when unset
     */
    public Optional<String> afterCursor() {
        return Optional.ofNullable(afterCursor);
    }

    /**
     * Returns the maximum number of entries per page.
     *
     * @return an {@code OptionalInt} carrying the limit, or empty when unset
     */
    public OptionalInt limit() {
        return limit == null ? OptionalInt.empty() : OptionalInt.of(limit);
    }

    /**
     * Returns the requested product thumbnail width in pixels.
     *
     * @return an {@code OptionalInt} carrying the width, or empty when unset
     */
    public OptionalInt imageWidth() {
        return imageWidth == null ? OptionalInt.empty() : OptionalInt.of(imageWidth);
    }

    /**
     * Returns the requested product thumbnail height in pixels.
     *
     * @return an {@code OptionalInt} carrying the height, or empty when unset
     */
    public OptionalInt imageHeight() {
        return imageHeight == null ? OptionalInt.empty() : OptionalInt.of(imageHeight);
    }

    /**
     * Returns the single-collection filter id.
     *
     * @return an {@code Optional} carrying the collection id, or empty when unset
     */
    public Optional<String> collectionId() {
        return Optional.ofNullable(collectionId);
    }

    /**
     * Returns whether shop-sourced products are included in the result.
     *
     * @return an {@code Optional} carrying the flag, or empty when unset
     */
    public Optional<Boolean> allowShopSource() {
        return Optional.ofNullable(allowShopSource);
    }

    /**
     * Returns the direct-connection routing blob.
     *
     * @return an {@code Optional} carrying the blob, or empty when unset
     */
    public Optional<String> directConnectionEncryptedInfo() {
        return Optional.ofNullable(directConnectionEncryptedInfo);
    }

    /**
     * Returns the variant projection selector.
     *
     * @return an {@code Optional} carrying the selector, or empty when unset
     */
    public Optional<String> variantInfoFields() {
        return Optional.ofNullable(variantInfoFields);
    }

    /**
     * Returns the requested variant thumbnail height in pixels.
     *
     * @return an {@code OptionalInt} carrying the height, or empty when unset
     */
    public OptionalInt variantThumbnailHeight() {
        return variantThumbnailHeight == null ? OptionalInt.empty() : OptionalInt.of(variantThumbnailHeight);
    }

    /**
     * Returns the requested variant thumbnail width in pixels.
     *
     * @return an {@code OptionalInt} carrying the width, or empty when unset
     */
    public OptionalInt variantThumbnailWidth() {
        return variantThumbnailWidth == null ? OptionalInt.empty() : OptionalInt.of(variantThumbnailWidth);
    }

    /**
     * Returns the browse-session id.
     *
     * @return an {@code Optional} carrying the session id, or empty when unset
     */
    public Optional<String> catalogSessionId() {
        return Optional.ofNullable(catalogSessionId);
    }

    /**
     * Returns whether the regulatory compliance disclosure block is requested.
     *
     * @return an {@code Optional} carrying the flag, or empty when unset
     */
    public Optional<Boolean> fetchComplianceInfo() {
        return Optional.ofNullable(fetchComplianceInfo);
    }

    /**
     * Returns the requesting-platform label.
     *
     * @return an {@code Optional} carrying the label, or empty when unset
     */
    public Optional<String> platform() {
        return Optional.ofNullable(platform);
    }
}
