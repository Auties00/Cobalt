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
 * Builds the Facebook GraphQL query that fetches a page of a WhatsApp Business catalog's collections.
 *
 * <p>The query takes one {@code request} GraphQL variable mapped onto the
 * {@code xfb_whatsapp_catalog_collections} field's {@code request} argument. WhatsApp Web's
 * {@code WAWebBizCatalogManagementFetchCollections.fetchCollections(request)} forwards the object
 * built by {@code WAWebQueryProductCollections} as {@code {collections: {biz_jid, after,
 * collection_limit, item_limit, width, height, direct_connection_encrypted_info, variant_info_fields,
 * variant_thumbnail_height, variant_thumbnail_width}}}. The {@code biz_jid} is the catalog-owner
 * {@link Jid}; {@code after} is the forward pagination cursor; {@code collection_limit} caps the
 * number of collections and {@code item_limit} caps the products per collection (both serialized as
 * decimal strings); {@code width} and {@code height} are render sizes; the remaining fields tune
 * direct-connection routing and the variant-thumbnail projection. The Meta graph endpoint returns the collections
 * page under {@code xfb_whatsapp_catalog_collections}; the reply is consumed through
 * {@link BizCatalogManagementFetchCollectionsFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementFetchCollectionsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchCollectionsQuery")
public final class BizCatalogManagementFetchCollectionsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchCollectionsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9687699931342731";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchCollectionsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementFetchCollectionsQuery";

    /**
     * The {@code collections.biz_jid} field naming the business account that owns the catalog, or
     * {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code collections.after} forward pagination cursor, or {@code null} to omit it.
     */
    private final String after;

    /**
     * The {@code collections.collection_limit} cap on returned collections serialized as a decimal
     * string, or {@code null} to omit it.
     */
    private final String collectionLimit;

    /**
     * The {@code collections.item_limit} cap on products per collection serialized as a decimal string,
     * or {@code null} to omit it.
     */
    private final String itemLimit;

    /**
     * The {@code collections.width} requested image width serialized as a decimal string, or
     * {@code null} to omit it.
     */
    private final String width;

    /**
     * The {@code collections.height} requested image height serialized as a decimal string, or
     * {@code null} to omit it.
     */
    private final String height;

    /**
     * The {@code collections.direct_connection_encrypted_info} routing blob, or {@code null} to omit
     * it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The {@code collections.variant_info_fields} selector naming which variant fields to project, or
     * {@code null} to omit it.
     */
    private final String variantInfoFields;

    /**
     * The {@code collections.variant_thumbnail_height} serialized as a decimal string, or {@code null}
     * to omit it.
     */
    private final String variantThumbnailHeight;

    /**
     * The {@code collections.variant_thumbnail_width} serialized as a decimal string, or {@code null}
     * to omit it.
     */
    private final String variantThumbnailWidth;

    /**
     * Constructs a fetch-collections query request.
     *
     * <p>All values populate the nested {@code collections} GraphQL object; each value that is
     * {@code null} is omitted from the serialized object.
     *
     * @param bizJid                        the catalog-owner {@link Jid}, or {@code null} to omit the
     *                                      field
     * @param after                         the forward pagination cursor, or {@code null} to omit the
     *                                      field
     * @param collectionLimit               the cap on returned collections, or {@code null} to omit
     *                                      the field
     * @param itemLimit                     the cap on products per collection, or {@code null} to omit
     *                                      the field
     * @param width                         the requested image width, or {@code null} to omit the field
     * @param height                        the requested image height, or {@code null} to omit the
     *                                      field
     * @param directConnectionEncryptedInfo the direct-connection routing blob, or {@code null} to omit
     *                                      the field
     * @param variantInfoFields             the variant-fields selector, or {@code null} to omit the
     *                                      field
     * @param variantThumbnailHeight        the variant thumbnail height, or {@code null} to omit the
     *                                      field
     * @param variantThumbnailWidth         the variant thumbnail width, or {@code null} to omit the
     *                                      field
     */
    public BizCatalogManagementFetchCollectionsFacebookGraphQlRequest(Jid bizJid, String after, String collectionLimit, String itemLimit, String width, String height, String directConnectionEncryptedInfo, String variantInfoFields, String variantThumbnailHeight, String variantThumbnailWidth) {
        this.bizJid = bizJid;
        this.after = after;
        this.collectionLimit = collectionLimit;
        this.itemLimit = itemLimit;
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
     * @implNote This implementation emits {@code {"request": {"collections": {...}}}}, writing each
     * field only when its value is non-null. The {@code biz_jid} is rendered as its canonical
     * {@link Jid} string.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchCollections", exports = "fetchCollections",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("collections");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            if (after != null) {
                writer.writeName("after");
                writer.writeColon();
                writer.writeString(after);
            }

            if (collectionLimit != null) {
                writer.writeName("collection_limit");
                writer.writeColon();
                writer.writeString(collectionLimit);
            }

            if (itemLimit != null) {
                writer.writeName("item_limit");
                writer.writeColon();
                writer.writeString(itemLimit);
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
