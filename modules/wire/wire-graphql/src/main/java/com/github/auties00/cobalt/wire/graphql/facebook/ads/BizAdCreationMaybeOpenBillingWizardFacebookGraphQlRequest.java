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
 * Builds the Facebook GraphQL query that decides whether a billing wizard must be opened before a WhatsApp
 * Business ad is paid for.
 *
 * <p>The query takes two GraphQL variables: {@code asset_id}, the Facebook asset whose billable
 * account is read, and {@code budget}, the proposed budget amount in the currency's minor units. The
 * query returns the billable account's required-action billing wizard, if any, under the linked
 * {@code billable_account_by_asset_id} root; the reply is consumed through
 * {@link BizAdCreationMaybeOpenBillingWizardFacebookGraphQlResponse}.
 *
 * @implNote This implementation models {@code asset_id} as a Facebook asset identifier (a numeric
 * string), not a WhatsApp address, so it is a {@code String} rather than a
 * {@link com.github.auties00.cobalt.wire.core.jid.Jid}; {@code budget} is modelled as a {@link Long}
 * amount in the currency's minor units. The variable shape matches the sibling
 * {@code WAWebBizAdCreationPaymentSectionQuery.graphql} document, whose {@code asset_id} and
 * {@code budget} {@code LocalArgument}s map onto the same {@code billable_account_by_asset_id} and
 * {@code billing_info} field arguments.
 *
 * @see BizAdCreationMaybeOpenBillingWizardFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationMaybeOpenBillingWizardQuery")
public final class BizAdCreationMaybeOpenBillingWizardFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationMaybeOpenBillingWizardQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "32843053468676588";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationMaybeOpenBillingWizardQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationMaybeOpenBillingWizardQuery";

    /**
     * The {@code asset_id} GraphQL variable naming the Facebook asset whose billable account is read,
     * or {@code null} to omit it.
     *
     * <p>A Facebook asset identifier (a numeric string), not a WhatsApp address.
     */
    private final String assetId;

    /**
     * The {@code budget} GraphQL variable carrying the proposed budget amount in the currency's minor
     * units, or {@code null} to omit it.
     */
    private final Long budget;

    /**
     * Constructs a maybe-open-billing-wizard query request.
     *
     * <p>The {@code assetId} populates the {@code asset_id} GraphQL variable and the {@code budget}
     * populates the {@code budget} GraphQL variable; each value that is {@code null} omits its
     * variable from the serialized object.
     *
     * @param assetId the Facebook asset identifier, or {@code null} to omit the variable
     * @param budget  the proposed budget amount in minor units, or {@code null} to omit the variable
     */
    public BizAdCreationMaybeOpenBillingWizardFacebookGraphQlRequest(String assetId, Long budget) {
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
     * @implNote This implementation emits {@code {"asset_id": <assetId>, "budget": <budget>}}, writing
     * each variable only when its value is non-null and emitting {@code "{}"} when both are
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
