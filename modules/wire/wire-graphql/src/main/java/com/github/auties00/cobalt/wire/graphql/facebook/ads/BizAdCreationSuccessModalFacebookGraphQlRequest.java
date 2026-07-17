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
 * Builds the Facebook GraphQL query that renders the success modal shown after a WhatsApp Business ad is
 * created.
 *
 * <p>The query takes two scalar GraphQL variables: {@code asset_id} is the Facebook asset id of the
 * billable account looked up by {@code billable_account_by_asset_id}, and {@code budget} is the
 * campaign budget amount used to render the payment summary in the success modal. The query returns
 * the billable account's billing info and payment-section details; the reply is consumed through
 * {@link BizAdCreationSuccessModalFacebookGraphQlResponse}.
 *
 * @see BizAdCreationSuccessModalFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationSuccessModalQuery")
public final class BizAdCreationSuccessModalFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationSuccessModalQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25629101233377520";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationSuccessModalQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationSuccessModalQuery";

    /**
     * The {@code asset_id} GraphQL variable carrying the Facebook asset id of the billable account,
     * or {@code null} to omit it.
     */
    private final String assetId;

    /**
     * The {@code budget} GraphQL variable carrying the campaign budget amount in the currency's minor
     * units, or {@code null} to omit it.
     */
    private final Long budget;

    /**
     * Constructs a success-modal query request.
     *
     * <p>Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param assetId the Facebook asset id of the billable account, or {@code null} to omit the
     *                variable
     * @param budget  the campaign budget amount in minor units, or {@code null} to omit the variable
     */
    public BizAdCreationSuccessModalFacebookGraphQlRequest(String assetId, Long budget) {
        this.assetId = assetId;
        this.budget = budget;
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
     * @implNote This implementation emits {@code {"asset_id": <assetId>, "budget": <budget>}},
     * writing each variable only when its value is non-null and emitting {@code "{}"} when both are
     * {@code null}.
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
