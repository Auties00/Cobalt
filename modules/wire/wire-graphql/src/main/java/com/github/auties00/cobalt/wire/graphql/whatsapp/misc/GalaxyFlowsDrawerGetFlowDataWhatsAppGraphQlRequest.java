package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

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
 * Builds the graph.whatsapp.com GraphQL query that fetches WhatsApp Flows (Galaxy) extension metadata for a business flow.
 *
 * <p>The single {@code request} GraphQL variable wraps an {@code extensions} object. WhatsApp Web's
 * {@code WAWebGalaxyFlowsDrawerGetFlowData} fills it with the business account {@code biz_jid} (the
 * phone-number form of the contact, resolved from a LID through
 * {@code WAWebLidMigrationUtils.toPn}) and the {@code flow_id} naming the flow whose metadata is being
 * fetched. The graph.whatsapp.com endpoint returns the flow data and the endpoint public key under
 * {@code xwa_extensions_get_flow_data}; the reply is consumed through
 * {@link GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlResponse}.
 *
 * @see GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebGalaxyFlowsDrawerGetFlowDataQuery")
public final class GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body. WhatsApp Web resolves the
     * live id from {@code WAWebGraphQLPersistedQueries}, which overrides the id compiled into the
     * {@code .graphql} document.
     */
    @WhatsAppWebExport(moduleName = "WAWebGalaxyFlowsDrawerGetFlowDataQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24989855014035777";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebGalaxyFlowsDrawerGetFlowDataQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebGalaxyFlowsDrawerGetFlowDataQuery";

    /**
     * The {@code biz_jid} field of the {@code extensions} object naming the business account, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web sets this to the phone-number form of the contact's address.
     */
    private final Jid bizJid;

    /**
     * The {@code flow_id} field of the {@code extensions} object naming the flow whose metadata is
     * fetched, or {@code null} to omit it.
     */
    private final String flowId;

    /**
     * Constructs a get-flow-data request carrying the business account and the flow id.
     *
     * <p>Both values populate the nested {@code request.extensions} object; each value that is
     * {@code null} is omitted from the serialized object.
     *
     * @param bizJid the business account {@link Jid}, or {@code null} to omit the field
     * @param flowId the flow whose metadata is fetched, or {@code null} to omit the field
     */
    public GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlRequest(Jid bizJid, String flowId) {
        this.bizJid = bizJid;
        this.flowId = flowId;
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
     * {@code {"request": {"extensions": {"biz_jid": <bizJid>, "flow_id": <flowId>}}}}, writing each
     * field only when its value is non-null and emitting an empty {@code extensions} object when both
     * are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebGalaxyFlowsDrawerGetFlowDataQuery", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            writer.writeName("extensions");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            if (flowId != null) {
                writer.writeName("flow_id");
                writer.writeColon();
                writer.writeString(flowId);
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
