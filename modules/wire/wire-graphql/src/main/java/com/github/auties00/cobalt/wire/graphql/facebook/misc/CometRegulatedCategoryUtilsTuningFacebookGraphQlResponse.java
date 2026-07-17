package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.AdTargetingTuningResult;
import com.github.auties00.cobalt.wire.linked.business.ads.AdTargetingTuningResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the regulated-category tuning query built by
 * {@link CometRegulatedCategoryUtilsTuningFacebookGraphQlRequest} into an
 * {@link AdTargetingTuningResult}.
 *
 * <p>Reads the linked {@code hec.tune_target_spec_for_category} sub-object and projects its
 * {@code target_spec_string} scalar (the targeting spec the server rewrote to comply with the
 * requested regulated category) onto the {@link AdTargetingTuningResult}.
 *
 * @see CometRegulatedCategoryUtilsTuningFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "LWICometRegulatedCategoryUtilsTuningQuery")
public final class CometRegulatedCategoryUtilsTuningFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed tuning result.
     */
    private final AdTargetingTuningResult result;

    /**
     * Constructs a response wrapping the parsed tuning result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the parsed tuning result
     */
    private CometRegulatedCategoryUtilsTuningFacebookGraphQlResponse(AdTargetingTuningResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code hec.tune_target_spec_for_category} sub-object and projects its
     * {@code target_spec_string} scalar onto the {@link AdTargetingTuningResult}; the returned
     * {@link Optional} is empty when {@code data}, the {@code hec} root, or the
     * {@code tune_target_spec_for_category} sub-object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data}, the {@code hec} root, or the
     *         {@code tune_target_spec_for_category} sub-object is missing
     */
    public static Optional<CometRegulatedCategoryUtilsTuningFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("hec");
        if (root == null) {
            return Optional.empty();
        }

        var spec = root.getJSONObject("tune_target_spec_for_category");
        if (spec == null) {
            return Optional.empty();
        }

        var result = new AdTargetingTuningResultBuilder()
                .tunedTargetingSpec(spec.getString("target_spec_string"))
                .build();
        return Optional.of(new CometRegulatedCategoryUtilsTuningFacebookGraphQlResponse(result));
    }

    /**
     * Returns the parsed tuning result.
     *
     * @return the parsed {@link AdTargetingTuningResult}, never {@code null}
     */
    public AdTargetingTuningResult result() {
        return result;
    }
}
