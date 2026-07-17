package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the estimated-daily-reach query of the Click-to-WhatsApp
 * ad creation flow.
 *
 * <p>A Click-to-WhatsApp ad is a paid promotion that opens a chat with the
 * business when tapped. While reviewing the ad the merchant sees a curve
 * projecting how many people the ad is expected to reach per day for a
 * chosen budget, audience, and placement. This input carries the
 * parameters the server uses to compute that projection.
 *
 * <p>The {@link #targetingSpecAudience() audience targeting},
 * {@link #optimizationGoalInput() optimisation goal},
 * {@link #audienceOptionAudience() chosen audience option}, and
 * {@link #configuredPlacementSpec() placement specification} parameterise
 * the estimate. The {@link #adAccountId() ad account}, {@link #currency()
 * currency}, and {@link #postId() promoted post id} scope it. The
 * {@link #flowId() flow id} is an opaque correlator the editor mints to
 * group all telemetry, drafts, and queries belonging to one ad-creation
 * funnel run; the {@link #flow() flow name} is the textual step inside
 * that funnel where the estimate was requested.
 */
@ProtobufMessage(name = "BusinessAdEstimatedReachQuery")
public final class BusinessAdEstimatedReachQuery {
    /**
     * Advertising-account identifier the estimate is computed for. Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String adAccountId;

    /**
     * Audience targeting specification. Unset omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final TargetingSpec targetingSpecAudience;

    /**
     * Optimisation goal. Unset omits the variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final OptimizationGoalInput optimizationGoalInput;

    /**
     * Chosen audience option. Unset omits the variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final AdsLwiAudience audienceOptionAudience;

    /**
     * Configured placement specification. Unset omits the variable.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final PlacementSpec configuredPlacementSpec;

    /**
     * Billing currency the estimate is expressed in. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String currency;

    /**
     * Identifier of the promoted post the estimate is computed for. Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String postId;

    /**
     * Opaque correlator grouping all telemetry, drafts, and queries that
     * belong to one ad-creation funnel run. Unset omits the variable.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String flowId;

    /**
     * Textual name of the step inside the ad-creation funnel where the
     * estimate was requested. Unset omits the variable.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String flow;

    /**
     * Constructs a new {@code BusinessAdEstimatedReachQuery}. Every
     * argument may be {@code null} to omit the corresponding variable from
     * the request.
     *
     * @param adAccountId             the advertising-account identifier, or {@code null}
     * @param targetingSpecAudience   the audience targeting, or {@code null}
     * @param optimizationGoalInput   the optimisation goal, or {@code null}
     * @param audienceOptionAudience  the chosen audience option, or {@code null}
     * @param configuredPlacementSpec the configured placement, or {@code null}
     * @param currency                the billing currency, or {@code null}
     * @param postId                  the promoted post identifier, or {@code null}
     * @param flowId                  the funnel-run correlator, or {@code null}
     * @param flow                    the funnel-step name, or {@code null}
     */
    public BusinessAdEstimatedReachQuery(String adAccountId, TargetingSpec targetingSpecAudience,
                                         OptimizationGoalInput optimizationGoalInput,
                                         AdsLwiAudience audienceOptionAudience,
                                         PlacementSpec configuredPlacementSpec, String currency,
                                         String postId, String flowId, String flow) {
        this.adAccountId = adAccountId;
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
     * Returns the advertising-account identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when unset
     */
    public Optional<String> adAccountId() {
        return Optional.ofNullable(adAccountId);
    }

    /**
     * Returns the audience targeting specification.
     *
     * @return an {@link Optional} carrying the targeting spec, or empty when unset
     */
    public Optional<TargetingSpec> targetingSpecAudience() {
        return Optional.ofNullable(targetingSpecAudience);
    }

    /**
     * Returns the optimisation goal.
     *
     * @return an {@link Optional} carrying the optimisation goal, or empty when unset
     */
    public Optional<OptimizationGoalInput> optimizationGoalInput() {
        return Optional.ofNullable(optimizationGoalInput);
    }

    /**
     * Returns the chosen audience option.
     *
     * @return an {@link Optional} carrying the audience option, or empty when unset
     */
    public Optional<AdsLwiAudience> audienceOptionAudience() {
        return Optional.ofNullable(audienceOptionAudience);
    }

    /**
     * Returns the configured placement specification.
     *
     * @return an {@link Optional} carrying the placement spec, or empty when unset
     */
    public Optional<PlacementSpec> configuredPlacementSpec() {
        return Optional.ofNullable(configuredPlacementSpec);
    }

    /**
     * Returns the billing currency.
     *
     * @return an {@link Optional} carrying the currency, or empty when unset
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the identifier of the promoted post.
     *
     * @return an {@link Optional} carrying the identifier, or empty when unset
     */
    public Optional<String> postId() {
        return Optional.ofNullable(postId);
    }

    /**
     * Returns the funnel-run correlator.
     *
     * @return an {@link Optional} carrying the correlator, or empty when unset
     */
    public Optional<String> flowId() {
        return Optional.ofNullable(flowId);
    }

    /**
     * Returns the funnel-step name.
     *
     * @return an {@link Optional} carrying the name, or empty when unset
     */
    public Optional<String> flow() {
        return Optional.ofNullable(flow);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAdEstimatedReachQuery) obj;
        return Objects.equals(adAccountId, that.adAccountId)
                && Objects.equals(targetingSpecAudience, that.targetingSpecAudience)
                && Objects.equals(optimizationGoalInput, that.optimizationGoalInput)
                && Objects.equals(audienceOptionAudience, that.audienceOptionAudience)
                && Objects.equals(configuredPlacementSpec, that.configuredPlacementSpec)
                && Objects.equals(currency, that.currency)
                && Objects.equals(postId, that.postId)
                && Objects.equals(flowId, that.flowId)
                && Objects.equals(flow, that.flow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adAccountId, targetingSpecAudience, optimizationGoalInput,
                audienceOptionAudience, configuredPlacementSpec, currency, postId, flowId,
                flow);
    }

    @Override
    public String toString() {
        return "BusinessAdEstimatedReachQuery[" +
                "adAccountId=" + adAccountId + ", " +
                "targetingSpecAudience=" + targetingSpecAudience + ", " +
                "optimizationGoalInput=" + optimizationGoalInput + ", " +
                "audienceOptionAudience=" + audienceOptionAudience + ", " +
                "configuredPlacementSpec=" + configuredPlacementSpec + ", " +
                "currency=" + currency + ", " +
                "postId=" + postId + ", " +
                "flowId=" + flowId + ", " +
                "flow=" + flow + ']';
    }
}
