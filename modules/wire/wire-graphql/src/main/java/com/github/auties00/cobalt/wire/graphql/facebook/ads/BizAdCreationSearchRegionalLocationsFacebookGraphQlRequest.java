package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL query that searches regional geographic locations for the WhatsApp Business
 * ad-creation audience targeting picker.
 *
 * <p>The query takes three GraphQL variables passed straight through to the
 * {@code geo_locations_search} field: {@code query} is the free-text place name the advertiser is
 * typing, {@code first} caps how many matches the relay returns (WhatsApp Web defaults it to
 * {@code 10}), and {@code locationTypes} is the list of location-type discriminators the search is
 * restricted to (for example regions or countries). The query returns the matching region-level
 * locations with their country and region identity; the reply is consumed through
 * {@link BizAdCreationSearchRegionalLocationsFacebookGraphQlResponse}.
 *
 * @see BizAdCreationSearchRegionalLocationsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSearchRegionalLocationsQuery")
public final class BizAdCreationSearchRegionalLocationsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSearchRegionalLocationsQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25916741681283976";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSearchRegionalLocationsQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationSearchRegionalLocationsQuery";

    /**
     * The {@code query} GraphQL variable carrying the free-text place name to search for, or
     * {@code null} to omit it.
     */
    private final String query;

    /**
     * The {@code first} GraphQL variable capping the number of matches returned, or {@code null} to
     * omit it.
     *
     * <p>WhatsApp Web defaults this variable to {@code 10} when the caller leaves it unset.
     */
    private final Integer first;

    /**
     * The {@code locationTypes} GraphQL variable restricting the search to the given location-type
     * discriminators, or {@code null} to omit it.
     */
    private final List<String> locationTypes;

    /**
     * Constructs a search-regional-locations query request.
     *
     * <p>Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param query         the free-text place name to search for, or {@code null} to omit the
     *                      variable
     * @param first         the maximum number of matches to return, or {@code null} to omit the
     *                      variable
     * @param locationTypes the location-type discriminators to restrict to, or {@code null} to omit
     *                      the variable
     */
    public BizAdCreationSearchRegionalLocationsFacebookGraphQlRequest(String query, Integer first, List<String> locationTypes) {
        this.query = query;
        this.first = first;
        this.locationTypes = locationTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
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
     * @implNote This implementation emits {@code {"query": <query>, "first": <first>,
     * "locationTypes": [<locationTypes>]}}, writing each variable only when its value is non-null,
     * rendering {@code locationTypes} as a JSON array of strings, and emitting {@code "{}"} when all
     * are {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (query != null) {
                writer.writeName("query");
                writer.writeColon();
                writer.writeString(query);
            }

            if (first != null) {
                writer.writeName("first");
                writer.writeColon();
                writer.writeInt32(first);
            }

            if (locationTypes != null) {
                writer.writeName("locationTypes");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < locationTypes.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(locationTypes.get(i));
                }
                writer.endArray();
            }
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
