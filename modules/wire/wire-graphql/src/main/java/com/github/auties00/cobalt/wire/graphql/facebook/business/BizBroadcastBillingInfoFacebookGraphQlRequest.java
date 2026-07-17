package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that fetches the billing info backing a WhatsApp Business broadcast.
 *
 * <p>The query takes three GraphQL variables: an {@code asset_id} naming the billable asset, a
 * {@code budget} for which the estimated taxes and totals are computed, and an {@code entrypoint}
 * identifying the surface that initiated the request. The Meta graph endpoint returns the billable account under
 * {@code billable_account_by_asset_id}, whose {@code billing_info} carries the payment mode, the
 * estimated-tax breakdown, the payment-section details, and any required action. The reply is
 * consumed through {@link BizBroadcastBillingInfoFacebookGraphQlResponse}.
 *
 * @see BizBroadcastBillingInfoFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizBroadcastBillingInfoQuery")
public final class BizBroadcastBillingInfoFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizBroadcastBillingInfoQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26321114247551283";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizBroadcastBillingInfoQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizBroadcastBillingInfoQuery";

    /**
     * The {@code asset_id} GraphQL variable naming the billable asset, or {@code null} to omit it.
     *
     * <p>Kept as a {@link String}: it is a Facebook billable-asset id, not a WhatsApp address.
     */
    private final String assetId;

    /**
     * The {@code budget} GraphQL variable for which estimated taxes and totals are computed, or
     * {@code null} to omit it.
     */
    private final Long budget;

    /**
     * The {@code entrypoint} GraphQL variable identifying the surface that initiated the request, or
     * {@code null} to omit it.
     */
    private final String entrypoint;

    /**
     * Constructs a broadcast-billing-info query request.
     *
     * <p>The {@code assetId} names the billable asset, the {@code budget} is the amount the estimated
     * taxes and totals are computed against, and the {@code entrypoint} identifies the originating
     * surface. Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param assetId    the billable-asset id, or {@code null} to omit the variable
     * @param budget     the budget for the tax estimate, or {@code null} to omit the variable
     * @param entrypoint the originating surface, or {@code null} to omit the variable
     */
    public BizBroadcastBillingInfoFacebookGraphQlRequest(String assetId, Long budget, String entrypoint) {
        this.assetId = assetId;
        this.budget = budget;
        this.entrypoint = entrypoint;
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
     * @implNote This implementation emits {@code {"asset_id": <assetId>, "budget": <budget>,
     * "entrypoint": <entrypoint>}}, writing each variable only when its value is non-null, rendering
     * {@code budget} as a JSON number, and emitting {@code "{}"} when all three are {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (assetId != null) {
                writer.writeName("asset_id");
                writer.writeColon();
                writer.writeString(assetId);
            }

            if (budget != null) {
                writer.writeName("budget");
                writer.writeColon();
                writer.writeInt64(budget);
            }

            if (entrypoint != null) {
                writer.writeName("entrypoint");
                writer.writeColon();
                writer.writeString(entrypoint);
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
