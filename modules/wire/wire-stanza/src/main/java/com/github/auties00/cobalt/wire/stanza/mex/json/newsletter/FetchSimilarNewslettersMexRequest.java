package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the MEX request that fetches newsletters similar to a seed newsletter.
 *
 * <p>This request drives the similar-channels carousel: WA Web feeds the result straight into the
 * newsletter directory's "similar channels" rail under a seed channel detail page. Build it through
 * the constructor with the seed newsletter Jid, an optional page limit, and an optional country-code
 * scope; submit through the MEX IQ dispatcher and pair the result with
 * {@link FetchSimilarNewslettersMexResponse#of(Stanza)}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchSimilarNewslettersJob")
public final class FetchSimilarNewslettersMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchSimilarNewslettersJobQuery.graphql} on the WhatsApp relay.
     *
     * <p>Sent as the {@code id} attribute of the outgoing {@code <query>} child; the WhatsApp relay
     * refuses requests whose persisted-query id is unknown.
     */
    public static final String QUERY_ID = "26217043484590756";

    /**
     * Holds the GraphQL operation name reported by WA Web's {@code MexPerfTracker} for this query.
     */
    public static final String OPERATION_NAME = "mexFetchSimilarNewsletters";

    /**
     * Holds the seed newsletter Jid sent under {@code variables.input.newsletter_id}.
     */
    private final String newsletterId;

    /**
     * Holds the page-size limit sent under {@code variables.input.limit}, or {@code null} to defer to
     * the relay's default page size.
     */
    private final Long limit;

    /**
     * Holds the ISO country-code scope sent under {@code variables.input.country_codes}; a
     * {@code null} value is serialised as an empty array to mirror the JS coalescing.
     */
    private final List<String> countryCodes;

    /**
     * Holds the flag sent under {@code variables.fetch_status_metadata}, gating the optional
     * {@code status_metadata} sub-selection on each result.
     */
    private final boolean fetchStatusMetadata;

    /**
     * Constructs a request with the given variables.
     *
     * <p>The {@code fetchStatusMetadata} flag gates the {@code status_metadata} sub-selection; Cobalt
     * callers pass the boolean explicitly because the gating heuristic is JS-only. The
     * {@code countryCodes} list is always emitted as a {@code country_codes} array (empty when
     * {@code null}) to mirror the JS coalescing {@code n!=null?n:[]} in WA Web's caller.
     *
     * @param newsletterId        the Jid of the seed newsletter whose similar channels are requested
     * @param limit               the maximum number of similar newsletters to return, or
     *                            {@code null} to defer to the relay's default page size
     * @param countryCodes        the ISO country-code scope, or {@code null} to serialise as an
     *                            empty array
     * @param fetchStatusMetadata {@code true} to request the optional {@code status_metadata}
     *                            sub-selection
     */
    public FetchSimilarNewslettersMexRequest(String newsletterId, Long limit, List<String> countryCodes, boolean fetchStatusMetadata) {
        this.newsletterId = newsletterId;
        this.limit = limit;
        this.countryCodes = countryCodes;
        this.fetchStatusMetadata = fetchStatusMetadata;
    }

    /**
     * Returns {@link #QUERY_ID}.
     *
     * @return the persisted-query identifier of this query
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * Returns {@link #OPERATION_NAME}.
     *
     * @return the GraphQL operation name of this query
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
     * {@code {variables: {input: {newsletter_id?, limit?, country_codes}, fetch_status_metadata}}}
     * payload consumed by the persisted-query identified by {@link #QUERY_ID}; {@code newsletter_id}
     * and {@code limit} are omitted when {@code null}, while {@code country_codes} is always emitted
     * as an array (empty when {@code null}) to mirror the JS coalescing {@code n!=null?n:[]}.
     *
     * @implNote This implementation writes the GraphQL variables directly through {@link JSONWriter}
     * and delegates IQ envelope construction to {@link Json#createMexNode(String, String)}; any
     * {@link IOException} raised by the in-memory writer is wrapped in an {@link UncheckedIOException}
     * since neither sink can fail in practice.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchSimilarNewslettersJob", exports = "mexFetchSimilarNewsletters",
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
            if (newsletterId != null) {
                writer.writeName("newsletter_id");
                writer.writeColon();
                writer.writeString(newsletterId);
            }
            if (limit != null) {
                writer.writeName("limit");
                writer.writeColon();
                writer.writeInt64(limit);
            }
            writer.writeName("country_codes");
            writer.writeColon();
            writer.startArray();
            if (countryCodes != null) {
                for (var i = 0; i < countryCodes.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(countryCodes.get(i));
                }
            }
            writer.endArray();
            writer.endObject();

            writer.writeName("fetch_status_metadata");
            writer.writeColon();
            writer.writeBool(fetchStatusMetadata);

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
