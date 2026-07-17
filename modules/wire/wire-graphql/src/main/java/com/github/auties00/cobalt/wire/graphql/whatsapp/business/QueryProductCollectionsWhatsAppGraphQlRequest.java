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
 * Builds the graph.whatsapp.com GraphQL query that reads one page of a WhatsApp Business catalog's product collections.
 *
 * <p>Collections are named groups of products a business defines inside a catalog; this query fetches
 * the page of collections paired with the products nested inside each. The single {@code request}
 * GraphQL variable nests a {@code collections} object that {@code WAWebQueryProductCollections} fills
 * with the catalog-owning business {@link Jid}, the per-page collection limit and per-collection item
 * limit, the page cursor, the requested image dimensions, and the optional direct-connection and
 * variant selectors. The graph.whatsapp.com endpoint returns the collections under
 * {@code xwa_product_catalog_get_collections}; the reply is consumed through
 * {@link QueryProductCollectionsWhatsAppGraphQlResponse}.
 *
 * @see QueryProductCollectionsWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebQueryProductCollectionsQuery")
public final class QueryProductCollectionsWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductCollectionsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9430970660362540";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductCollectionsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebQueryProductCollectionsQuery";

    /**
     * The {@code biz_jid} field of the {@code collections} object naming the catalog-owning business
     * account, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The page size bounding the number of collections returned per page, or {@code null} to omit it.
     */
    private final Integer collectionLimit;

    /**
     * The per-collection limit bounding the number of products returned inside every collection, or
     * {@code null} to omit it.
     */
    private final Integer itemLimit;

    /**
     * The pagination cursor returned by a previous page, or {@code null} for the first page.
     */
    private final String after;

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
     * Constructs a product-collections query request.
     *
     * <p>All values populate the nested {@code collections} object. The {@code collectionLimit} and
     * {@code itemLimit} are independent: the first bounds the number of collections returned per page,
     * the second bounds the number of products returned inside each collection. The {@link Jid} is
     * omitted when {@code null}; every other optional field is emitted as explicit JSON {@code null}
     * when absent.
     *
     * @param bizJid                        the catalog-owning business account, or {@code null} to
     *                                      omit the field
     * @param collectionLimit               the per-page collection limit, or {@code null} to omit the
     *                                      field
     * @param itemLimit                     the per-collection item limit, or {@code null} to omit the
     *                                      field
     * @param after                         the pagination cursor, or {@code null} for the first page
     * @param width                         the requested image width in pixels, or {@code null} to
     *                                      omit the field
     * @param height                        the requested image height in pixels, or {@code null} to
     *                                      omit the field
     * @param directConnectionEncryptedInfo the optional direct-connection encrypted payload, or
     *                                      {@code null} when not used
     * @param variantInfoFields             the optional variant-info field selector, or {@code null}
     *                                      when not requested
     * @param variantThumbnailHeight        the optional variant thumbnail height in pixels, or
     *                                      {@code null} when not requested
     * @param variantThumbnailWidth         the optional variant thumbnail width in pixels, or
     *                                      {@code null} when not requested
     */
    public QueryProductCollectionsWhatsAppGraphQlRequest(Jid bizJid, Integer collectionLimit, Integer itemLimit, String after,
                                                            Integer width, Integer height, String directConnectionEncryptedInfo,
                                                            String variantInfoFields, Integer variantThumbnailHeight,
                                                            Integer variantThumbnailWidth) {
        this.bizJid = bizJid;
        this.collectionLimit = collectionLimit;
        this.itemLimit = itemLimit;
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
     * @implNote This implementation emits {@code {"request": {"collections": {...}}}}, mirroring the
     * {@code xwa_product_catalog_get_collections} argument shape. The {@code collection_limit},
     * {@code item_limit}, {@code width}, {@code height}, {@code variant_thumbnail_height} and
     * {@code variant_thumbnail_width} fields are stringified to match the WhatsApp Web wire shape,
     * which sends them as JSON strings even though they are integers in the schema. The {@link Jid} is
     * omitted when {@code null}; the remaining optional fields are written as explicit JSON
     * {@code null} when absent.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryProductCollections", exports = "default",
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

            writeStringifiedOrNull(writer, "collection_limit", collectionLimit);
            writeStringifiedOrNull(writer, "item_limit", itemLimit);

            writer.writeName("after");
            writer.writeColon();
            writeStringOrNull(writer, after);

            writeStringifiedOrNull(writer, "width", width);
            writeStringifiedOrNull(writer, "height", height);

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
