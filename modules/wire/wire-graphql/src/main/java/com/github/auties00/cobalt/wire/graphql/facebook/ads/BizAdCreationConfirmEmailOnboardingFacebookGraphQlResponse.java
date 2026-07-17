package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResultBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the confirm-email-onboarding mutation built by
 * {@link BizAdCreationConfirmEmailOnboardingFacebookGraphQlRequest} into a {@link BusinessAdMutationResult}.
 *
 * <p>Projects the {@code wa_ad_account_upsert_onboarding_data} root onto the shared mutation-result
 * shape. The {@code success} flag becomes the outcome, the {@code failure_reason} becomes the failure
 * reason when the upsert was rejected, and the confirmed onboarding email, when echoed, is reported
 * as the affected id.
 *
 * @see BizAdCreationConfirmEmailOnboardingFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationConfirmEmailOnboardingMutation")
public final class BizAdCreationConfirmEmailOnboardingFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected mutation result, or {@code null} when the relay omitted the field.
     */
    private final BusinessAdMutationResult result;

    /**
     * Constructs a response wrapping the projected mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected mutation result, or {@code null} when the relay omitted the field
     */
    private BizAdCreationConfirmEmailOnboardingFacebookGraphQlResponse(BusinessAdMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code wa_ad_account_upsert_onboarding_data} root onto a {@link BusinessAdMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationConfirmEmailOnboardingFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("wa_ad_account_upsert_onboarding_data");
        if (node == null) {
            return Optional.of(new BizAdCreationConfirmEmailOnboardingFacebookGraphQlResponse(null));
        }

        var onboardingData = node.getJSONObject("onboarding_data");
        var email = onboardingData == null ? null : onboardingData.getString("email");
        var result = new BusinessAdMutationResultBuilder()
                .success(Boolean.TRUE.equals(node.getBoolean("success")))
                .affectedIds(email == null ? List.of() : List.of(email))
                .errorMessage(node.getString("failure_reason"))
                .build();
        return Optional.of(new BizAdCreationConfirmEmailOnboardingFacebookGraphQlResponse(result));
    }

    /**
     * Returns the projected mutation result.
     *
     * @return the projected {@link BusinessAdMutationResult}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdMutationResult> result() {
        return Optional.ofNullable(result);
    }
}
