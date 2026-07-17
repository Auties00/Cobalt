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
 * Builds the Facebook GraphQL query that fetches a single WhatsApp Business catalog product.
 *
 * <p>The single {@code request} GraphQL variable wraps a {@code product} object that
 * {@code WAWebBizCatalogManagementFetchProduct.fetchProduct} fills with the catalog-owning business
 * {@code jid}, the {@code product_id} to fetch, the requested thumbnail {@code width} and
 * {@code height}, an optional {@code direct_connection_encrypted_info} blob, a
 * {@code fetch_compliance_info} flag, and the variant projection knobs ({@code variant_info_fields},
 * {@code variant_thumbnail_height}, {@code variant_thumbnail_width}). The numeric dimensions and the
 * compliance flag travel on the wire as strings. The Meta graph endpoint returns the product under
 * {@code xfb_whatsapp_catalog_product}; the reply is consumed through
 * {@link BizCatalogManagementFetchProductFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementFetchProductFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementFetchProductQuery")
public final class BizCatalogManagementFetchProductFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchProductQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24529100180014015";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchProductQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementFetchProductQuery";

    /**
     * The {@code product.jid} field naming the business account that owns the catalog, or
     * {@code null} to omit it.
     */
    private final Jid jid;

    /**
     * The {@code product.product_id} field identifying the product to fetch, or {@code null} to omit
     * it.
     *
     * <p>This is an opaque catalog product identifier rather than a WhatsApp address, so it is kept as
     * a plain {@link String}.
     */
    private final String productId;

    /**
     * The {@code product.width} field carrying the requested thumbnail width, or {@code null} to omit
     * it.
     *
     * <p>Serialized verbatim as a string, matching {@code String(width)} in the source.
     */
    private final String width;

    /**
     * The {@code product.height} field carrying the requested thumbnail height, or {@code null} to
     * omit it.
     *
     * <p>Serialized verbatim as a string, matching {@code String(height)} in the source.
     */
    private final String height;

    /**
     * The {@code product.fetch_compliance_info} flag requesting the compliance projection, or
     * {@code null} to omit it.
     *
     * <p>Serialized verbatim as a string, matching {@code String(fetchComplianceInfo)} in the source
     * (the boolean is rendered as {@code "true"} or {@code "false"}).
     */
    private final String fetchComplianceInfo;

    /**
     * The {@code product.direct_connection_encrypted_info} blob authorizing a direct-connection
     * fetch, or {@code null} to omit it.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The {@code product.variant_info_fields} selector listing the variant sub-projections to fetch,
     * or {@code null} to omit it.
     *
     * <p>A single comma-separated token (for example
     * {@code "listing_details,types,availability,variant_properties"}), not a JSON array.
     */
    private final String variantInfoFields;

    /**
     * The {@code product.variant_thumbnail_height} field carrying the requested variant-thumbnail
     * height, or {@code null} to omit it.
     *
     * <p>Serialized verbatim as a string.
     */
    private final String variantThumbnailHeight;

    /**
     * The {@code product.variant_thumbnail_width} field carrying the requested variant-thumbnail
     * width, or {@code null} to omit it.
     *
     * <p>Serialized verbatim as a string.
     */
    private final String variantThumbnailWidth;

    /**
     * Constructs a fetch-product query request carrying the catalog owner, the target product, and
     * the thumbnail and variant projection knobs.
     *
     * <p>All values populate the {@code request.product} GraphQL object; each value that is
     * {@code null} is omitted from the serialized object.
     *
     * @param jid                           the catalog-owning business {@link Jid}, or {@code null} to
     *                                      omit the field
     * @param productId                     the product identifier to fetch, or {@code null} to omit
     *                                      the field
     * @param width                         the requested thumbnail width, or {@code null} to omit the
     *                                      field
     * @param height                        the requested thumbnail height, or {@code null} to omit
     *                                      the field
     * @param fetchComplianceInfo           the compliance-projection flag, or {@code null} to omit the
     *                                      field
     * @param directConnectionEncryptedInfo the direct-connection authorization blob, or {@code null}
     *                                      to omit the field
     * @param variantInfoFields             the comma-separated variant projection selector, or
     *                                      {@code null} to omit the field
     * @param variantThumbnailHeight        the requested variant-thumbnail height, or {@code null} to
     *                                      omit the field
     * @param variantThumbnailWidth         the requested variant-thumbnail width, or {@code null} to
     *                                      omit the field
     */
    public BizCatalogManagementFetchProductFacebookGraphQlRequest(Jid jid, String productId, String width, String height, String fetchComplianceInfo, String directConnectionEncryptedInfo, String variantInfoFields, String variantThumbnailHeight, String variantThumbnailWidth) {
        this.jid = jid;
        this.productId = productId;
        this.width = width;
        this.height = height;
        this.fetchComplianceInfo = fetchComplianceInfo;
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
     * @implNote This implementation emits {@code {"request": {"product": {...}}}}, writing each
     * {@code product} field only when its value is non-null, rendering {@code jid} as its canonical
     * string, and emitting an empty {@code product} object when every field is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementFetchProduct", exports = "fetchProduct",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("product");
            writer.writeColon();
            writer.startObject();
            if (jid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(jid.toString());
            }

            if (productId != null) {
                writer.writeName("product_id");
                writer.writeColon();
                writer.writeString(productId);
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

            if (fetchComplianceInfo != null) {
                writer.writeName("fetch_compliance_info");
                writer.writeColon();
                writer.writeString(fetchComplianceInfo);
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
