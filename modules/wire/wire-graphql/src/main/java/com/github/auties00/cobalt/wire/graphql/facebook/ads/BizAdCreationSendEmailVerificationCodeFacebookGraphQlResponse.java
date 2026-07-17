package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the send-email-verification-code mutation built by
 * {@link BizAdCreationSendEmailVerificationCodeFacebookGraphQlRequest} into a {@link BusinessAdMutationResult}.
 *
 * <p>Projects the linked root {@code wa_ad_account_send_email_verification_code} onto the shared
 * mutation-result shape. The {@code email_sent} flag becomes the outcome and the {@code failure_reason}
 * becomes the failure reason when the email was not sent.
 *
 * @see BizAdCreationSendEmailVerificationCodeFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSendEmailVerificationCodeMutation")
public final class BizAdCreationSendEmailVerificationCodeFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
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
    private BizAdCreationSendEmailVerificationCodeFacebookGraphQlResponse(BusinessAdMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code wa_ad_account_send_email_verification_code} root onto a {@link BusinessAdMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationSendEmailVerificationCodeFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("wa_ad_account_send_email_verification_code");
        if (node == null) {
            return Optional.of(new BizAdCreationSendEmailVerificationCodeFacebookGraphQlResponse(null));
        }

        var result = new BusinessAdMutationResultBuilder()
                .success(Boolean.TRUE.equals(node.getBoolean("email_sent")))
                .errorMessage(node.getString("failure_reason"))
                .build();
        return Optional.of(new BizAdCreationSendEmailVerificationCodeFacebookGraphQlResponse(result));
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
