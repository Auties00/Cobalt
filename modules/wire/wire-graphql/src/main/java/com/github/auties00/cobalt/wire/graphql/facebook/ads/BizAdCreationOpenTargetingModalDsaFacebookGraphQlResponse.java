package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the open-targeting-modal compliance query built by
 * {@link BizAdCreationOpenTargetingModalDsaFacebookGraphQlRequest}.
 *
 * <p>Reads the single scalar {@code target_spec_subject_to_dsa} under the linked {@code lwi} root,
 * which reports whether the evaluated targeting spec triggers the European Union
 * advertising-transparency obligations that require extra disclosure.
 *
 * @see BizAdCreationOpenTargetingModalDsaFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationOpenTargetingModalDSAQuery")
public final class BizAdCreationOpenTargetingModalDsaFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds whether the evaluated targeting spec is subject to the EU advertising-transparency rules.
     */
    private final boolean subjectToEuComplianceRules;

    /**
     * Constructs a response wrapping the parsed compliance verdict.
     *
     * <p>Reserved for the static parser.
     *
     * @param subjectToEuComplianceRules whether the targeting spec is subject to the EU rules
     */
    private BizAdCreationOpenTargetingModalDsaFacebookGraphQlResponse(boolean subjectToEuComplianceRules) {
        this.subjectToEuComplianceRules = subjectToEuComplianceRules;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and reads the
     * {@code target_spec_subject_to_dsa} verdict under the {@code lwi} root.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationOpenTargetingModalDsaFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var lwi = data.getJSONObject("lwi");
        var verdict = lwi != null && Boolean.TRUE.equals(lwi.getBoolean("target_spec_subject_to_dsa"));
        return Optional.of(new BizAdCreationOpenTargetingModalDsaFacebookGraphQlResponse(verdict));
    }

    /**
     * Returns whether the evaluated targeting spec is subject to the European Union
     * advertising-transparency rules.
     *
     * @return {@code true} when the relay reported the spec subject to the rules, {@code false}
     *         otherwise
     */
    public boolean subjectToEuComplianceRules() {
        return subjectToEuComplianceRules;
    }
}
