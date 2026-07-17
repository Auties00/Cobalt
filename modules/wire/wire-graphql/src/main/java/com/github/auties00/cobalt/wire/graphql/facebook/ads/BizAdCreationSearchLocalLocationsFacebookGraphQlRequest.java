package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that searches local geographic locations for the WhatsApp Business
 * ad-creation audience targeting picker.
 *
 * <p>The query takes two scalar GraphQL variables passed straight through to the
 * {@code geo_locations_search} field: {@code query} is the free-text place name the advertiser is
 * typing, and {@code first} caps how many matches the relay returns (WhatsApp Web defaults it to
 * {@code 5}). The query returns the matching city-level locations with their country, region, and
 * map coordinates; the reply is consumed through
 * {@link BizAdCreationSearchLocalLocationsFacebookGraphQlResponse}.
 *
 * @see BizAdCreationSearchLocalLocationsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSearchLocalLocationsQuery")
public final class BizAdCreationSearchLocalLocationsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSearchLocalLocationsQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27552846414317661";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSearchLocalLocationsQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationSearchLocalLocationsQuery";

    /**
     * The {@code query} GraphQL variable carrying the free-text place name to search for, or
     * {@code null} to omit it.
     */
    private final String query;

    /**
     * The {@code first} GraphQL variable capping the number of matches returned, or {@code null} to
     * omit it.
     *
     * <p>WhatsApp Web defaults this variable to {@code 5} when the caller leaves it unset.
     */
    private final Integer first;

    /**
     * Constructs a search-local-locations query request.
     *
     * <p>Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param query the free-text place name to search for, or {@code null} to omit the variable
     * @param first the maximum number of matches to return, or {@code null} to omit the variable
     */
    public BizAdCreationSearchLocalLocationsFacebookGraphQlRequest(String query, Integer first) {
        this.query = query;
        this.first = first;
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
     * @implNote This implementation emits {@code {"query": <query>, "first": <first>}}, writing each
     * variable only when its value is non-null and emitting {@code "{}"} when both are {@code null}.
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
