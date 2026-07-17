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
 * Builds the graph.whatsapp.com GraphQL query that tests whether a WhatsApp Business catalog exposes any product
 * categories.
 *
 * <p>The single {@code request} GraphQL variable nests a {@code categories} object that
 * {@code WAWebQueryCatalogHasCategories} fills with the catalog-owning business {@link Jid}, an
 * optional direct-connection blob, the image dimensions to request (defaulting to one hundred pixels
 * square), and an optional catalog session id. The graph.whatsapp.com endpoint returns the categories under
 * {@code xwa_product_catalog_get_categories}; WhatsApp Web reduces the reply to a single boolean
 * "has any category" verdict. The reply is consumed through
 * {@link QueryCatalogHasCategoriesWhatsAppGraphQlResponse}.
 *
 * @see QueryCatalogHasCategoriesWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebQueryCatalogHasCategoriesQuery")
public final class QueryCatalogHasCategoriesWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalogHasCategoriesQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9759957480718978";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalogHasCategoriesQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebQueryCatalogHasCategoriesQuery";

    /**
     * The default image dimension WhatsApp Web requests when the caller leaves a side unspecified.
     */
    private static final int DEFAULT_IMAGE_DIMENSION = 100;

    /**
     * The {@code biz_jid} field of the {@code categories} object naming the catalog-owning business
     * account, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The optional direct-connection encrypted payload produced by the business direct-connection
     * retry loop, or {@code null} when not used.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The requested image width in pixels, or {@code null} to fall back to
     * {@link #DEFAULT_IMAGE_DIMENSION}.
     */
    private final Integer imageWidth;

    /**
     * The requested image height in pixels, or {@code null} to fall back to
     * {@link #DEFAULT_IMAGE_DIMENSION}.
     */
    private final Integer imageHeight;

    /**
     * The optional catalog session id correlating a browse session, or {@code null} when not used.
     */
    private final String catalogSessionId;

    /**
     * Constructs a has-categories query request.
     *
     * <p>All values populate the nested {@code categories} object. The image dimensions are written
     * as a nested {@code image_dimensions} object with integer {@code width} and {@code height}
     * members; a {@code null} side falls back to {@link #DEFAULT_IMAGE_DIMENSION}, matching the
     * WhatsApp Web default of one hundred pixels square. The {@link Jid} is omitted when {@code null};
     * the direct-connection blob and session id are emitted as explicit JSON {@code null} when absent.
     *
     * @param bizJid                        the catalog-owning business account, or {@code null} to
     *                                      omit the field
     * @param directConnectionEncryptedInfo the optional direct-connection encrypted payload, or
     *                                      {@code null} when not used
     * @param imageWidth                    the requested image width in pixels, or {@code null} for
     *                                      the default
     * @param imageHeight                   the requested image height in pixels, or {@code null} for
     *                                      the default
     * @param catalogSessionId              the optional catalog session id, or {@code null} when not
     *                                      used
     */
    public QueryCatalogHasCategoriesWhatsAppGraphQlRequest(Jid bizJid, String directConnectionEncryptedInfo,
                                                              Integer imageWidth, Integer imageHeight, String catalogSessionId) {
        this.bizJid = bizJid;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.catalogSessionId = catalogSessionId;
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
     * {@code {"request": {"categories": {"biz_jid": ..., "direct_connection_encrypted_info": ...,
     * "image_dimensions": {"width": <int>, "height": <int>}, "catalog_session_id": ...}}}}, mirroring
     * the {@code xwa_product_catalog_get_categories} argument shape. Unlike the other catalog queries,
     * {@code image_dimensions} carries numeric, not stringified, dimensions and defaults each missing
     * side to {@value #DEFAULT_IMAGE_DIMENSION}. The {@link Jid} is omitted when {@code null}; the
     * direct-connection blob and session id are written as explicit JSON {@code null} when absent.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalogHasCategories", exports = "queryCatalogHasCategories",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("categories");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            writer.writeName("direct_connection_encrypted_info");
            writer.writeColon();
            if (directConnectionEncryptedInfo == null) {
                writer.writeNull();
            } else {
                writer.writeString(directConnectionEncryptedInfo);
            }

            writer.writeName("image_dimensions");
            writer.writeColon();
            writer.startObject();
            writer.writeName("width");
            writer.writeColon();
            writer.writeInt32(imageWidth == null ? DEFAULT_IMAGE_DIMENSION : imageWidth);
            writer.writeName("height");
            writer.writeColon();
            writer.writeInt32(imageHeight == null ? DEFAULT_IMAGE_DIMENSION : imageHeight);
            writer.endObject();

            writer.writeName("catalog_session_id");
            writer.writeColon();
            if (catalogSessionId == null) {
                writer.writeNull();
            } else {
                writer.writeString(catalogSessionId);
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
