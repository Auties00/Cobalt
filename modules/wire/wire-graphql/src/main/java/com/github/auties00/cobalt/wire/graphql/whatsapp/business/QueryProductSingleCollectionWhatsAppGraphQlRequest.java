package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the graph.whatsapp.com GraphQL query that fetches a single product collection and one page of its products for a
 * business catalog.
 *
 * <p>The single {@code request} GraphQL variable wraps a {@code collection} object naming the
 * business catalog {@link Jid}, the collection id, the page size and forward cursor, the requested
 * image dimensions, and the variant projection controls. WhatsApp Web's
 * {@code WAWebQueryProductSingleCollection} builds it from the catalog wid, the collection id, the
 * page size and after-cursor, the rendered image width and height (stringified pixels), an optional
 * direct-connection encrypted-info blob, a comma-separated {@code variant_info_fields} selection
 * string, and the optional variant thumbnail dimensions. The graph.whatsapp.com endpoint returns the collection and its
 * paging cursor under {@code xwa_product_catalog_get_single_collection}; the reply is consumed
 * through {@link QueryProductSingleCollectionWhatsAppGraphQlResponse}.
 *
 * @see QueryProductSingleCollectionWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebQueryProductSingleCollectionQuery")
public final class QueryProductSingleCollectionWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductSingleCollectionQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9546992575408789";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductSingleCollectionQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebQueryProductSingleCollectionQuery";

    /**
     * The {@code biz_jid} field of the {@code collection} object naming the business catalog to
     * query, or {@code null} to omit it.
     */
    private final Jid catalogJid;

    /**
     * The {@code id} field of the {@code collection} object naming the collection to fetch, or
     * {@code null} to omit it.
     */
    private final String collectionId;

    /**
     * The {@code limit} field of the {@code collection} object holding the requested page size, or
     * {@code null} to omit it.
     */
    private final Integer limit;

    /**
     * The {@code after} field of the {@code collection} object holding the forward pagination cursor,
     * or {@code null} to omit it.
     */
    private final String afterCursor;

    /**
     * The {@code width} field of the {@code collection} object holding the requested image width in
     * pixels, or {@code null} to omit it.
     */
    private final Integer width;

    /**
     * The {@code height} field of the {@code collection} object holding the requested image height in
     * pixels, or {@code null} to omit it.
     */
    private final Integer height;

    /**
     * The {@code direct_connection_encrypted_info} field of the {@code collection} object carrying
     * the merchant direct-connection encrypted-info blob, or {@code null} to omit it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The {@code variant_info_fields} field of the {@code collection} object holding the
     * comma-separated variant projection selection, or {@code null} to omit it.
     *
     * <p>WhatsApp Web sends the literal {@code "listing_details,types,availability,variant_properties"}
     * to request the full variant projection.
     */
    private final String variantInfoFields;

    /**
     * The {@code variant_thumbnail_height} field of the {@code collection} object holding the
     * requested variant thumbnail height in pixels, or {@code null} to omit it.
     */
    private final Integer variantThumbnailHeight;

    /**
     * The {@code variant_thumbnail_width} field of the {@code collection} object holding the
     * requested variant thumbnail width in pixels, or {@code null} to omit it.
     */
    private final Integer variantThumbnailWidth;

    /**
     * Constructs a fetch-single-collection request.
     *
     * <p>The {@code catalogJid} names the business catalog and {@code collectionId} the collection to
     * fetch. The {@code limit} and {@code afterCursor} drive forward pagination. The {@code width} and
     * {@code height} are the requested product image dimensions in pixels. The
     * {@code directConnectionEncryptedInfo} carries the merchant direct-connection blob. The
     * {@code variantInfoFields} selects the variant projection (the literal
     * {@code "listing_details,types,availability,variant_properties"} for the full projection), and
     * {@code variantThumbnailHeight}/{@code variantThumbnailWidth} size the variant thumbnails. Each
     * value that is {@code null} is omitted from the serialized object; the pixel dimensions are
     * serialized as their decimal strings.
     *
     * @param catalogJid                    the business catalog {@link Jid} to query, or {@code null}
     *                                      to omit the field
     * @param collectionId                  the collection id to fetch, or {@code null} to omit the
     *                                      field
     * @param limit                         the requested page size, or {@code null} to omit the field
     * @param afterCursor                   the forward pagination cursor, or {@code null} to omit the
     *                                      field
     * @param width                         the requested image width in pixels, or {@code null} to
     *                                      omit the field
     * @param height                        the requested image height in pixels, or {@code null} to
     *                                      omit the field
     * @param directConnectionEncryptedInfo the merchant direct-connection encrypted-info blob, or
     *                                      {@code null} to omit the field
     * @param variantInfoFields             the comma-separated variant projection selection, or
     *                                      {@code null} to omit the field
     * @param variantThumbnailHeight        the requested variant thumbnail height in pixels, or
     *                                      {@code null} to omit the field
     * @param variantThumbnailWidth         the requested variant thumbnail width in pixels, or
     *                                      {@code null} to omit the field
     */
    public QueryProductSingleCollectionWhatsAppGraphQlRequest(Jid catalogJid, String collectionId, Integer limit, String afterCursor, Integer width, Integer height, String directConnectionEncryptedInfo, String variantInfoFields, Integer variantThumbnailHeight, Integer variantThumbnailWidth) {
        this.catalogJid = catalogJid;
        this.collectionId = collectionId;
        this.limit = limit;
        this.afterCursor = afterCursor;
        this.width = width;
        this.height = height;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
        this.variantInfoFields = variantInfoFields;
        this.variantThumbnailHeight = variantThumbnailHeight;
        this.variantThumbnailWidth = variantThumbnailWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits
     * {@code {"request": {"collection": {"biz_jid": <jid>, "id": <id>, "limit": <limit>, "after":
     * <after>, "width": <width>, "height": <height>, "direct_connection_encrypted_info": <info>,
     * "variant_info_fields": <fields>, "variant_thumbnail_height": <h>, "variant_thumbnail_width":
     * <w>}}}}, writing each field only when its value is non-null and rendering every integer pixel
     * dimension and the {@code limit} as a decimal string.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductSingleCollection", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("collection");
            writer.writeColon();
            writer.startObject();
            if (catalogJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(catalogJid.toString());
            }

            if (collectionId != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(collectionId);
            }

            if (limit != null) {
                writer.writeName("limit");
                writer.writeColon();
                writer.writeString(String.valueOf(limit));
            }

            if (afterCursor != null) {
                writer.writeName("after");
                writer.writeColon();
                writer.writeString(afterCursor);
            }

            if (width != null) {
                writer.writeName("width");
                writer.writeColon();
                writer.writeString(String.valueOf(width));
            }

            if (height != null) {
                writer.writeName("height");
                writer.writeColon();
                writer.writeString(String.valueOf(height));
            }

            if (directConnectionEncryptedInfo != null) {
                writer.writeName("direct_connection_encrypted_info");
                writer.writeColon();
                writer.writeString(directConnectionEncryptedInfo);
            }

            if (variantInfoFields != null) {
                writer.writeName("variant_info_fields");
                writer.writeColon();
                writer.writeString(variantInfoFields);
            }

            if (variantThumbnailHeight != null) {
                writer.writeName("variant_thumbnail_height");
                writer.writeColon();
                writer.writeString(String.valueOf(variantThumbnailHeight));
            }

            if (variantThumbnailWidth != null) {
                writer.writeName("variant_thumbnail_width");
                writer.writeColon();
                writer.writeString(String.valueOf(variantThumbnailWidth));
            }
            writer.endObject();
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
