package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONObject;
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
 * Builds the MEX request that updates the mutable metadata of a newsletter.
 *
 * <p>This request backs the newsletter-edit owner flow: the owner edits one or more of the
 * newsletter's name, description, picture, or reaction-codes setting, then this mutation runs with
 * only the changed fields populated in the {@code updates} object. The relay echoes the full updated
 * {@code thread_metadata} block so the client refreshes its local cache through
 * {@link UpdateNewsletterMexResponse#threadMetadata()}.
 *
 * @implNote This implementation expects the caller to own the retry policy and the merge of the
 * echoed metadata into the local cache, where the source caller wraps the mutation in a backoff
 * retry and merges the parsed response automatically.
 */
@WhatsAppWebModule(moduleName = "WAWebMexUpdateNewsletterJob")
public final class UpdateNewsletterMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of the update-newsletter mutation.
     *
     * <p>Emitted as the {@code query_id} attribute of the outgoing {@code <query>} child; the relay
     * refuses requests whose persisted-query id is unknown.
     */
    public static final String QUERY_ID = "24250201037901610";

    /**
     * Holds the GraphQL operation name reported for this mutation.
     *
     * <p>Forwarded to observability sinks that key telemetry on the operation name.
     */
    public static final String OPERATION_NAME = "mexUpdateNewsletter";

    /**
     * Holds the Jid string of the newsletter being edited.
     */
    private final String newsletterId;

    /**
     * Holds the pre-built {@code updates} object carrying the changed fields, keyed by {@code name},
     * {@code description}, {@code picture}, and {@code settings.reaction_codes.value}.
     */
    private final JSONObject updates;

    /**
     * Constructs a request that applies the given updates to the newsletter.
     *
     * <p>The {@code updates} object carries the changed fields under the
     * {@code {name, description, picture, settings}} shape; only changed fields should be populated.
     * The {@code settings} sub-object carries only the {@code reaction_codes.value} key.
     *
     * @param newsletterId the newsletter Jid being edited
     * @param updates      the pre-built {@code updates} object carrying the changed fields
     */
    public UpdateNewsletterMexRequest(String newsletterId, JSONObject updates) {
        this.newsletterId = newsletterId;
        this.updates = updates;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #QUERY_ID}, the persisted-query identifier of the mutation.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #OPERATION_NAME}, the operation name reported for this mutation.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * Serialises this request into a MEX IQ {@link StanzaBuilder} ready to be dispatched through the
     * WhatsApp relay.
     *
     * <p>Produces the {@code {variables: {newsletter_id, updates: {...}}}} payload consumed by the
     * persisted-query identified by {@link #QUERY_ID}. Both are declared top-level variables and are
     * always emitted; only the changed sub-fields inside {@code updates} are conditionally populated.
     *
     * @implNote This implementation writes the GraphQL variables directly through
     * {@link JSONWriter} and delegates IQ envelope construction to
     * {@link Json#createMexNode(String, String)}; the {@code updates} variable is emitted as a
     * structured JSON object rather than a string-encoded payload to match the GraphQL input type
     * the relay expects, and any {@link IOException} raised by the in-memory writer is wrapped in an
     * {@link UncheckedIOException} since neither sink can fail in practice.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateNewsletterJob", exports = "mexUpdateNewsletter",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("newsletter_id");
            writer.writeColon();
            writer.writeString(newsletterId);
            writer.writeName("updates");
            writer.writeColon();
            writer.writeAny(updates);
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
