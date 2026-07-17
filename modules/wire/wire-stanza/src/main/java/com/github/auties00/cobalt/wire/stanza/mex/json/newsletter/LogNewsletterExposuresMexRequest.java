package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterExposure;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * Builds the MEX request that uploads a batch of newsletter capability exposure events.
 *
 * <p>This request backs a low-priority background job: as the client renders newsletter surfaces it
 * queues one {@link NewsletterExposure} per (newsletter Jid, capability) pair the user was exposed
 * to, then flushes the batch through this mutation so the relay can drive directory ranking and
 * feature-rollout analytics. Construct it with a non-null (possibly empty) batch and submit it
 * through the MEX IQ dispatcher; the relay returns no data of interest, so
 * {@link LogNewsletterExposuresMexResponse} is a presence marker.
 */
@WhatsAppWebModule(moduleName = "WAWebMexLogNewsletterExposuresJob")
public final class LogNewsletterExposuresMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of the log-newsletter-exposures mutation.
     *
     * <p>Emitted as the {@code query_id} attribute of the outgoing {@code <query>} child; the relay
     * refuses requests whose persisted-query id is unknown.
     */
    public static final String QUERY_ID = "25260800823586918";

    /**
     * Holds the GraphQL operation name reported for this mutation.
     *
     * <p>Forwarded to observability sinks that key telemetry on the operation name.
     */
    public static final String OPERATION_NAME = "mexLogNewsletterExposures";

    /**
     * Holds the defensive copy of the exposure batch this request flushes.
     */
    private final List<NewsletterExposure> exposures;

    /**
     * Constructs a request that uploads the given batch of exposures.
     *
     * <p>Each {@link NewsletterExposure} pairs a newsletter Jid with a capability identifier. An
     * empty list is accepted and produces an empty {@code exposures} array on the wire; a
     * {@code null} list is rejected so callers cannot silently send malformed batches.
     *
     * @param exposures the batch of exposure entries; never {@code null}, may be empty
     * @throws NullPointerException if {@code exposures} is {@code null}
     */
    public LogNewsletterExposuresMexRequest(List<NewsletterExposure> exposures) {
        Objects.requireNonNull(exposures, "exposures cannot be null");
        this.exposures = List.copyOf(exposures);
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
     * <p>Produces the
     * {@code {variables: {input: {exposures: [{newsletter_id, capability}, ...]}}}} payload consumed
     * by the persisted-query identified by {@link #QUERY_ID}. The {@code newsletter_id} and
     * {@code capability} fields are written as JSON {@code null} when the underlying
     * {@link NewsletterExposure#newsletterId()} or {@link NewsletterExposure#capability()} accessor
     * is empty.
     *
     * @implNote This implementation writes the GraphQL variables directly through
     * {@link JSONWriter} and delegates IQ envelope construction to
     * {@link Json#createMexNode(String, String)}; any {@link IOException} raised by the in-memory
     * writer is wrapped in an {@link UncheckedIOException} since neither sink can fail in practice.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexLogNewsletterExposuresJob", exports = "mexLogNewsletterExposures",
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

            writer.writeName("exposures");
            writer.writeColon();
            writer.startArray();
            for (var i = 0; i < exposures.size(); i++) {
                if (i > 0) {
                    writer.writeComma();
                }
                var exposure = exposures.get(i);
                writer.startObject();
                writer.writeName("newsletter_id");
                writer.writeColon();
                writer.writeString(exposure.newsletterId().orElse(null));
                writer.writeName("capability");
                writer.writeColon();
                writer.writeString(exposure.capability().orElse(null));
                writer.endObject();
            }
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
