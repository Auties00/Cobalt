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
 * Builds the Facebook GraphQL mutation that deletes WhatsApp Business catalog collections.
 *
 * <p>The single {@code input} GraphQL variable carries a nested {@code collections} object naming the
 * collections to delete. WhatsApp Web's {@code WAWebProductCollectionsJob.deleteCollectionGraphQL}
 * fills it with {@code {collections: {collection_ids, biz_jid, catalog_session_id}}}: the list of
 * {@code collection_ids} to delete, the owning {@code biz_jid}, and the {@code catalog_session_id}
 * grouping this edit with the rest of the catalog session. The Meta graph endpoint returns the deletion outcome
 * under {@code xfb_whatsapp_catalog_delete_collections}; the reply is consumed through
 * {@link BizCatalogManagementDeleteCollectionsFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementDeleteCollectionsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementDeleteCollectionsMutation")
public final class BizCatalogManagementDeleteCollectionsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementDeleteCollectionsMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "29970196299234260";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementDeleteCollectionsMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementDeleteCollectionsMutation";

    /**
     * The {@code collections.collection_ids} field of the {@code input} object listing the collection
     * ids to delete, or {@code null} to omit it.
     *
     * <p>These are opaque catalog collection identifiers rather than WhatsApp addresses, so they are
     * kept as plain {@link String} values.
     */
    private final List<String> collectionIds;

    /**
     * The {@code collections.biz_jid} field of the {@code input} object naming the business account
     * that owns the collections, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code collections.catalog_session_id} field of the {@code input} object grouping this edit
     * with the rest of the catalog session, or {@code null} to omit it.
     */
    private final String catalogSessionId;

    /**
     * Constructs a delete-collections mutation request carrying the collection ids to delete, the
     * owning business account, and the catalog session id.
     *
     * <p>All three values populate the nested {@code collections} object of the {@code input}
     * variable; each value that is {@code null} is omitted from the serialized object, and a
     * {@code null} {@code collectionIds} omits the {@code collection_ids} array while an empty list
     * serializes as an empty array.
     *
     * @param collectionIds    the collection ids to delete, or {@code null} to omit the field
     * @param bizJid           the business account {@link Jid} that owns the collections, or
     *                         {@code null} to omit the field
     * @param catalogSessionId the catalog session id grouping this edit, or {@code null} to omit the
     *                         field
     */
    public BizCatalogManagementDeleteCollectionsFacebookGraphQlRequest(List<String> collectionIds, Jid bizJid, String catalogSessionId) {
        this.collectionIds = collectionIds;
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
     * @implNote This implementation emits {@code {"input": {"collections": {"collection_ids": [...],
     * "biz_jid": <bizJid>, "catalog_session_id": <catalogSessionId>}}}}, writing each field only when
     * its value is non-null and emitting {@code {"input": {"collections": {}}}} when all are
     * {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebProductCollectionsJob", exports = "deleteCollectionGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            writer.writeName("collections");
            writer.writeColon();
            writer.startObject();
            if (collectionIds != null) {
                writer.writeName("collection_ids");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < collectionIds.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(collectionIds.get(i));
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
