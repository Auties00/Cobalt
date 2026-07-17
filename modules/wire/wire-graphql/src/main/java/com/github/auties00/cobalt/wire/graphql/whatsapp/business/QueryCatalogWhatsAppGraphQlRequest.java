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
 * Builds the graph.whatsapp.com GraphQL query that reads one page of a WhatsApp Business catalog's products.
 *
 * <p>A catalog is the flat storefront attached to a business account; this query fetches the page of
 * products it contains together with their core metadata, status, media and variant info. The single
 * {@code request} GraphQL variable nests a {@code product_catalog} object that
 * {@code WAWebQueryCatalog} fills from the guest browse path: the catalog-owner {@link Jid}, the
 * shop-source opt-in, the requested image dimensions, the page size and cursor, and the optional
 * direct-connection, session and variant selectors. The graph.whatsapp.com endpoint returns the products under
 * {@code xwa_product_catalog_get_product_catalog}; the reply is consumed through
 * {@link QueryCatalogWhatsAppGraphQlResponse}.
 *
 * @see QueryCatalogWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebQueryCatalogQuery")
public final class QueryCatalogWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalogQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9916553288394782";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalogQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebQueryCatalogQuery";

    /**
     * The {@code jid} field of the {@code product_catalog} object naming the catalog-owning business
     * account, or {@code null} to omit it.
     */
    private final Jid jid;

    /**
     * Whether the query opts into the WhatsApp shop source surface.
     *
     * <p>Serialized as the WhatsApp Web enum literal {@code ALLOWSHOPSOURCE_TRUE} or
     * {@code ALLOWSHOPSOURCE_FALSE}.
     */
    private final boolean allowShopSource;

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
     * The optional direct-connection encrypted payload produced by the business direct-connection
     * retry loop, or {@code null} when not used.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * The page size bounding the number of products returned per page, or {@code null} to omit it.
     */
    private final Integer limit;

    /**
     * The pagination cursor returned by a previous page, or {@code null} for the first page.
     */
    private final String after;

    /**
     * The optional catalog session id correlating a browse session, or {@code null} when not used.
     */
    private final String catalogSessionId;

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
     * Constructs a catalog-products query request.
     *
     * <p>All values populate the nested {@code product_catalog} object. The {@code allowShopSource}
     * flag selects the WhatsApp shop source surface and is serialized as the enum literal
     * {@code ALLOWSHOPSOURCE_TRUE} or {@code ALLOWSHOPSOURCE_FALSE}. Every other value that is
     * {@code null} is emitted as explicit JSON {@code null}, matching the graph.whatsapp.com endpoint document shape, except
     * for the {@link Jid} which is omitted when {@code null}.
     *
     * @param jid                           the catalog-owning business account, or {@code null} to
     *                                      omit the field
     * @param allowShopSource               whether to opt into the WhatsApp shop source surface
     * @param width                         the requested image width in pixels, or {@code null} to
     *                                      omit the field
     * @param height                        the requested image height in pixels, or {@code null} to
     *                                      omit the field
     * @param directConnectionEncryptedInfo the optional direct-connection encrypted payload, or
     *                                      {@code null} when not used
     * @param limit                         the page size, or {@code null} to omit the field
     * @param after                         the pagination cursor, or {@code null} for the first page
     * @param catalogSessionId              the optional catalog session id, or {@code null} when not
     *                                      used
     * @param variantInfoFields             the optional variant-info field selector, or {@code null}
     *                                      when not requested
     * @param variantThumbnailHeight        the optional variant thumbnail height in pixels, or
     *                                      {@code null} when not requested
     * @param variantThumbnailWidth         the optional variant thumbnail width in pixels, or
     *                                      {@code null} when not requested
     */
    public QueryCatalogWhatsAppGraphQlRequest(Jid jid, boolean allowShopSource, Integer width, Integer height,
                                                 String directConnectionEncryptedInfo, Integer limit, String after,
                                                 String catalogSessionId, String variantInfoFields,
                                                 Integer variantThumbnailHeight, Integer variantThumbnailWidth) {
        this.jid = jid;
        this.allowShopSource = allowShopSource;
        this.width = width;
        this.height = height;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
        this.limit = limit;
        this.after = after;
        this.catalogSessionId = catalogSessionId;
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
     * {@code {"request": {"product_catalog": {...}}}}, mirroring the
     * {@code xwa_product_catalog_get_product_catalog} argument shape. The {@code width},
     * {@code height}, {@code limit}, {@code variant_thumbnail_height} and {@code variant_thumbnail_width}
     * fields are stringified to match the WhatsApp Web wire shape, which sends them as JSON strings
     * even though they are integers in the schema; {@code allow_shop_source} is written as the
     * {@code ALLOWSHOPSOURCE_TRUE}/{@code ALLOWSHOPSOURCE_FALSE} enum literal. The optional fields are
     * emitted as explicit JSON {@code null} when absent rather than omitted, except for the
     * {@link Jid}, which is omitted entirely when {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryCatalog", exports = "default",
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

            writer.writeName("allow_shop_source");
            writer.writeColon();
            writer.writeString(allowShopSource ? "ALLOWSHOPSOURCE_TRUE" : "ALLOWSHOPSOURCE_FALSE");

            writeStringifiedOrNull(writer, "width", width);
            writeStringifiedOrNull(writer, "height", height);

            writer.writeName("direct_connection_encrypted_info");
            writer.writeColon();
            writeStringOrNull(writer, directConnectionEncryptedInfo);

            writeStringifiedOrNull(writer, "limit", limit);

            writer.writeName("after");
            writer.writeColon();
            writeStringOrNull(writer, after);

            writer.writeName("catalog_session_id");
            writer.writeColon();
            writeStringOrNull(writer, catalogSessionId);

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
