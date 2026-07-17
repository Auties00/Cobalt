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
 * Builds the graph.whatsapp.com GraphQL query that reads a single product from a WhatsApp Business catalog.
 *
 * <p>The single {@code request} GraphQL variable nests a {@code product} object that
 * {@code WAWebQueryCatalogProduct} fills with the catalog-owning business {@link Jid}, the target
 * product id, the requested image dimensions, a flag asking the graph.whatsapp.com endpoint to attach compliance info, and
 * the optional direct-connection and variant selectors. The graph.whatsapp.com endpoint returns the product under
 * {@code xwa_product_catalog_get_product}; the reply is consumed through
 * {@link QueryCatalogProductWhatsAppGraphQlResponse}.
 *
 * @see QueryCatalogProductWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebQueryCatalogProductQuery")
public final class QueryCatalogProductWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalogProductQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9647868451963105";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalogProductQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebQueryCatalogProductQuery";

    /**
     * The {@code jid} field of the {@code product} object naming the catalog-owning business account,
     * or {@code null} to omit it.
     */
    private final Jid jid;

    /**
     * The {@code product_id} field identifying the target product, or {@code null} to omit it.
     */
    private final String productId;

    /**
     * The requested image width in pixels used when the graph.whatsapp.com endpoint rewrites image URLs, or {@code null} to
     * omit it.
     */
    private final Integer width;

    /**
     * The requested image height in pixels used when the graph.whatsapp.com endpoint rewrites image URLs, or {@code null}
     * to omit it.
     */
    private final Integer height;

    /**
     * Whether the graph.whatsapp.com endpoint should attach the product's compliance info to the reply.
     *
     * <p>Serialized as the stringified boolean {@code "true"} or {@code "false"}.
     */
    private final boolean fetchComplianceInfo;

    /**
     * The optional direct-connection encrypted payload produced by the business direct-connection
     * retry loop, or {@code null} when not used.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The optional variant-info field selector, or {@code null} when not requested.
     */
    private final String variantInfoFields;

    /**
     * The optional variant thumbnail height in pixels, or {@code null} when not requested.
     */
    private final Integer variantThumbnailHeight;

    /**
     * The optional variant thumbnail width in pixels, or {@code null} when not requested.
     */
    private final Integer variantThumbnailWidth;

    /**
     * Constructs a single-product query request.
     *
     * <p>All values populate the nested {@code product} object. The {@code fetchComplianceInfo} flag
     * asks the graph.whatsapp.com endpoint to attach compliance info and is serialized as the stringified boolean
     * {@code "true"} or {@code "false"}. The {@link Jid} and {@code productId} are omitted when
     * {@code null}; the remaining optional fields are emitted as explicit JSON {@code null} when
     * absent.
     *
     * @param jid                           the catalog-owning business account, or {@code null} to
     *                                      omit the field
     * @param productId                     the target product id, or {@code null} to omit the field
     * @param width                         the requested image width in pixels, or {@code null} to
     *                                      omit the field
     * @param height                        the requested image height in pixels, or {@code null} to
     *                                      omit the field
     * @param fetchComplianceInfo           whether the graph.whatsapp.com endpoint should attach compliance info
     * @param directConnectionEncryptedInfo the optional direct-connection encrypted payload, or
     *                                      {@code null} when not used
     * @param variantInfoFields             the optional variant-info field selector, or {@code null}
     *                                      when not requested
     * @param variantThumbnailHeight        the optional variant thumbnail height in pixels, or
     *                                      {@code null} when not requested
     * @param variantThumbnailWidth         the optional variant thumbnail width in pixels, or
     *                                      {@code null} when not requested
     */
    public QueryCatalogProductWhatsAppGraphQlRequest(Jid jid, String productId, Integer width, Integer height,
                                                        boolean fetchComplianceInfo, String directConnectionEncryptedInfo,
                                                        String variantInfoFields, Integer variantThumbnailHeight,
                                                        Integer variantThumbnailWidth) {
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
     * @implNote This implementation emits {@code {"request": {"product": {...}}}}, mirroring the
     * {@code xwa_product_catalog_get_product} argument shape. The {@code width}, {@code height},
     * {@code variant_thumbnail_height} and {@code variant_thumbnail_width} fields are stringified, and
     * {@code fetch_compliance_info} is written as the stringified boolean {@code "true"}/{@code "false"},
     * matching the WhatsApp Web wire shape that serializes these via {@code String(...)}. The
     * {@link Jid} and {@code product_id} are omitted when {@code null}; the remaining optional fields
     * are written as explicit JSON {@code null} when absent.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalogProduct", exports = "default",
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

            writeStringifiedOrNull(writer, "width", width);
            writeStringifiedOrNull(writer, "height", height);

            writer.writeName("fetch_compliance_info");
            writer.writeColon();
            writer.writeString(Boolean.toString(fetchComplianceInfo));

            writer.writeName("direct_connection_encrypted_info");
            writer.writeColon();
            writeStringOrNull(writer, directConnectionEncryptedInfo);

            writer.writeName("variant_info_fields");
            writer.writeColon();
            writeStringOrNull(writer, variantInfoFields);

            writeStringifiedOrNull(writer, "variant_thumbnail_height", variantThumbnailHeight);
            writeStringifiedOrNull(writer, "variant_thumbnail_width", variantThumbnailWidth);

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
     * Writes a named field whose integer value is stringified, or explicit JSON {@code null} when the
     * value is {@code null}.
     *
     * @param writer the JSON writer to append to
     * @param name   the wire field name
     * @param value  the integer value, or {@code null} to write JSON {@code null}
     */
    private static void writeStringifiedOrNull(JSONWriter writer, String name, Integer value) {
        writer.writeName(name);
        writer.writeColon();
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeString(Integer.toString(value));
        }
    }

    /**
     * Writes the given string value, or explicit JSON {@code null} when the value is {@code null}.
     *
     * @param writer the JSON writer to append to
     * @param value  the string value, or {@code null} to write JSON {@code null}
     */
    private static void writeStringOrNull(JSONWriter writer, String value) {
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeString(value);
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
