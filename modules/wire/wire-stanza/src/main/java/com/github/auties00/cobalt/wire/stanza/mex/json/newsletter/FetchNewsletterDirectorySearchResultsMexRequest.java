package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the MEX request that searches the newsletter discovery directory by free-text query.
 *
 * <p>Backs the directory search box. The {@code searchText} argument carries the user-typed query,
 * matched against newsletter name, handle and description on the server side; the {@code categories}
 * list narrows the search to specific topic categories; and {@code limit} plus {@code cursorToken}
 * drive forward pagination. The matching response is parsed by
 * {@link FetchNewsletterDirectorySearchResultsMexResponse}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterDirectorySearchResultsJob")
public final class FetchNewsletterDirectorySearchResultsMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchNewsletterDirectorySearchResultsJobQuery.graphql} on the WhatsApp relay.
     *
     * <p>Sent as the {@code query_id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "26301059626252132";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this query.
     */
    public static final String OPERATION_NAME = "mexFetchNewsletterDirectorySearchResults";

    /**
     * Holds the user-typed free-text query.
     */
    private final String searchText;

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
     * Holds whether to populate the {@code status_metadata} fragment in the response.
     */
    private final boolean fetchStatusMetadata;

    /**
     * Constructs a request for one page of search results.
     *
     * <p>The {@code categories} list must already carry the on-wire enum names the directory expects.
     * The {@code fetchStatusMetadata} flag mirrors whether the newsletter status receiver is
     * enabled.
     *
     * @param searchText          the free-text search query
     * @param categories          the category enum-string filter, may be {@code null}
     * @param limit               the page size, may be {@code null}
     * @param cursorToken         the forward pagination cursor, may be {@code null}
     * @param fetchStatusMetadata whether to request the optional {@code status_metadata} fragment
     */
    public FetchNewsletterDirectorySearchResultsMexRequest(String searchText,
                   List<String> categories,
                   Long limit,
                   String cursorToken,
                   boolean fetchStatusMetadata) {
        this.searchText = searchText;
        this.categories = categories;
        this.limit = limit;
        this.cursorToken = cursorToken;
        this.fetchStatusMetadata = fetchStatusMetadata;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the value of {@link #QUERY_ID}.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the value of {@link #OPERATION_NAME}.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the
     * {@code {variables: {input: {search_text, categories, limit, start_cursor}, fetch_status_metadata}}}
     * payload.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and wraps any {@link IOException} from the in-memory writer in an
     * {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterDirectorySearchResultsJob", exports = "mexFetchNewsletterDirectorySearchResults",
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

            writer.writeName("search_text");
            writer.writeColon();
            writer.writeString(searchText);

            writer.writeName("categories");
            writer.writeColon();
            writer.startArray();
            if (categories != null) {
                for (var i = 0; i < categories.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(categories.get(i));
                }
            }
            writer.endArray();

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
}
