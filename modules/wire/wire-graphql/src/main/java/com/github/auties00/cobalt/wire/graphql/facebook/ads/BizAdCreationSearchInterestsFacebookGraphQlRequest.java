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
 * Builds the Facebook GraphQL query that searches detailed-targeting interests for the WhatsApp Business
 * ad-creation audience picker.
 *
 * <p>The query takes three GraphQL variables. The {@code query} variable carries the free-text search
 * term the advertiser typed. The {@code adAccountID} variable is the Facebook ad-account legacy id
 * scoping the search; it is a numeric Facebook account id rather than a WhatsApp address, so it is
 * kept as a {@link String}. The {@code count} variable is the maximum number of interest results to
 * return. The query returns the matching detailed-targeting interests; the reply is consumed through
 * {@link BizAdCreationSearchInterestsFacebookGraphQlResponse}.
 *
 * @see BizAdCreationSearchInterestsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSearchInterestsQuery")
public final class BizAdCreationSearchInterestsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSearchInterestsQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25330358363249831";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSearchInterestsQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationSearchInterestsQuery";

    /**
     * The {@code query} GraphQL variable carrying the free-text interest search term, or {@code null}
     * to omit it.
     */
    private final String query;

    /**
     * The {@code adAccountID} GraphQL variable carrying the Facebook ad-account legacy id scoping the
     * search, or {@code null} to omit it.
     */
    private final String adAccountId;

    /**
     * The {@code count} GraphQL variable carrying the maximum number of interest results to return, or
     * {@code null} to omit it.
     */
    private final Integer count;

    /**
     * Constructs a search-interests query request.
     *
     * <p>The {@code query} is the free-text term to search, {@code adAccountId} is the Facebook
     * ad-account legacy id scoping the search, and {@code count} is the result limit. Each value that
     * is {@code null} omits its variable from the serialized object.
     *
     * @param query       the free-text interest search term, or {@code null} to omit the variable
     * @param adAccountId the Facebook ad-account legacy id, or {@code null} to omit the variable
     * @param count       the maximum number of results to return, or {@code null} to omit the variable
     */
    public BizAdCreationSearchInterestsFacebookGraphQlRequest(String query, String adAccountId, Integer count) {
        this.query = query;
        this.adAccountId = adAccountId;
        this.count = count;
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
     * @implNote This implementation emits {@code {"query": <query>, "adAccountID": <adAccountId>,
     * "count": <count>}}, writing each variable only when its value is non-null and emitting
     * {@code "{}"} when all are {@code null}.
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

            if (adAccountId != null) {
                writer.writeName("adAccountID");
                writer.writeColon();
                writer.writeString(adAccountId);
            }

            if (count != null) {
                writer.writeName("count");
                writer.writeColon();
                writer.writeInt32(count);
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
