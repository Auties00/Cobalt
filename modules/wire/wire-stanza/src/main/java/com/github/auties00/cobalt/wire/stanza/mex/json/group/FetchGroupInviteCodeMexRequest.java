package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.wire.stanza.mex.json.MexGroupQueryContext;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Outbound MEX query that reads the invite code currently bound to a group without rotating it.
 *
 * <p>The reply carries the shareable {@code chat.whatsapp.com/<code>} link's opaque suffix, used to
 * display the link, prime the group-info panel, or copy the link to the clipboard. Callers that need
 * to rotate the code instead of reading it use {@link CreateInviteCodeMexRequest}.
 *
 * @implNote This implementation always emits the {@code query_context} variable as a typed
 * {@link MexGroupQueryContext}; WA Web pins it to {@link MexGroupQueryContext#INVITE_CODE} at its
 * only call site.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchGroupInviteCodeJob")
public final class FetchGroupInviteCodeMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the {@code WAWebMexFetchGroupInviteCodeJobQuery}
     * document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text is never sent on the
     * wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInviteCodeJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "29247029834912157";

    /**
     * GraphQL operation name reported alongside this query when it is dispatched.
     *
     * <p>Tags the query in latency and error metrics; kept on the request for embedders mirroring
     * WhatsApp's telemetry surface.
     */
    public static final String OPERATION_NAME = "fetchMexGroupInviteCode";

    /**
     * Target group identifier bound to the {@code id} GraphQL variable.
     */
    private final String id;

    /**
     * Query-context tag bound to the {@code query_context} GraphQL variable; always emitted.
     */
    private final MexGroupQueryContext queryContext;

    /**
     * Constructs a new request with the two GraphQL variables.
     *
     * <p>The {@code queryContext} mirrors the WA Web tag pinned at the call site, which is always
     * {@link MexGroupQueryContext#INVITE_CODE}; it is required and always emitted.
     *
     * @param id           the target group identifier
     * @param queryContext the query-context tag; never {@code null}
     * @throws NullPointerException if {@code queryContext} is {@code null}
     */
    public FetchGroupInviteCodeMexRequest(String id, MexGroupQueryContext queryContext) {
        this.id = id;
        this.queryContext = Objects.requireNonNull(queryContext, "queryContext cannot be null");
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
     * @implNote This implementation streams the GraphQL variables through fastjson2's
     * {@link JSONWriter}, always emitting {@code query_context} and emitting {@code id} only when its
     * constructor argument is non-null, matching WA Web which always sends the query context and omits
     * other undefined GraphQL variables. The wrapped envelope is built through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInviteCodeJob", exports = "fetchMexGroupInviteCode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            if (id != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(id);
            }
            writer.writeName("query_context");
            writer.writeColon();
            writer.writeString(queryContext.wireValue());
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
