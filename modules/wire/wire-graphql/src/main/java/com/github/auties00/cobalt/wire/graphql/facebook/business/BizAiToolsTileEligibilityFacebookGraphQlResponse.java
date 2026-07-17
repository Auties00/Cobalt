package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiToolsEligibility;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiToolsEligibilityBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the AI-tools tile eligibility query built by
 * {@link BizAiToolsTileEligibilityFacebookGraphQlRequest} into a {@link BusinessAiToolsEligibility}.
 *
 * <p>Reads the linked {@code xfb_meta_ai_biz_agent_wa_onboarding_eligibility} root and projects
 * its scalar {@code eligible} onto the {@link BusinessAiToolsEligibility} verdict.
 *
 * @see BizAiToolsTileEligibilityFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiToolsTileEligibilityQuery")
public final class BizAiToolsTileEligibilityFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed eligibility verdict.
     */
    private final BusinessAiToolsEligibility eligibility;

    /**
     * Constructs a response wrapping the parsed eligibility verdict.
     *
     * <p>Reserved for the static parser.
     *
     * @param eligibility the parsed eligibility verdict
     */
    private BizAiToolsTileEligibilityFacebookGraphQlResponse(BusinessAiToolsEligibility eligibility) {
        this.eligibility = eligibility;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code xfb_meta_ai_biz_agent_wa_onboarding_eligibility} root and
     * projects its scalar {@code eligible} onto the {@link BusinessAiToolsEligibility} verdict;
     * the returned {@link Optional} is empty when {@code data} or the root sub-object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the root sub-object is missing
     */
    public static Optional<BizAiToolsTileEligibilityFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xfb_meta_ai_biz_agent_wa_onboarding_eligibility");
        if (root == null) {
            return Optional.empty();
        }

        var eligible = root.getBoolean("eligible");
        var eligibility = new BusinessAiToolsEligibilityBuilder()
                .eligible(eligible != null && eligible)
                .build();
        return Optional.of(new BizAiToolsTileEligibilityFacebookGraphQlResponse(eligibility));
    }

    /**
     * Returns the parsed eligibility verdict.
     *
     * @return the parsed {@link BusinessAiToolsEligibility}, never {@code null}
     */
    public BusinessAiToolsEligibility eligibility() {
        return eligibility;
    }
}
