package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.facebook.ads.BizAdInputJson;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.AdsLwiAudience;
import com.github.auties00.cobalt.wire.linked.business.ads.OptimizationGoalInput;
import com.github.auties00.cobalt.wire.linked.business.ads.PlacementSpec;
import com.github.auties00.cobalt.wire.linked.business.ads.TargetingSpec;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that estimates the daily reach of a WhatsApp Business ad given a targeting
 * spec, optimisation goal, and budget configuration.
 *
 * <p>The query takes nine GraphQL variables. {@code legacyAdAccountID} is the Facebook ad account the
 * estimate is computed for and {@code postID} is the Facebook post being boosted. {@code currency},
 * {@code flowID} and {@code flow} are scalar strings identifying the billing currency and the
 * ad-creation flow the estimate belongs to. {@code targetingSpecAudience}, {@code optimizationGoalInput},
 * {@code audienceOptionAudience} and {@code configuredPlacementSpec} are GraphQL input objects that
 * parameterise the estimate: the audience targeting spec, the optimisation goal, the chosen audience
 * option, and the configured placement spec. The relay returns the budget-estimate curve under the
 * linked {@code lwi} root's {@code budget_estimate_data_v2.daily_outcomes_curve}; the reply is
 * consumed through {@link EstimatedDailyReachFacebookGraphQlResponse}.
 *
 * @implNote This implementation derives its variables from the operation spec because the
 * {@code useWAWebEstimatedDailyReachQuery} module and its compiled {@code .graphql} document are not
 * present in the static bundle of snapshot {@code 1040120866}; it is one of the Comet ad-creation
 * documents loaded on demand. {@code legacyAdAccountID} and {@code postID} are Facebook identifiers
 * (numeric strings), not WhatsApp addresses, so they are modelled as {@code String} rather than
 * {@link com.github.auties00.cobalt.wire.core.jid.Jid}; {@code currency}, {@code flowID} and {@code flow}
 * are scalar {@code String} variables. The {@code targetingSpecAudience}, {@code optimizationGoalInput},
 * {@code audienceOptionAudience} and {@code configuredPlacementSpec} variables are the typed
 * {@link TargetingSpec}, {@link OptimizationGoalInput}, {@link AdsLwiAudience} and {@link PlacementSpec}
 * input objects, mapped to their snake_case JSON shapes by {@link BizAdInputJson}.
 *
 * @see EstimatedDailyReachFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebEstimatedDailyReachQuery")
