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
import java.util.List;

/**
 * Builds the Facebook GraphQL query that fetches a list of WhatsApp Business catalog products by identifier.
 *
 * <p>The single {@code request} GraphQL variable wraps a {@code product_list} object that
 * {@code WAWebBizCatalogManagementFetchProductList.fetchProductList} fills with the catalog-owning
 * business {@code jid}, the {@code products} array (each element being an {@code {"id": ...}} object
 * naming a product to fetch), the requested thumbnail {@code width} and {@code height}, and an
 * optional {@code direct_connection_encrypted_info} blob. The numeric dimensions travel on the wire
 * as strings. The Meta graph endpoint returns the products under {@code xfb_whatsapp_catalog_product_list}; the
 * reply is consumed through {@link BizCatalogManagementFetchProductListFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementFetchProductListFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchProductListQuery")
public final class BizCatalogManagementFetchProductListFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchProductListQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9742717385774446";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchProductListQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementFetchProductListQuery";

    /**
     * The {@code product_list.jid} field naming the business account that owns the catalog, or
     * {@code null} to omit it.
     */
    private final Jid jid;

    /**
     * The {@code product_list.products} identifiers to fetch, each emitted as an
     * {@code {"id": ...}} object, or {@code null} to omit the field.
     *
     * <p>These are opaque catalog product identifiers rather than WhatsApp addresses, so they are kept
     * as plain {@link String}s.
     */
    private final List<String> productIds;

    /**
     * The {@code product_list.width} field carrying the requested thumbnail width, or {@code null} to
     * omit it.
     *
     * <p>Serialized verbatim as a string, matching {@code String(width)} in the source.
     */
    private final String width;

    /**
     * The {@code product_list.height} field carrying the requested thumbnail height, or {@code null}
     * to omit it.
     *
     * <p>Serialized verbatim as a string, matching {@code String(height)} in the source.
     */
    private final String height;

    /**
     * The {@code product_list.direct_connection_encrypted_info} blob authorizing a direct-connection
     * fetch, or {@code null} to omit it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * Constructs a fetch-product-list query request carrying the catalog owner, the product
     * identifiers, and the thumbnail dimensions.
     *
     * <p>All values populate the {@code request.product_list} GraphQL object; each value that is
     * {@code null} is omitted from the serialized object.
     *
     * @param jid                           the catalog-owning business {@link Jid}, or {@code null} to
     *                                      omit the field
     * @param productIds                    the product identifiers to fetch, or {@code null} to omit
     *                                      the field
     * @param width                         the requested thumbnail width, or {@code null} to omit the
     *                                      field
     * @param height                        the requested thumbnail height, or {@code null} to omit
     *                                      the field
     * @param directConnectionEncryptedInfo the direct-connection authorization blob, or {@code null}
     *                                      to omit the field
     */
    public BizCatalogManagementFetchProductListFacebookGraphQlRequest(Jid jid, List<String> productIds, String width, String height, String directConnectionEncryptedInfo) {
        this.jid = jid;
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
     * {@code {"request": {"product_list": {"jid": ..., "products": [{"id": ...}], ...}}}}, writing
     * each field only when its value is non-null, mapping every product identifier to its own
     * {@code {"id": ...}} object, and emitting an empty {@code product_list} object when every field
     * is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchProductList", exports = "fetchProductList",
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
            if (jid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(jid.toString());
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
