package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

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
 * Fetches the current reachout-timelock enforcement state for the account.
 *
 * <p>The reachout timelock is a cooldown window during which outgoing contact attempts are
 * restricted following spam reports or suspicious-account-behaviour signals. The resulting snapshot
 * gates the reach-out UI; embedders apply the gating using the three scalars exposed by
 * {@link FetchReachoutTimelockMexResponse}. The query takes no GraphQL variables.
 *
 * @implNote This implementation emits an empty {@code variables} object because the compiled
 * GraphQL artifact takes no inputs.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchReachoutTimelockJob")
public final class FetchReachoutTimelockMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled GraphQL query identifier for the reachout-timelock query document.
     *
     * <p>The relay maps this identifier to a server-side persisted operation and never sees the
     * GraphQL text on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchReachoutTimelockJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "23983697327930364";

    /**
     * Holds the GraphQL operation name reported to the MEX perf tracker when this query is
     * dispatched.
     *
     * <p>The name tags the query in latency and error metrics; it is kept on the request so
     * embedders mirroring that telemetry surface can emit the same tag.
     */
    public static final String OPERATION_NAME = "mexFetchReachoutTimelock";

    /**
     * Constructs a new request carrying no GraphQL variables.
     *
     * <p>The compiled GraphQL artifact takes no inputs so a single instance is sufficient for every
     * dispatch.
     */
    public FetchReachoutTimelockMexRequest() {
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
     * @implNote This implementation writes an empty {@code variables} object using fastjson2's
     * {@link JSONWriter} and wraps it via
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchReachoutTimelockJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
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
