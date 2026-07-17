package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.DetailedTargetingItem;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL query that fetches suggested detailed-targeting interests for the WhatsApp
 * Business ad-creation audience picker.
 *
 * <p>The query takes three GraphQL variables. The {@code adAccountID} variable is the Facebook
 * ad-account legacy id scoping the suggestion request; it is a numeric Facebook account id rather
 * than a WhatsApp address, so it is kept as a {@link String}. The {@code detailedTargetingItems}
 * variable is the list of {@link DetailedTargetingItem detailed-targeting selections} the advertiser
 * has already picked, used as the seed the server expands suggestions from. The {@code count} variable
 * is the maximum number of suggested interests to return. The query returns the suggested interests
 * under the advertiser's {@code ad_account}; the reply is consumed through
 * {@link BizAdCreationSuggestedInterestsFacebookGraphQlResponse}.
 *
 * @see BizAdCreationSuggestedInterestsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSuggestedInterestsQuery")
public final class BizAdCreationSuggestedInterestsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSuggestedInterestsQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26091585947105251";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationSuggestedInterestsQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationSuggestedInterestsQuery";

    /**
     * The {@code adAccountID} GraphQL variable carrying the Facebook ad-account legacy id scoping the
     * suggestion request, or {@code null} to omit it.
     */
    private final String adAccountId;

    /**
     * The {@code detailedTargetingItems} GraphQL variable carrying the already-selected
     * detailed-targeting seed items, written only when non-empty. Never {@code null} after
     * construction.
     */
    private final List<DetailedTargetingItem> detailedTargetingItems;

    /**
     * The {@code count} GraphQL variable carrying the maximum number of suggested interests to
     * return, or {@code null} to omit it.
     */
    private final Integer count;

    /**
     * Constructs a suggested-interests query request.
     *
     * <p>The {@code adAccountId} is the Facebook ad-account legacy id scoping the request,
     * {@code detailedTargetingItems} is the list of detailed-targeting seed items, and {@code count}
     * is the result limit. Each value that is {@code null} (or, for the list, empty) omits its variable
     * from the serialized object.
     *
     * @param adAccountId            the Facebook ad-account legacy id, or {@code null} to omit the
     *                               variable
     * @param detailedTargetingItems the {@code detailedTargetingItems} seed list, written only when
     *                               non-empty
     * @param count                  the maximum number of results to return, or {@code null} to omit
     *                               the variable
     */
    public BizAdCreationSuggestedInterestsFacebookGraphQlRequest(String adAccountId, List<DetailedTargetingItem> detailedTargetingItems, Integer count) {
        this.adAccountId = adAccountId;
        this.detailedTargetingItems = detailedTargetingItems == null ? List.of() : List.copyOf(detailedTargetingItems);
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
     * @implNote This implementation emits {@code {"adAccountID": <adAccountId>,
     * "detailedTargetingItems": [{"id": ..., "name": ..., "type": ...}, ...], "count": <count>}},
     * writing {@code adAccountID} and {@code count} only when non-null and the
     * {@code detailedTargetingItems} array only when non-empty, and emitting {@code "{}"} when all are
     * absent. Each entry is rendered by
     * {@link BizAdInputJson#writeDetailedTargetingItem(JSONWriter, DetailedTargetingItem)}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (adAccountId != null) {
                writer.writeName("adAccountID");
                writer.writeColon();
                writer.writeString(adAccountId);
            }

            if (!detailedTargetingItems.isEmpty()) {
                writer.writeName("detailedTargetingItems");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < detailedTargetingItems.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    BizAdInputJson.writeDetailedTargetingItem(writer, detailedTargetingItems.get(i));
                }
                writer.endArray();
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
