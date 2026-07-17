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
 * Fetches the new-chat messaging quota counters and per-policy status flags enforced on the
 * account.
 *
 * <p>This query is issued when the individual new-chat messaging capping feature is enabled. The
 * parsed snapshot drives the messaging-capping UI and exposes the {@code capping_status},
 * {@code ote_status} and {@code mv_status} flags. The {@link #input} value names the capping
 * thread type and is always wrapped on the wire as the GraphQL object {@code {"type": <input>}}.
 *
 * @implNote This implementation wraps {@link #input} in a {@code {"type": ...}} object, mirroring
 * WhatsApp Web which hard-codes {@code {"type":"INDIVIDUAL_NEW_CHAT_THREAD"}}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewChatMessageCappingInfoJob")
public final class FetchNewChatMessageCappingInfoMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled GraphQL query identifier for the message-capping query document.
     *
     * <p>The relay maps this identifier to a server-side persisted operation and never sees the
     * GraphQL text on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewChatMessageCappingInfoJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "24503548349331633";

    /**
     * Holds the GraphQL operation name reported to the MEX perf tracker when this query is
     * dispatched.
     *
     * <p>The name tags the query in latency and error metrics; it is kept on the request so
     * embedders mirroring that telemetry surface can emit the same tag.
     */
    public static final String OPERATION_NAME = "mexFetchNewChatMessageCapping";

    /**
     * Holds the capping thread type bound to the {@code input.type} GraphQL variable.
     *
     * <p>The value names the capping policy to query; WhatsApp Web uses
     * {@snippet :
     * String input = "INDIVIDUAL_NEW_CHAT_THREAD";
     * }
     * and {@link #toStanza()} wraps it as the GraphQL object {@code {"type": <input>}}.
     */
    private final String input;

    /**
     * Constructs a new request for the given capping thread type.
     *
     * <p>The value is always emitted on the wire as the object {@code {"type": <input>}}.
     *
     * @param input the capping thread type (for example {@code "INDIVIDUAL_NEW_CHAT_THREAD"})
     */
    public FetchNewChatMessageCappingInfoMexRequest(String input) {
        this.input = input;
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
     * {@link JSONWriter}, always materialising the declared {@code input} variable as the object
     * {@code {"type": <input>}}, then wraps the payload via
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewChatMessageCappingInfoJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            writer.writeName("type");
            writer.writeColon();
            writer.writeString(input);
            writer.endObject();
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
