package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.LwiBoostedComponentInput;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that renders the audience section of the WhatsApp Business ad-creation flow.
 *
 * <p>The query takes seven GraphQL variables. The {@code input} is the {@link LwiBoostedComponentInput}
 * WhatsApp Web passes to
 * {@code lwi.boosted_component(caller: "AUDIENCE_SECTION_RENDERING", input: $input)} (the query
 * re-sends the boosted-component input). The {@code objective}, {@code budget}, {@code budget_type} and
 * {@code duration_in_seconds} variables parameterise the {@code audiences_v2} suggestion set;
 * {@code adAccountID} is the Facebook ad-account legacy id used to look up saved audiences;
 * {@code savedAudienceCount} is the Relay connection page size requested for the saved-audience list.
 * The query returns the suggested audiences, the template target spec, and the paged saved audiences;
 * the reply is consumed through {@link BizAdCreationAudienceSectionFacebookGraphQlResponse}.
 *
 * @implNote This implementation maps the typed {@code input} object to its snake_case JSON shape by
 * {@link BizAdInputJson}. The {@code budget} and {@code duration_in_seconds} variables are modelled as
 * {@link Long} amounts and {@code savedAudienceCount} as an {@link Integer} page size; {@code objective}
 * and {@code budget_type} are kept as {@link String} because their closed value sets are not
 * confirmable from the static bundle.
 *
 * @see BizAdCreationAudienceSectionFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationAudienceSectionQuery")
public final class BizAdCreationAudienceSectionFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceSectionQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26377538738598766";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAdCreationAudienceSectionQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAdCreationAudienceSectionQuery";

    /**
     * The {@code input} GraphQL object identifying the boosted component the audience suggestions are
     * computed for, or {@code null} to omit it.
     */
    private final LwiBoostedComponentInput input;

    /**
     * The {@code objective} GraphQL variable naming the campaign objective, or {@code null} to omit
     * it.
     */
    private final String objective;

    /**
     * The {@code budget} GraphQL variable carrying the budget amount in the currency's minor units,
     * or {@code null} to omit it.
     */
    private final Long budget;

    /**
     * The {@code budget_type} GraphQL variable naming the budget cadence, or {@code null} to omit it.
     */
    private final String budgetType;

    /**
     * The {@code duration_in_seconds} GraphQL variable carrying the campaign duration in seconds, or
     * {@code null} to omit it.
     */
    private final Long durationInSeconds;

    /**
     * The {@code adAccountID} GraphQL variable carrying the Facebook ad-account legacy id, or
     * {@code null} to omit it.
     */
    private final String adAccountId;

    /**
     * The {@code savedAudienceCount} GraphQL variable carrying the requested saved-audience page
     * size, or {@code null} to omit it.
     */
    private final Integer savedAudienceCount;

    /**
     * Constructs an audience-section query request.
     *
     * <p>The {@code input} identifies the boosted component the suggestions are computed for. Each
     * value that is {@code null} omits its variable from the serialized object.
     *
     * @param input              the boosted-component input, or {@code null} to omit the variable
     * @param objective          the campaign objective, or {@code null} to omit the variable
     * @param budget             the budget amount in minor units, or {@code null} to omit the
     *                           variable
     * @param budgetType         the budget cadence, or {@code null} to omit the variable
     * @param durationInSeconds  the campaign duration in seconds, or {@code null} to omit the
     *                           variable
     * @param adAccountId        the Facebook ad-account legacy id, or {@code null} to omit the
     *                           variable
     * @param savedAudienceCount the requested saved-audience page size, or {@code null} to omit the
     *                           variable
     */
    public BizAdCreationAudienceSectionFacebookGraphQlRequest(LwiBoostedComponentInput input, String objective, Long budget, String budgetType, Long durationInSeconds, String adAccountId, Integer savedAudienceCount) {
        this.input = input;
        this.objective = objective;
        this.budget = budget;
        this.budgetType = budgetType;
        this.durationInSeconds = durationInSeconds;
        this.adAccountId = adAccountId;
        this.savedAudienceCount = savedAudienceCount;
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
     * @implNote This implementation emits {@code {"input": {...}, "objective": <objective>,
     * "budget": <budget>, "budget_type": <budgetType>, "duration_in_seconds": <durationInSeconds>,
     * "adAccountID": <adAccountId>, "savedAudienceCount": <savedAudienceCount>}}, writing each
     * variable only when its value is non-null and emitting {@code "{}"} when all are {@code null}.
     * The {@code input} object is mapped by
     * {@link BizAdInputJson#writeLwiBoostedComponentInput(JSONWriter, LwiBoostedComponentInput)}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (input != null) {
                writer.writeName("input");
                writer.writeColon();
                BizAdInputJson.writeLwiBoostedComponentInput(writer, input);
            }

            if (objective != null) {
                writer.writeName("objective");
                writer.writeColon();
                writer.writeString(objective);
            }

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

            if (durationInSeconds != null) {
                writer.writeName("duration_in_seconds");
                writer.writeColon();
                writer.writeInt64(durationInSeconds);
            }

            if (adAccountId != null) {
                writer.writeName("adAccountID");
                writer.writeColon();
                writer.writeString(adAccountId);
            }

            if (savedAudienceCount != null) {
                writer.writeName("savedAudienceCount");
                writer.writeColon();
                writer.writeInt32(savedAudienceCount);
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