public final class EstimatedDailyReachFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebEstimatedDailyReachQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26555147174103537";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebEstimatedDailyReachQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebEstimatedDailyReachQuery";

    /**
     * The {@code legacyAdAccountID} GraphQL variable naming the Facebook ad account the estimate is
     * computed for, or {@code null} to omit it.
     *
     * <p>A Facebook ad-account identifier (a numeric string), not a WhatsApp address.
     */
    private final String legacyAdAccountId;

    /**
     * The {@code targetingSpecAudience} GraphQL input object carrying the audience targeting spec, or
     * {@code null} to omit it.
     */
    private final TargetingSpec targetingSpecAudience;

    /**
     * The {@code optimizationGoalInput} GraphQL input object carrying the optimisation goal, or
     * {@code null} to omit it.
     */
    private final OptimizationGoalInput optimizationGoalInput;

    /**
     * The {@code audienceOptionAudience} GraphQL input object carrying the chosen audience option, or
     * {@code null} to omit it.
     */
    private final AdsLwiAudience audienceOptionAudience;

    /**
     * The {@code configuredPlacementSpec} GraphQL input object carrying the configured placement spec,
     * or {@code null} to omit it.
     */
    private final PlacementSpec configuredPlacementSpec;

    /**
     * The {@code currency} GraphQL variable naming the billing currency the estimate is expressed in,
     * or {@code null} to omit it.
     */
    private final String currency;

    /**
     * The {@code postID} GraphQL variable naming the Facebook post being boosted, or {@code null} to
     * omit it.
     *
     * <p>A Facebook post identifier (a numeric string), not a WhatsApp address.
     */
    private final String postId;

    /**
     * The {@code flowID} GraphQL variable identifying the ad-creation flow instance, or {@code null}
     * to omit it.
     */
    private final String flowId;

    /**
     * The {@code flow} GraphQL variable naming the ad-creation flow, or {@code null} to omit it.
     */
    private final String flow;

    /**
     * Constructs an estimated-daily-reach query request.
     *
     * <p>The {@code legacyAdAccountId}, {@code currency}, {@code postId}, {@code flowId} and
     * {@code flow} populate the corresponding scalar GraphQL variables. The
     * {@code targetingSpecAudience}, {@code optimizationGoalInput}, {@code audienceOptionAudience} and
     * {@code configuredPlacementSpec} are the typed input objects the estimate is parameterised by.
     * Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param legacyAdAccountId       the Facebook ad-account identifier, or {@code null} to omit
     *                                the variable
     * @param targetingSpecAudience   the {@code targetingSpecAudience} object, or {@code null} to omit
     *                                the variable
     * @param optimizationGoalInput   the {@code optimizationGoalInput} object, or {@code null} to omit
     *                                the variable
     * @param audienceOptionAudience  the {@code audienceOptionAudience} object, or {@code null} to omit
     *                                the variable
     * @param configuredPlacementSpec the {@code configuredPlacementSpec} object, or {@code null} to
     *                                omit the variable
     * @param currency                the billing currency, or {@code null} to omit the variable
     * @param postId                  the Facebook post identifier, or {@code null} to omit the variable
     * @param flowId                  the ad-creation flow instance identifier, or {@code null} to omit
     *                                the variable
     * @param flow                    the ad-creation flow name, or {@code null} to omit the variable
     */
    public EstimatedDailyReachFacebookGraphQlRequest(String legacyAdAccountId, TargetingSpec targetingSpecAudience, OptimizationGoalInput optimizationGoalInput, AdsLwiAudience audienceOptionAudience, PlacementSpec configuredPlacementSpec, String currency, String postId, String flowId, String flow) {
        this.legacyAdAccountId = legacyAdAccountId;
        this.targetingSpecAudience = targetingSpecAudience;
        this.optimizationGoalInput = optimizationGoalInput;
        this.audienceOptionAudience = audienceOptionAudience;
        this.configuredPlacementSpec = configuredPlacementSpec;
        this.currency = currency;
        this.postId = postId;
        this.flowId = flowId;
        this.flow = flow;
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
     * @implNote This implementation emits {@code {"legacyAdAccountID": <legacyAdAccountId>,
     * "targetingSpecAudience": {...}, "optimizationGoalInput": {...}, "audienceOptionAudience": {...},
     * "configuredPlacementSpec": {...}, "currency": <currency>, "postID": <postId>, "flowID": <flowId>,
     * "flow": <flow>}}, writing each variable only when its value is non-null and emitting {@code "{}"}
     * when all are {@code null}. The four typed input objects are mapped to their snake_case JSON
     * shapes by {@link BizAdInputJson}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (legacyAdAccountId != null) {
                writer.writeName("legacyAdAccountID");
                writer.writeColon();
                writer.writeString(legacyAdAccountId);
            }

            if (targetingSpecAudience != null) {
                writer.writeName("targetingSpecAudience");
                writer.writeColon();
                BizAdInputJson.writeTargetingSpec(writer, targetingSpecAudience);
            }

            if (optimizationGoalInput != null) {
                writer.writeName("optimizationGoalInput");
                writer.writeColon();
                BizAdInputJson.writeOptimizationGoalInput(writer, optimizationGoalInput);
            }

            if (audienceOptionAudience != null) {
                writer.writeName("audienceOptionAudience");
                writer.writeColon();
                BizAdInputJson.writeAdsLwiAudience(writer, audienceOptionAudience);
            }

            if (configuredPlacementSpec != null) {
                writer.writeName("configuredPlacementSpec");
                writer.writeColon();
                BizAdInputJson.writePlacementSpec(writer, configuredPlacementSpec);
            }

            if (currency != null) {
                writer.writeName("currency");
                writer.writeColon();
                writer.writeString(currency);
            }

            if (postId != null) {
                writer.writeName("postID");
                writer.writeColon();
                writer.writeString(postId);
            }

            if (flowId != null) {
                writer.writeName("flowID");
                writer.writeColon();
                writer.writeString(flowId);
            }

            if (flow != null) {
                writer.writeName("flow");
                writer.writeColon();
                writer.writeString(flow);
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
