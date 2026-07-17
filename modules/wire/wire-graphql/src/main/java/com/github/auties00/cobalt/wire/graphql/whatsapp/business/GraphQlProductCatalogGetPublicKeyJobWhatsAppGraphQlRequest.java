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
 * Builds the graph.whatsapp.com GraphQL query that fetches a business catalog's encryption public key.
 *
 * <p>The single {@code request} GraphQL variable wraps a {@code public_key} object naming the
 * business whose catalog public key is being fetched. WhatsApp Web's
 * {@code WAWebGraphQLProductCatalogGetPublicKeyJob} fills it as
 * {@code {request: {public_key: {biz_jid: <jid>}}}} from the business {@link Jid}. The graph.whatsapp.com endpoint returns
 * the PEM-encoded public key and its signature under {@code xwa_product_catalog_get_public_key}; the
 * reply is consumed through {@link GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlResponse}.
 *
 * @see GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebGraphQLProductCatalogGetPublicKeyJobQuery")
public final class GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     *
     * @implNote This implementation ships the numeric persisted-query id resolved from
     * {@code WAWebGraphQLPersistedQueries.PersistedQueries["WAWebGraphQLProductCatalogGetPublicKeyJobQuery"]}
     * rather than the document's own {@code params.id}, which is the non-numeric document name; the
     * graph.whatsapp.com endpoint rewrites the document-name {@code params.id} to this registered numeric id at dispatch.
     */
    @WhatsAppWebExport(moduleName = "WAWebGraphQLPersistedQueries", exports = "PersistedQueries",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24765734146405491";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebGraphQLProductCatalogGetPublicKeyJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebGraphQLProductCatalogGetPublicKeyJobQuery";

    /**
     * The {@code request.public_key.biz_jid} field naming the business whose catalog public key is
     * being fetched, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * Constructs a get-public-key query request.
     *
     * <p>The {@code bizJid} populates the nested {@code request.public_key.biz_jid} field; a
     * {@code null} value omits it from the serialized object.
     *
     * @param bizJid the business {@link Jid}, or {@code null} to omit the field
     */
    public GraphQlProductCatalogGetPublicKeyJobWhatsAppGraphQlRequest(Jid bizJid) {
        this.bizJid = bizJid;
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
     * @implNote This implementation emits {@code {"request": {"public_key": {"biz_jid": <bizJid>}}}},
     * writing the {@code biz_jid} scalar only when {@code bizJid} is non-null and otherwise emitting
     * the empty nested {@code public_key} object.
     */
    @WhatsAppWebExport(moduleName = "WAWebGraphQLProductCatalogGetPublicKeyJob", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("public_key");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
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
