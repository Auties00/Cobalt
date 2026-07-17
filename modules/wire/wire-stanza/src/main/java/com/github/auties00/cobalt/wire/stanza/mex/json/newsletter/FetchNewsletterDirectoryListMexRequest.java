package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterDirectoryListView;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the MEX request that lists newsletters in the discovery directory under a given view.
 *
 * <p>This query drives the newsletter directory explore tab; the {@link NewsletterDirectoryListView}
 * argument selects between the {@code RECOMMENDED}, {@code NEW}, {@code POPULAR}, {@code FEATURED}
 * and {@code TRENDING} pills, the {@code countryCodes} and {@code categories} arguments narrow the
 * list to specific countries or topic categories, and {@code limit} plus {@code cursorToken} drive
 * forward pagination.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterDirectoryListJob")
public final class FetchNewsletterDirectoryListMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchNewsletterDirectoryListJobQuery.graphql} on the WhatsApp relay.
     *
     * <p>Sent as the {@code query_id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "26125047313831973";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this query.
     */
    public static final String OPERATION_NAME = "mexFetchNewsletterDirectoryList";

    /**
     * Holds the directory view pill being requested.
     */
    private final NewsletterDirectoryListView view;

    /**
     * Holds the list of ISO country codes to filter by, or {@code null} when no country filter is
     * applied.
     */
    private final List<String> countryCodes;

    /**
     * Holds the list of category enum strings to filter by, or {@code null} when no category filter
     * is applied.
     */
    private final List<String> categories;

    /**
     * Holds the page size, or {@code null} for the server default.
     */
    private final Long limit;

    /**
     * Holds the forward pagination cursor returned by the previous page, or {@code null} for the
     * first page.
     */
    private final String cursorToken;

    /**
     * Indicates whether to populate the {@code status_metadata} fragment in the response.
     */
    private final boolean fetchStatusMetadata;

    /**
     * Constructs a request for one page of the directory list view.
     *
     * <p>The {@code view} value is mapped to the GraphQL upper-case enum by
     * {@link NewsletterDirectoryListView#value()}; the {@code categories} list must already carry
     * the on-wire enum names the relay expects, and the {@code fetchStatusMetadata} flag mirrors the
     * newsletter-status receiver gating decision.
     *
     * @param view                the directory view pill
     * @param countryCodes        the ISO country codes filter, may be {@code null}
     * @param categories          the category enum-string filter, may be {@code null}
     * @param limit               the page size, may be {@code null}
     * @param cursorToken         the forward pagination cursor, may be {@code null}
     * @param fetchStatusMetadata whether to request the optional {@code status_metadata} fragment
     */
    public FetchNewsletterDirectoryListMexRequest(NewsletterDirectoryListView view,
                   List<String> countryCodes,
                   List<String> categories,
                   Long limit,
                   String cursorToken,
                   boolean fetchStatusMetadata) {
        this.view = view;
        this.countryCodes = countryCodes;
        this.categories = categories;
        this.limit = limit;
        this.cursorToken = cursorToken;
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
     * <p>Produces the
     * {@code {variables: {input: {view, filters: {country_codes, categories}, limit, start_cursor}, fetch_status_metadata}}}
     * payload; the {@code filters} object is always emitted with both {@code country_codes} and
     * {@code categories} keys (each as an empty array when the corresponding list is {@code null})
     * so the on-wire shape never drops the keys.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and wraps any {@link IOException} from the in-memory writer in an
     * {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterDirectoryListJob", exports = "mexFetchNewsletterDirectoryList",
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

            if (view != null) {
                writer.writeName("view");
                writer.writeColon();
                writer.writeString(view.value());
            }

            writer.writeName("filters");
            writer.writeColon();
            writer.startObject();
            writer.writeName("country_codes");
            writer.writeColon();
            writeStringArray(writer, countryCodes);
            writer.writeName("categories");
            writer.writeColon();
            writeStringArray(writer, categories);
            writer.endObject();

            if (limit != null) {
                writer.writeName("limit");
                writer.writeColon();
                writer.writeInt64(limit);
            }
            if (cursorToken != null) {
                writer.writeName("start_cursor");
                writer.writeColon();
                writer.writeString(cursorToken);
            }

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

    /**
     * Writes a list of strings as a JSON array into the given writer.
     *
     * <p>Emits an empty array when {@code values} is {@code null} so the enclosing object always
     * carries the array key.
     *
     * @param writer the JSON writer to emit into
     * @param values the string values to serialise, may be {@code null}
     */
    private static void writeStringArray(JSONWriter writer, List<String> values) {
        writer.startArray();
        if (values != null) {
            for (var i = 0; i < values.size(); i++) {
                if (i > 0) {
                    writer.writeComma();
                }
                writer.writeString(values.get(i));
            }
        }
        writer.endArray();
    }
}
