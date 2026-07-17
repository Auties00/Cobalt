package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Outbound MEX query that probes whether a group is flagged as internal by the WhatsApp relay.
 *
 * <p>The resulting boolean drives the staff-only indicator badge rendered on Meta-internal testing
 * groups, and is only relevant when the internal-group-indicator gating is enabled; most embedders
 * do not need this query.
 *
 * @implNote This implementation issues a single GraphQL variable ({@code id}) and reads the
 * {@code properties.internal} scalar from the response. The relay's four group inline-fragment
 * variants (regular group, community, default subgroup, subgroup) all surface the flag under the
 * same {@code properties.internal} path, so the parser collapses them.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchGroupIsInternalJob")
public final class FetchGroupIsInternalMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the {@code WAWebMexFetchGroupIsInternalJobQuery}
     * document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text is never sent on the
     * wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupIsInternalJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "34119218944390847";

    /**
     * GraphQL operation name reported alongside this query when it is dispatched.
     *
     * <p>Tags the query in latency and error metrics; kept on the request for embedders mirroring
     * WhatsApp's telemetry surface.
     */
    public static final String OPERATION_NAME = "mexFetchGroupIsInternal";

    /**
     * Target group identifier bound to the {@code id} GraphQL variable.
     */
    private final String groupId;

    /**
     * Constructs a new request with the single {@code id} GraphQL variable.
     *
     * <p>The argument is forwarded verbatim to the relay as the declared top-level {@code id}
     * variable, which WA Web always sends; callers are expected to supply a non-null identifier.
     *
     * @param groupId the target group identifier
     */
    public FetchGroupIsInternalMexRequest(String groupId) {
        this.groupId = groupId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return QUERY_ID;
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
     * @implNote This implementation streams the single {@code id} GraphQL variable through
     * fastjson2's {@link JSONWriter} and wraps the resulting variables object in the standard MEX IQ
     * envelope built through {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupIsInternalJob", exports = "mexFetchGroupIsInternal",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("id");
            writer.writeColon();
            writer.writeString(groupId);
            writer.endObject();
            writer.endObject();

            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return Json.createMexNode(QUERY_ID, output.toString());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
