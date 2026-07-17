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
 * Builds the Facebook GraphQL mutation that creates a WhatsApp Business catalog collection.
 *
 * <p>The single {@code input} GraphQL variable carries a nested {@code collection} object naming the
 * collection to create. WhatsApp Web's {@code WAWebProductCollectionsJob.createCollectionGraphQL}
 * fills it with {@code {collection: {name, product_ids, biz_jid, catalog_session_id}}}: the display
 * {@code name}, the list of {@code product_ids} to seed the collection with, the owning
 * {@code biz_jid}, and the {@code catalog_session_id} grouping this edit with the rest of the catalog
 * session. The Meta graph endpoint returns the created collection under
 * {@code xfb_whatsapp_catalog_create_collection}; the reply is consumed through
 * {@link BizCatalogManagementCreateCollectionFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementCreateCollectionFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementCreateCollectionMutation")
public final class BizCatalogManagementCreateCollectionFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementCreateCollectionMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "29361942130088470";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementCreateCollectionMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementCreateCollectionMutation";

    /**
     * The {@code collection.name} field of the {@code input} object carrying the new collection's
     * display name, or {@code null} to omit it.
     */
    private final String name;

    /**
     * The {@code collection.product_ids} field of the {@code input} object listing the product ids to
     * seed the new collection with, or {@code null} to omit it.
     *
     * <p>These are opaque catalog product identifiers rather than WhatsApp addresses, so they are kept
     * as plain {@link String} values.
     */
    private final List<String> productIds;

    /**
     * The {@code collection.biz_jid} field of the {@code input} object naming the business account
     * that owns the new collection, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code collection.catalog_session_id} field of the {@code input} object grouping this edit
     * with the rest of the catalog session, or {@code null} to omit it.
     */
    private final String catalogSessionId;

    /**
     * Constructs a create-collection mutation request carrying the collection name, the seed product
     * ids, the owning business account, and the catalog session id.
     *
     * <p>All four values populate the nested {@code collection} object of the {@code input} variable;
     * each value that is {@code null} is omitted from the serialized object, and a {@code null}
     * {@code productIds} omits the {@code product_ids} array while an empty list serializes as an
     * empty array.
     *
     * @param name             the new collection's display name, or {@code null} to omit the field
     * @param productIds       the product ids to seed the collection with, or {@code null} to omit the
     *                         field
     * @param bizJid           the business account {@link Jid} that owns the collection, or
     *                         {@code null} to omit the field
     * @param catalogSessionId the catalog session id grouping this edit, or {@code null} to omit the
     *                         field
     */
    public BizCatalogManagementCreateCollectionFacebookGraphQlRequest(String name, List<String> productIds, Jid bizJid, String catalogSessionId) {
        this.name = name;
        this.productIds = productIds;
        this.bizJid = bizJid;
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
     * @implNote This implementation emits {@code {"input": {"collection": {"name": <name>,
     * "product_ids": [...], "biz_jid": <bizJid>, "catalog_session_id": <catalogSessionId>}}}}, writing
     * each field only when its value is non-null and emitting {@code {"input": {"collection": {}}}}
     * when all are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebProductCollectionsJob", exports = "createCollectionGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            writer.writeName("collection");
            writer.writeColon();
            writer.startObject();
            if (name != null) {
                writer.writeName("name");
                writer.writeColon();
                writer.writeString(name);
            }

            if (productIds != null) {
                writer.writeName("product_ids");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < productIds.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(productIds.get(i));
                }
                writer.endArray();
            }

            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            if (catalogSessionId != null) {
                writer.writeName("catalog_session_id");
                writer.writeColon();
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
}
