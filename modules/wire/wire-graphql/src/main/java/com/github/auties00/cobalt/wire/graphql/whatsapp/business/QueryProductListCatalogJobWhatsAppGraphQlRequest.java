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
import java.util.List;

/**
 * Builds the graph.whatsapp.com GraphQL query that fetches a list of catalog products for a business catalog by their
 * retailer product ids.
 *
 * <p>The single {@code request} GraphQL variable wraps a {@code product_list} object naming the
 * business catalog {@link Jid}, the explicit set of product ids to fetch, and the requested image
 * dimensions. WhatsApp Web's {@code WAWebQueryProductListCatalogJob} builds it from the catalog wid,
 * the product-id list, the rendered image width and height (stringified pixels), and an optional
 * direct-connection encrypted-info blob used for the merchant direct-connection path. The graph.whatsapp.com endpoint
 * returns the product list under {@code xwa_product_catalog_get_product_list}; the reply is consumed
 * through {@link QueryProductListCatalogJobWhatsAppGraphQlResponse}.
 *
 * @see QueryProductListCatalogJobWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebQueryProductListCatalogJobQuery")
public final class QueryProductListCatalogJobWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductListCatalogJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "30125049463760630";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductListCatalogJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebQueryProductListCatalogJobQuery";

    /**
     * The {@code jid} field of the {@code product_list} object naming the business catalog to query,
     * or {@code null} to omit it.
     */
    private final Jid catalogJid;

    /**
     * The {@code products} field of the {@code product_list} object holding the retailer product ids
     * to fetch, or {@code null} to omit it.
     *
     * <p>Each id is serialized as a {@code {"id": <value>}} object inside the {@code products} array.
     */
    private final List<String> productIds;

    /**
     * The {@code width} field of the {@code product_list} object holding the requested image width in
     * pixels, or {@code null} to omit it.
     */
    private final Integer width;

    /**
     * The {@code height} field of the {@code product_list} object holding the requested image height
     * in pixels, or {@code null} to omit it.
     */
    private final Integer height;

    /**
     * The {@code direct_connection_encrypted_info} field of the {@code product_list} object carrying
     * the merchant direct-connection encrypted-info blob, or {@code null} to omit it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * Constructs a fetch-product-list request.
     *
     * <p>The {@code catalogJid} names the business catalog. The {@code productIds} are the retailer
     * product ids to fetch. The {@code width} and {@code height} are the requested image dimensions in
     * pixels, serialized as their decimal strings. The {@code directConnectionEncryptedInfo} carries
     * the merchant direct-connection blob. Each value that is {@code null} is omitted from the
     * serialized object.
     *
     * @param catalogJid                    the business catalog {@link Jid} to query, or {@code null}
     *                                      to omit the field
     * @param productIds                    the retailer product ids to fetch, or {@code null} to omit
     *                                      the field
     * @param width                         the requested image width in pixels, or {@code null} to
     *                                      omit the field
     * @param height                        the requested image height in pixels, or {@code null} to
     *                                      omit the field
     * @param directConnectionEncryptedInfo the merchant direct-connection encrypted-info blob, or
     *                                      {@code null} to omit the field
     */
    public QueryProductListCatalogJobWhatsAppGraphQlRequest(Jid catalogJid, List<String> productIds, Integer width, Integer height, String directConnectionEncryptedInfo) {
        this.catalogJid = catalogJid;
        this.productIds = productIds;
        this.width = width;
        this.height = height;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
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
     * {@code {"request": {"product_list": {"jid": <jid>, "products": [{"id": <id>}, ...], "width":
     * <width>, "height": <height>, "direct_connection_encrypted_info": <info>}}}}, writing each field
     * only when its value is non-null, rendering {@code width} and {@code height} as their decimal
     * strings, and emitting empty enclosing objects when their fields are all {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductListCatalogJob", exports = "QueryProductListCatalog",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("product_list");
            writer.writeColon();
            writer.startObject();
            if (catalogJid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(catalogJid.toString());
            }

            if (productIds != null) {
                writer.writeName("products");
                writer.writeColon();
                writer.startArray();
                for (var productId : productIds) {
                    writer.startObject();
                    writer.writeName("id");
                    writer.writeColon();
                    writer.writeString(productId);
                    writer.endObject();
                }
                writer.endArray();
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
