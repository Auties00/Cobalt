package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

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
 * Builds the MEX request that fetches a preview of newsletter directory categories with featured
 * channels.
 *
 * <p>This query drives the newsletter directory landing screen: the relay returns one entry per
 * requested category, each carrying a handful of featured newsletters for visual preview. The
 * {@code input} payload is supplied as a pre-serialised JSON object string so callers can shape the
 * categories list, country code and per-category limit themselves; it is spliced into the request as
 * a raw JSON value.
 *
 * @implNote This implementation embeds the caller-supplied pre-serialised {@code input} payload as a
 * raw JSON object value (not a quoted string), matching the structured object WhatsApp Web sends with
 * {@code categories}, {@code country_code} and {@code per_category_limit} entries alongside
 * {@code fetch_status_metadata}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterDirectoryCategoriesPreviewJob")
public final class FetchNewsletterDirectoryCategoriesPreviewMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchNewsletterDirectoryCategoriesPreviewJobQuery.graphql} on the WhatsApp
     * relay.
     *
     * <p>Sent as the {@code query_id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "35266481849605779";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this query.
     */
    public static final String OPERATION_NAME = "mexFetchNewsletterDirectoryCategoriesPreview";

    /**
     * Holds the pre-serialised {@code input} GraphQL variable payload, a JSON object carrying the
     * {@code categories} list, {@code country_code} and {@code per_category_limit}.
     */
    private final String input;

    /**
     * Holds whether to populate the {@code status_metadata} fragment in the response.
     */
    private final boolean fetchStatusMetadata;

    /**
     * Constructs a request with the given pre-serialised {@code input} payload without requesting
     * the optional {@code status_metadata} fragment.
     *
     * <p>Equivalent to {@link #FetchNewsletterDirectoryCategoriesPreviewMexRequest(String, boolean)}
     * with {@code fetchStatusMetadata} set to {@code false}.
     *
     * @param input the pre-serialised input payload, or {@code null} to omit
     */
    public FetchNewsletterDirectoryCategoriesPreviewMexRequest(String input) {
        this(input, false);
    }

    /**
     * Constructs a request with the given pre-serialised {@code input} payload.
     *
     * <p>The pre-serialised {@code input} object is spliced as a raw JSON value under the
     * {@code variables.input} key; callers are responsible for matching the schema (categories list,
     * country code, per-category limit). The {@code fetchStatusMetadata} flag mirrors whether the
     * newsletter status receiver is enabled.
     *
     * @param input               the pre-serialised input payload, or {@code null} to omit
     * @param fetchStatusMetadata whether to request the optional {@code status_metadata} fragment
     */
    public FetchNewsletterDirectoryCategoriesPreviewMexRequest(String input, boolean fetchStatusMetadata) {
        this.input = input;
        this.fetchStatusMetadata = fetchStatusMetadata;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #QUERY_ID}.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #OPERATION_NAME}.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code {variables: {input: {...}, fetch_status_metadata}}} envelope; the
     * pre-serialised {@link #input} payload is spliced in as a raw JSON object and is omitted only
     * when {@code null} so the GraphQL schema never receives an explicit {@code null} variable, while
     * {@code fetch_status_metadata} is always emitted.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter}, splicing the pre-serialised {@link #input} object in via
     * {@link JSONWriter#writeRaw(String)}, and wraps any {@link IOException} from the in-memory writer
     * in an {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterDirectoryCategoriesPreviewJob", exports = "mexFetchNewsletterDirectoryCategoriesPreview",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            if (input != null) {
                writer.writeName("input");
                writer.writeColon();
                writer.writeRaw(input);
            }
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
