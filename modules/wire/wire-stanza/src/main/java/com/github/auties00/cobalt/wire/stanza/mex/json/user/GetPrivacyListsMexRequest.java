package com.github.auties00.cobalt.wire.stanza.mex.json.user;

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
 * Builds the MEX IQ stanza that fetches a server-side privacy contact list.
 *
 * <p>A privacy contact list is the roster backing a privacy feature such as the call deny-list. The
 * {@code dhash} field carries the digest of the locally cached list and lets the relay reply with a
 * delta rather than the full roster when the cache is current. The dispatched stanza is paired with
 * {@link GetPrivacyListsMexResponse} to consume the reply.
 *
 * @implNote This implementation embeds the full
 * {@code {jid, privacy_contact_list_type: {dhash, category, type}}} shape inline and accepts the
 * components individually. WhatsApp Web instead constructs the request from a typed input payload via
 * {@code {input: {query_input: [...]}}}; Cobalt writes the same wire shape.
 *
 * @see GetPrivacyListsMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexGetPrivacyList")
public final class GetPrivacyListsMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled-document identifier the relay maps to the persisted query.
     *
     * <p>Emitted as the {@code query_id} attribute of the outbound {@code <query>} stanza.
     *
     * @implNote The value matches the compiled query for the WhatsApp Web snapshot this file was
     * generated against, and must be rotated together with that bundle.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetPrivacyListsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "26806428515612550";

    /**
     * Holds the GraphQL operation name reported alongside this request.
     *
     * <p>WhatsApp Web tags this name onto its per-operation latency metrics.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetPrivacyListsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "fetchPrivacyList";

    /**
     * Holds the {@code jid} field of the {@code query_input[0]} entry.
     */
    private final Jid jid;

    /**
     * Holds the {@code dhash} of the locally cached list, possibly {@code null}.
     */
    private final String dhash;

    /**
     * Holds the {@code category} string identifying the privacy domain.
     */
    private final String category;

    /**
     * Holds the {@code type} string identifying the allow/deny polarity.
     */
    private final String type;

    /**
     * Constructs a privacy-list fetch request.
     *
     * <p>The {@code category} and {@code type} arguments are WA enum tokens declared elsewhere in the
     * WhatsApp Web source (for example {@code "CALL"} with {@code "DENYLIST"} for the call deny-list);
     * both are forwarded verbatim. Pass the empty string for {@code dhash} when fetching a fresh
     * list, or the previously observed digest to receive a delta refresh.
     *
     * @param jid the requesting user's JID
     * @param dhash the digest of the locally cached list, or {@code null} to omit the variable
     * @param category the privacy list domain, or {@code null} to omit the variable
     * @param type the allow/deny polarity, or {@code null} to omit the variable
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    public GetPrivacyListsMexRequest(Jid jid, String dhash, String category, String type) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.dhash = dhash;
        this.category = category;
        this.type = type;
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
     * @implNote This implementation serialises
     * {@code {"variables": {"input": {"query_input": [{"jid": ..., "privacy_contact_list_type": {"dhash"?, "category"?, "type"?}}]}}}}
     * and defers envelope construction to {@link MexStanza.Request.Json#createMexNode(String, String)}.
     * The three optional sub-fields are emitted only when non-{@code null}; {@code query_input} is
     * always a single-element array since the Cobalt API takes one {@link Jid} per request.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetPrivacyList", exports = "fetchPrivacyList",
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
            writer.writeString(jid.toString());

            writer.writeName("privacy_contact_list_type");
            writer.writeColon();
            writer.startObject();
            if (dhash != null) {
                writer.writeName("dhash");
                writer.writeColon();
                writer.writeString(dhash);
            }
            if (category != null) {
                writer.writeName("category");
                writer.writeColon();
                writer.writeString(category);
            }
            if (type != null) {
                writer.writeName("type");
                writer.writeColon();
                writer.writeString(type);
            }
            writer.endObject();
            writer.endObject();
            writer.endArray();

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
