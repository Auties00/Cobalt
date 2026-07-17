package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX IQ stanza that batches text-status fetches for several users.
 *
 * <p>The text status is the message displayed on the Status tab and on the Search Within Chat
 * header. The request carries an {@code input} GraphQL variable holding the batch of users to query;
 * the dispatched stanza is paired with {@link FetchTextStatusListMexResponse} to consume the reply.
 *
 * @implNote This implementation accepts the {@code input} variable as an already-serialised string
 * and forwards it verbatim. WhatsApp Web instead constructs the variable as an array of
 * {@code [{jid, last_update_time, privacy_token?}]} objects, de-duplicates the JID list, batches the
 * inputs through a 50 ms window before invoking the relay, and attaches per-entry
 * {@code privacy_token.tctoken} values when the profile-scraping-protection gate is enabled. Cobalt
 * callers that need those features must materialise the array at a higher layer.
 *
 * @see FetchTextStatusListMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchTextStatusListJob")
public final class FetchTextStatusListMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled-document identifier the relay maps to the persisted query.
     *
     * <p>Emitted as the {@code query_id} attribute of the outbound {@code <query>} stanza.
     *
     * @implNote The value matches the compiled query for the WhatsApp Web snapshot this file was
     * generated against, and must be rotated together with that bundle.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchTextStatusListJobQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "24072923595647473";

    /**
     * Holds the GraphQL operation name reported alongside this request.
     *
     * <p>WhatsApp Web tags this name onto its per-operation latency metrics.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchTextStatusListJobQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexGetTextStatusList";

    /**
     * Holds the {@code input} GraphQL variable carrying the pre-serialised batch.
     */
    private final String input;

    /**
     * Constructs a request that asks for the text-status of a batch of users.
     *
     * <p>The argument must already be the JSON string representation of the
     * {@code [{jid, last_update_time}]} array the relay expects; the array is not materialised on
     * the caller's behalf.
     *
     * @param input the serialised batch payload
     */
    public FetchTextStatusListMexRequest(String input) {
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
     * @implNote This implementation always materialises the declared {@code input} variable, emitting
     * {@code {"variables": {"input": <input>}}}, and defers envelope construction
     * to {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchTextStatusListJob", exports = "mexGetTextStatusList",
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
            writer.writeString(input);
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
