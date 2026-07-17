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
 * Builds the Facebook GraphQL query that fetches the billing summary content for the WhatsApp Business
 * ad-creation review step.
 *
 * <p>The query takes two GraphQL variables. The {@code asset_id} variable is the Facebook billable
 * asset id whose billable account the summary is computed for; it is a numeric Facebook id rather
 * than a WhatsApp address, so it is kept as a {@link String}. The {@code budget} variable is the
 * advertiser's chosen budget the server estimates taxes and totals against: the {@code budget} amount,
 * the {@code budget_type}, the {@code currency}, and the {@code duration_in_days}. The query returns
 * the billable account's estimated taxes and total under {@code billable_account_by_asset_id}; the
 * reply is consumed through {@link BizAdCreationSummaryContentFacebookGraphQlResponse}.
 *
 * @see BizAdCreationSummaryContentFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationSummaryContentQuery")
public final class BizAdCreationSummaryContentFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationSummaryContentQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "33567643556217656";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationSummaryContentQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationSummaryContentQuery";

    /**
     * The {@code asset_id} GraphQL variable carrying the Facebook billable asset id whose billable
     * account is summarized, or {@code null} to omit it.
     */
    private final String assetId;

    /**
     * The {@code budget} field of the {@code budget} object holding the budget amount in minor units,
     * or {@code null} to omit it.
     */
    private final Long budget;

    /**
     * The {@code budget_type} field of the {@code budget} object (for example daily or lifetime), or
     * {@code null} to omit it.
     */
    private final String budgetType;

    /**
     * The {@code currency} field of the {@code budget} object holding the currency code, or
     * {@code null} to omit it.
     */
    private final String currency;

    /**
     * The {@code duration_in_days} field of the {@code budget} object holding the campaign duration, or
     * {@code null} to omit it.
     */
    private final Integer durationInDays;

    /**
     * Constructs a summary-content query request.
     *
     * <p>The {@code assetId} is the Facebook billable asset id, and the {@code budget},
     * {@code budgetType}, {@code currency} and {@code durationInDays} populate the nested {@code budget}
     * object. Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param assetId        the Facebook billable asset id, or {@code null} to omit the variable
     * @param budget         the budget amount in minor units, or {@code null} to omit the field
     * @param budgetType     the budget-type token, or {@code null} to omit the field
     * @param currency       the currency code, or {@code null} to omit the field
     * @param durationInDays the campaign duration in days, or {@code null} to omit the field
     */
    public BizAdCreationSummaryContentFacebookGraphQlRequest(String assetId, Long budget, String budgetType, String currency, Integer durationInDays) {
        this.assetId = assetId;
        this.budget = budget;
        this.budgetType = budgetType;
        this.currency = currency;
        this.durationInDays = durationInDays;
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
     * @implNote This implementation emits {@code {"asset_id": <assetId>, "budget": {"budget": <budget>,
     * "budget_type": <budgetType>, "currency": <currency>, "duration_in_days": <durationInDays>}}},
     * writing {@code asset_id} only when non-null and each {@code budget} sub-field only when non-null,
     * and omitting the {@code budget} object entirely when all four sub-fields are {@code null}.
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

            if (budget != null || budgetType != null || currency != null || durationInDays != null) {
                writer.writeName("budget");
                writer.writeColon();
                writer.startObject();
                if (budget != null) {
                    writer.writeName("budget");
                    writer.writeColon();
                    writer.writeInt64(budget);
                }

                if (budgetType != null) {
                    writer.writeName("budget_type");
                    writer.writeColon();
                    writer.writeString(budgetType);
                }

                if (currency != null) {
                    writer.writeName("currency");
                    writer.writeColon();
                    writer.writeString(currency);
                }

                if (durationInDays != null) {
                    writer.writeName("duration_in_days");
                    writer.writeColon();
                    writer.writeInt32(durationInDays);
                }
                writer.endObject();
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
