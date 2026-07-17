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
 * Builds the Facebook GraphQL query that reads the WhatsApp Business ad-creation budget state for the linked
 * boosted component.
 *
 * <p>The single {@code input} GraphQL variable identifies the ad account and budget the state is read
 * for: the legacy Facebook ad account ({@code legacy_ad_account_id}), the {@code budget} amount, and
 * the {@code currency}. The query returns the boosted component's current budget amount, the
 * high-granularity budget option list, and the minimum daily-budget constraint under
 * {@code lwi.boosted_component}; the reply is consumed through
 * {@link BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlResponse}.
 *
 * @see BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationAdAccountUpdate_BudgetQuery")
public final class BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationAdAccountUpdate_BudgetQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27196835776576294";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationAdAccountUpdate_BudgetQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationAdAccountUpdate_BudgetQuery";

    /**
     * The {@code legacy_ad_account_id} field of the {@code input} object naming the funding ad
     * account, or {@code null} to omit it.
     *
     * <p>A Facebook ad-account identifier (a numeric string), not a WhatsApp address.
     */
    private final String legacyAdAccountId;

    /**
     * The {@code budget} field of the {@code input} object holding the budget amount in minor units,
     * or {@code null} to omit it.
     */
    private final Long budget;

    /**
     * The {@code currency} field of the {@code input} object holding the currency code, or
     * {@code null} to omit it.
     */
    private final String currency;

    /**
     * Constructs a budget-state query request.
     *
     * <p>Each value that is {@code null} omits its field from the serialized {@code input} object.
     *
     * @param legacyAdAccountId the funding ad-account identifier, or {@code null} to omit the field
     * @param budget            the budget amount in minor units, or {@code null} to omit the field
     * @param currency          the currency code, or {@code null} to omit the field
     */
    public BizAdCreationAdAccountUpdate_BudgetFacebookGraphQlRequest(String legacyAdAccountId, Long budget, String currency) {
        this.legacyAdAccountId = legacyAdAccountId;
        this.budget = budget;
        this.currency = currency;
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
     * @implNote This implementation emits {@code {"input": {"legacy_ad_account_id": <legacyAdAccountId>,
     * "budget": <budget>, "currency": <currency>}}}, writing each field only when its value is non-null
     * and emitting {@code {"input": {}}} when all are {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (legacyAdAccountId != null) {
                writer.writeName("legacy_ad_account_id");
                writer.writeColon();
                writer.writeString(legacyAdAccountId);
            }

            if (budget != null) {
                writer.writeName("budget");
                writer.writeColon();
                writer.writeInt64(budget);
            }

            if (currency != null) {
                writer.writeName("currency");
                writer.writeColon();
                writer.writeString(currency);
            }
            writer.endObject();
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
