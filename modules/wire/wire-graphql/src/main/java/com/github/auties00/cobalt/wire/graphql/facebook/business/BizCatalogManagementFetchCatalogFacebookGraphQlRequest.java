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
 * Builds the Facebook GraphQL query that fetches a page of a WhatsApp Business catalog's products.
 *
 * <p>The query takes one {@code request} GraphQL variable mapped onto the {@code xfb_whatsapp_catalog}
 * field's {@code request} argument. WhatsApp Web's
 * {@code WAWebBizCatalogManagementFetchCatalog.fetchCatalog(request)} forwards the object built by
 * {@code WAWebQueryCatalog} as {@code {product_catalog: {jid, after, limit, width, height, belongs_to,
 * allow_shop_source, direct_connection_encrypted_info, variant_info_fields, variant_thumbnail_height,
 * variant_thumbnail_width}, platform}}. The {@code jid} is the catalog-owner {@link Jid}; {@code after}
 * is the forward pagination cursor; {@code limit}, {@code width}, and {@code height} are render and
 * paging sizes serialized as decimal strings; {@code belongs_to.collection_id} restricts the page to
 * one collection; the remaining fields tune shop-source visibility, direct-connection routing, and the
 * variant-thumbnail projection. The Meta graph endpoint returns the catalog page under {@code xfb_whatsapp_catalog};
 * the reply is consumed through {@link BizCatalogManagementFetchCatalogFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementFetchCatalogFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchCatalogQuery")
public final class BizCatalogManagementFetchCatalogFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchCatalogQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9957894520961099";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchCatalogQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementFetchCatalogQuery";

    /**
     * The {@code product_catalog.jid} field naming the business account that owns the catalog, or
     * {@code null} to omit it.
     */
    private final Jid jid;

    /**
     * The {@code product_catalog.after} forward pagination cursor, or {@code null} to omit it.
     */
    private final String after;

    /**
     * The {@code product_catalog.limit} page size serialized as a decimal string, or {@code null} to
     * omit it.
     */
    private final String limit;

    /**
     * The {@code product_catalog.width} requested image width serialized as a decimal string, or
     * {@code null} to omit it.
     */
    private final String width;

    /**
     * The {@code product_catalog.height} requested image height serialized as a decimal string, or
     * {@code null} to omit it.
     */
    private final String height;

    /**
     * The {@code product_catalog.belongs_to.collection_id} restricting the page to one collection, or
     * {@code null} to omit the {@code belongs_to} object.
     */
    private final String collectionId;

    /**
     * The {@code product_catalog.allow_shop_source} flag toggling shop-source visibility, or
     * {@code null} to omit it.
     */
    private final Boolean allowShopSource;

    /**
     * The {@code product_catalog.direct_connection_encrypted_info} routing blob, or {@code null} to
     * omit it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The {@code product_catalog.variant_info_fields} selector naming which variant fields to project,
     * or {@code null} to omit it.
     */
    private final String variantInfoFields;

    /**
     * The {@code product_catalog.variant_thumbnail_height} serialized as a decimal string, or
     * {@code null} to omit it.
     */
    private final String variantThumbnailHeight;

    /**
     * The {@code product_catalog.variant_thumbnail_width} serialized as a decimal string, or
     * {@code null} to omit it.
     */
    private final String variantThumbnailWidth;

    /**
     * The top-level {@code platform} field naming the requesting platform, or {@code null} to omit it.
     */
    private final String platform;

    /**
     * Constructs a fetch-catalog query request.
     *
     * <p>The {@code jid}, {@code after}, {@code limit}, {@code width}, {@code height},
     * {@code collectionId}, {@code allowShopSource}, {@code directConnectionEncryptedInfo},
     * {@code variantInfoFields}, {@code variantThumbnailHeight}, and {@code variantThumbnailWidth}
     * values populate the nested {@code product_catalog} object; {@code platform} is a top-level
     * sibling. Each value that is {@code null} is omitted from the serialized object, and a
     * {@code null} {@code collectionId} omits the whole {@code belongs_to} object.
     *
     * @param jid                           the catalog-owner {@link Jid}, or {@code null} to omit the
     *                                      field
     * @param after                         the forward pagination cursor, or {@code null} to omit the
     *                                      field
     * @param limit                         the page size, or {@code null} to omit the field
     * @param width                         the requested image width, or {@code null} to omit the field
     * @param height                        the requested image height, or {@code null} to omit the
     *                                      field
     * @param collectionId                  the collection filter, or {@code null} to omit the
     *                                      {@code belongs_to} object
     * @param allowShopSource               the shop-source visibility flag, or {@code null} to omit the
     *                                      field
     * @param directConnectionEncryptedInfo the direct-connection routing blob, or {@code null} to omit
     *                                      the field
     * @param variantInfoFields             the variant-fields selector, or {@code null} to omit the
     *                                      field
     * @param variantThumbnailHeight        the variant thumbnail height, or {@code null} to omit the
     *                                      field
     * @param variantThumbnailWidth         the variant thumbnail width, or {@code null} to omit the
     *                                      field
     * @param platform                      the requesting platform, or {@code null} to omit the field
     */
    public BizCatalogManagementFetchCatalogFacebookGraphQlRequest(Jid jid, String after, String limit, String width, String height, String collectionId, Boolean allowShopSource, String directConnectionEncryptedInfo, String variantInfoFields, String variantThumbnailHeight, String variantThumbnailWidth, String platform) {
        this.jid = jid;
        this.after = after;
        this.limit = limit;
        this.width = width;
        this.height = height;
        this.collectionId = collectionId;
        this.allowShopSource = allowShopSource;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
        this.variantInfoFields = variantInfoFields;
        this.variantThumbnailHeight = variantThumbnailHeight;
        this.variantThumbnailWidth = variantThumbnailWidth;
        this.platform = platform;
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
     * {@code {"request": {"product_catalog": {...}, "platform": <platform>}}}, writing each field only
     * when its value is non-null and omitting the {@code belongs_to} object entirely when
     * {@code collectionId} is {@code null}. The {@code jid} is rendered as its canonical {@link Jid}
     * string.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchCatalog", exports = "fetchCatalog",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("product_catalog");
            writer.writeColon();
            writer.startObject();
            if (jid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(jid.toString());
            }

            if (after != null) {
                writer.writeName("after");
                writer.writeColon();
                writer.writeString(after);
            }

            if (limit != null) {
                writer.writeName("limit");
                writer.writeColon();
                writer.writeString(limit);
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

            if (collectionId != null) {
                writer.writeName("belongs_to");
                writer.writeColon();
                writer.startObject();
                writer.writeName("collection_id");
                writer.writeColon();
                writer.writeString(collectionId);
                writer.endObject();
            }

            if (allowShopSource != null) {
                writer.writeName("allow_shop_source");
                writer.writeColon();
                writer.writeBool(allowShopSource);
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

            if (platform != null) {
                writer.writeName("platform");
                writer.writeColon();
                writer.writeString(platform);
            }
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
