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
 * Builds the Facebook GraphQL mutation that updates a WhatsApp Business catalog collection.
 *
 * <p>The single {@code input} GraphQL variable wraps a {@code collection} object that
 * {@code WAWebProductCollectionsJob.editCollection} fills with the collection {@code id}, the owning
 * business {@code biz_jid}, the {@code catalog_session_id}, an optional new {@code name}, and the
 * optional {@code add} and {@code remove} members (each an {@code {"ids": [...]}} object listing the
 * product identifiers to add to or remove from the collection). WhatsApp Web emits {@code name} only
 * when a rename is requested and emits {@code add}/{@code remove} only when their identifier lists are
 * non-empty. The Meta graph endpoint returns the update outcome under
 * {@code xfb_whatsapp_catalog_update_collection}; the reply is consumed through
 * {@link BizCatalogManagementUpdateCollectionFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementUpdateCollectionFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementUpdateCollectionMutation")
public final class BizCatalogManagementUpdateCollectionFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateCollectionMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24486970300891371";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateCollectionMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementUpdateCollectionMutation";

    /**
     * The {@code collection.id} field identifying the collection to update, or {@code null} to omit
     * it.
     *
     * <p>This is an opaque collection identifier rather than a WhatsApp address, so it is kept as a
     * plain {@link String}.
     */
    private final String id;

    /**
     * The {@code collection.biz_jid} field naming the business account that owns the collection, or
     * {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code collection.catalog_session_id} field correlating this edit with a catalog editing
     * session, or {@code null} to omit it.
     */
    private final String catalogSessionId;

    /**
     * The {@code collection.name} field carrying the new collection name, or {@code null} to leave the
     * name unchanged.
     */
    private final String name;

    /**
     * The product identifiers to add, emitted as {@code collection.add = {"ids": [...]}}, or
     * {@code null} to omit the {@code add} member.
     *
     * <p>These are opaque catalog product identifiers rather than WhatsApp addresses, so they are kept
     * as plain {@link String}s.
     */
    private final List<String> addIds;

    /**
     * The product identifiers to remove, emitted as {@code collection.remove = {"ids": [...]}}, or
     * {@code null} to omit the {@code remove} member.
     *
     * <p>These are opaque catalog product identifiers rather than WhatsApp addresses, so they are kept
     * as plain {@link String}s.
     */
    private final List<String> removeIds;

    /**
     * Constructs an update-collection mutation request carrying the collection identity, the owning
     * business, the catalog session, an optional rename, and the optional add and remove identifier
     * lists.
     *
     * <p>All values populate the {@code input.collection} GraphQL object; each value that is
     * {@code null} is omitted from the serialized object, and the {@code add} and {@code remove}
     * members are written as nested {@code {"ids": [...]}} objects when present.
     *
     * @param id               the collection identifier to update, or {@code null} to omit the field
     * @param bizJid           the owning business {@link Jid}, or {@code null} to omit the field
     * @param catalogSessionId the catalog editing session id, or {@code null} to omit the field
     * @param name             the new collection name, or {@code null} to leave it unchanged
     * @param addIds           the product identifiers to add, or {@code null} to omit the {@code add}
     *                         member
     * @param removeIds        the product identifiers to remove, or {@code null} to omit the
     *                         {@code remove} member
     */
    public BizCatalogManagementUpdateCollectionFacebookGraphQlRequest(String id, Jid bizJid, String catalogSessionId, String name, List<String> addIds, List<String> removeIds) {
        this.id = id;
        this.bizJid = bizJid;
        this.catalogSessionId = catalogSessionId;
        this.name = name;
        this.addIds = addIds;
        this.removeIds = removeIds;
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
     * {@code {"input": {"collection": {"id": ..., "biz_jid": ..., "catalog_session_id": ..., ...}}}},
     * writing each field only when its value is non-null and rendering {@code add} and {@code remove}
     * as {@code {"ids": [...]}} objects; an empty {@code collection} object is emitted when every
     * field is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebProductCollectionsJob", exports = "editCollection",
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
            if (id != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(id);
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

            if (name != null) {
                writer.writeName("name");
                writer.writeColon();
                writer.writeString(name);
            }

            if (addIds != null) {
                writer.writeName("add");
                writer.writeColon();
                writeIds(writer, addIds);
            }

            if (removeIds != null) {
                writer.writeName("remove");
                writer.writeColon();
                writeIds(writer, removeIds);
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
     * Writes an {@code {"ids": [...]}} membership object for the {@code add} or {@code remove} member.
     *
     * @param writer the JSON writer to append to
     * @param ids    the product identifiers to enclose under {@code ids}
     */
    private static void writeIds(JSONWriter writer, List<String> ids) {
        writer.startObject();
        writer.writeName("ids");
        writer.writeColon();
        writer.startArray();
        for (var productId : ids) {
            writer.writeString(productId);
        }
        writer.endArray();
        writer.endObject();
    }
}
