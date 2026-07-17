package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that fetches a single WhatsApp Business catalog collection and one page of
 * its products.
 *
 * <p>The single {@code request} GraphQL variable wraps a {@code collection} object that
 * {@code WAWebBizCatalogManagementFetchSingleCollection.fetchSingleCollection} fills with the
 * owning business {@code biz_jid}, the collection {@code id}, the page {@code limit}, the
 * {@code after} forward cursor, the requested thumbnail {@code width} and {@code height}, an optional
 * {@code direct_connection_encrypted_info} blob, and the variant projection knobs
 * ({@code variant_info_fields}, {@code variant_thumbnail_height}, {@code variant_thumbnail_width}).
 * The numeric dimensions and the page limit travel on the wire as strings. The Meta graph endpoint returns the
 * collection under {@code xfb_whatsapp_catalog_collection}; the reply is consumed through
 * {@link BizCatalogManagementFetchSingleCollectionFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementFetchSingleCollectionFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchSingleCollectionQuery")
public final class BizCatalogManagementFetchSingleCollectionFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchSingleCollectionQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9547298772047931";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchSingleCollectionQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementFetchSingleCollectionQuery";

    /**
     * The {@code collection.biz_jid} field naming the business account that owns the collection, or
     * {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code collection.id} field identifying the collection to fetch, or {@code null} to omit it.
     *
     * <p>This is an opaque collection identifier rather than a WhatsApp address, so it is kept as a
     * plain {@link String}.
     */
    private final String id;

    /**
     * The {@code collection.limit} field bounding the number of products in this page, or
     * {@code null} to omit it.
     *
     * <p>Serialized verbatim as a string, matching {@code String(limit)} in the source.
     */
    private final String limit;

    /**
     * The {@code collection.after} forward cursor selecting the page to fetch, or {@code null} to omit
     * it.
     */
    private final String after;

    /**
     * The {@code collection.width} field carrying the requested thumbnail width, or {@code null} to
     * omit it.
     *
     * <p>Serialized verbatim as a string, matching {@code String(width)} in the source.
     */
    private final String width;

    /**
     * The {@code collection.height} field carrying the requested thumbnail height, or {@code null} to
     * omit it.
     *
     * <p>Serialized verbatim as a string, matching {@code String(height)} in the source.
     */
    private final String height;

    /**
     * The {@code collection.direct_connection_encrypted_info} blob authorizing a direct-connection
     * fetch, or {@code null} to omit it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The {@code collection.variant_info_fields} selector listing the variant sub-projections to
     * fetch, or {@code null} to omit it.
     *
     * <p>A single comma-separated token (for example
     * {@code "listing_details,types,availability,variant_properties"}), not a JSON array.
     */
    private final String variantInfoFields;

    /**
     * The {@code collection.variant_thumbnail_height} field carrying the requested variant-thumbnail
     * height, or {@code null} to omit it.
     *
     * <p>Serialized verbatim as a string.
     */
    private final String variantThumbnailHeight;

    /**
     * The {@code collection.variant_thumbnail_width} field carrying the requested variant-thumbnail
     * width, or {@code null} to omit it.
     *
     * <p>Serialized verbatim as a string.
     */
    private final String variantThumbnailWidth;

    /**
     * Constructs a fetch-single-collection query request carrying the collection owner and identity,
     * the paging window, and the thumbnail and variant projection knobs.
     *
     * <p>All values populate the {@code request.collection} GraphQL object; each value that is
     * {@code null} is omitted from the serialized object.
     *
     * @param bizJid                        the collection-owning business {@link Jid}, or {@code null}
     *                                      to omit the field
     * @param id                            the collection identifier to fetch, or {@code null} to omit
     *                                      the field
     * @param limit                         the page size, or {@code null} to omit the field
     * @param after                         the forward paging cursor, or {@code null} to omit the
     *                                      field
     * @param width                         the requested thumbnail width, or {@code null} to omit the
     *                                      field
     * @param height                        the requested thumbnail height, or {@code null} to omit
     *                                      the field
     * @param directConnectionEncryptedInfo the direct-connection authorization blob, or {@code null}
     *                                      to omit the field
     * @param variantInfoFields             the comma-separated variant projection selector, or
     *                                      {@code null} to omit the field
     * @param variantThumbnailHeight        the requested variant-thumbnail height, or {@code null} to
     *                                      omit the field
     * @param variantThumbnailWidth         the requested variant-thumbnail width, or {@code null} to
     *                                      omit the field
     */
    public BizCatalogManagementFetchSingleCollectionFacebookGraphQlRequest(Jid bizJid, String id, String limit, String after, String width, String height, String directConnectionEncryptedInfo, String variantInfoFields, String variantThumbnailHeight, String variantThumbnailWidth) {
        this.bizJid = bizJid;
        this.id = id;
        this.limit = limit;
        this.after = after;
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
     * @implNote This implementation emits {@code {"request": {"collection": {...}}}}, writing each
     * {@code collection} field only when its value is non-null, rendering {@code biz_jid} as its
     * canonical string, and emitting an empty {@code collection} object when every field is
     * {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchSingleCollection", exports = "fetchSingleCollection",
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
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            if (id != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(id);
            }

            if (limit != null) {
                writer.writeName("limit");
                writer.writeColon();
                writer.writeString(limit);
            }

            if (after != null) {
                writer.writeName("after");
                writer.writeColon();
                writer.writeString(after);
            }

            if (width != null) {
                writer.writeName("width");
                writer.writeColon();
                writer.writeString(width);
            }

            if (height != null) {
                writer.writeName("height");
                writer.writeColon();
                writer.writeString(height);
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
                writer.writeString(variantThumbnailHeight);
            }

            if (variantThumbnailWidth != null) {
                writer.writeName("variant_thumbnail_width");
                writer.writeColon();
                writer.writeString(variantThumbnailWidth);
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
}
