package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.AdTargetingComplianceStatus;
import com.github.auties00.cobalt.wire.linked.business.ads.AdTargetingComplianceStatusBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the Singapore universal-category regulated-category query built by
 * {@link CometRegulatedCategoryUtilsSingaporeUniversalFacebookGraphQlRequest} into an
 * {@link AdTargetingComplianceStatus}.
 *
 * <p>Reads the linked {@code lwi} root and projects its scalar
 * {@code target_spec_subject_to_singapore_universal} onto the shared
 * {@link AdTargetingComplianceStatus} verdict.
 *
 * @see CometRegulatedCategoryUtilsSingaporeUniversalFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "LWICometRegulatedCategoryUtilsSingaporeUniversalQuery")
public final class CometRegulatedCategoryUtilsSingaporeUniversalFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed compliance status.
     */
    private final AdTargetingComplianceStatus status;

    /**
     * Constructs a response wrapping the parsed compliance status.
     *
     * <p>Reserved for the static parser.
     *
     * @param status the parsed compliance status
     */
    private CometRegulatedCategoryUtilsSingaporeUniversalFacebookGraphQlResponse(AdTargetingComplianceStatus status) {
        this.status = status;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code lwi} and projects its scalar
     * {@code target_spec_subject_to_singapore_universal} onto the shared
     * {@link AdTargetingComplianceStatus} verdict; the returned {@link Optional} is empty when
     * {@code data} or the {@code lwi} root is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the {@code lwi} root is missing
     */
    public static Optional<CometRegulatedCategoryUtilsSingaporeUniversalFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("lwi");
        if (root == null) {
            return Optional.empty();
        }

        var subject = root.getBoolean("target_spec_subject_to_singapore_universal");
        var status = new AdTargetingComplianceStatusBuilder()
                .subject(subject != null && subject)
                .build();
        return Optional.of(new CometRegulatedCategoryUtilsSingaporeUniversalFacebookGraphQlResponse(status));
    }

    /**
     * Returns the parsed compliance status.
     *
     * @return the parsed {@link AdTargetingComplianceStatus}, never {@code null}
     */
    public AdTargetingComplianceStatus status() {
        return status;
    }
}
