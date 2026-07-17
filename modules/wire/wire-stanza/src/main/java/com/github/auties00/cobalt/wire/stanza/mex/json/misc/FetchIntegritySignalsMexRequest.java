package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Fetches first-message-experience (FMX) integrity signals for a target user.
 *
 * <p>This query reports whether the target account is newly registered and whether starting a chat
 * with it is considered suspicious. The resulting flags drive the FMX safety nudges shown above the
 * message composer before a chat is opened with an unfamiliar contact. The target {@link #userJid}
 * must address an individual user; the FMX surface never queries groups or broadcasts.
 *
 * @implNote This implementation hard-codes {@code use_case = "CHAT_FMX"} and
 * {@code telemetry.context = "INTERACTIVE"} to match the only call-site WhatsApp Web emits today,
 * and shapes the {@code query_input} array as a single-entry list so the wire format stays
 * compatible with the batched form even though only one JID is ever requested.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchIntegritySignals")
public final class FetchIntegritySignalsMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled GraphQL query identifier for the integrity-signals query document.
     *
     * <p>The relay maps this identifier to a server-side persisted operation and never sees the
     * GraphQL text on the wire.
     */
    public static final String QUERY_ID = "26438847999065394";

    /**
     * Holds the GraphQL operation name reported to the MEX perf tracker when this query is
     * dispatched.
     *
     * <p>The name tags the query in latency and error metrics; it is kept on the request so
     * embedders mirroring that telemetry surface can emit the same tag.
     */
    public static final String OPERATION_NAME = "fetchIntegritySignals";

    /**
     * Holds the target user JID bound to the {@code query_input[0].jid} GraphQL variable.
     */
    private final Jid userJid;

    /**
     * Constructs a new request targeting the given user.
     *
     * <p>The JID must address an individual user; the FMX surface never queries groups or
     * broadcasts. Embedders typically issue the request as the user opens a chat for the first time
     * with a recipient not already in their contact list.
     *
     * @param userJid the target user JID, must not be {@code null}
     * @throws NullPointerException if {@code userJid} is {@code null}
     */
    public FetchIntegritySignalsMexRequest(Jid userJid) {
        this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
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
     * {@link JSONWriter}, packs the lone {@link Jid} into the single-element {@code query_input}
     * array, fixes the FMX {@code use_case} discriminator at {@code "CHAT_FMX"} and the telemetry
     * {@code context} at {@code "INTERACTIVE"}, then wraps the payload via
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchIntegritySignals", exports = "fetchIntegritySignals",
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

            writer.writeName("query_input");
            writer.writeColon();
            writer.startArray();
            writer.startObject();
            writer.writeName("jid");
            writer.writeColon();
            writer.writeString(userJid.toString());
            writer.writeName("integrity_signals");
            writer.writeColon();
            writer.startObject();
            writer.writeName("use_case");
            writer.writeColon();
            writer.writeString("CHAT_FMX");
            writer.endObject();
            writer.endObject();
            writer.endArray();

            writer.writeName("telemetry");
            writer.writeColon();
            writer.startObject();
            writer.writeName("context");
            writer.writeColon();
            writer.writeString("INTERACTIVE");
            writer.endObject();

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
