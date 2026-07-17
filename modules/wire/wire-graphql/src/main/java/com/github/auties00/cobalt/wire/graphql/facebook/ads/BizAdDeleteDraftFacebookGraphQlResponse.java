package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the delete-ad-draft mutation built by
 * {@link BizAdDeleteDraftFacebookGraphQlRequest} into a {@link BusinessAdMutationResult}.
 *
 * <p>Projects the single root scalar {@code delete_ads_ctwa_draft}, which reports whether the draft
 * was deleted, onto the shared mutation-result shape. Only an explicit {@code true} is treated as a
 * successful deletion.
 *
 * @implNote This implementation reads {@code delete_ads_ctwa_draft} as a nullable {@code Boolean}
 * deletion outcome. The {@code useWAWebBizAdDeleteDraftMutation} document is absent from the static
 * bundle of snapshot {@code 1040120866}, so the scalar's exact GraphQL type is not confirmable; the
 * field name follows WhatsApp Web's {@code delete_*} success-flag convention, and the lenient
 * {@link JSONObject#getBoolean(String)} coercion accepts a boolean, the strings {@code "true"} and
 * {@code "false"}, and numeric truthiness without throwing on an unexpected shape.
 *
 * @see BizAdDeleteDraftFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdDeleteDraftMutation")
public final class BizAdDeleteDraftFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected mutation result, or {@code null} when {@code data} was {@code null}.
     */
    private final BusinessAdMutationResult result;

    /**
     * Constructs a response wrapping the projected mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected mutation result
     */
    private BizAdDeleteDraftFacebookGraphQlResponse(BusinessAdMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code delete_ads_ctwa_draft} scalar onto a {@link BusinessAdMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdDeleteDraftFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var result = new BusinessAdMutationResultBuilder()
                .success(Boolean.TRUE.equals(data.getBoolean("delete_ads_ctwa_draft")))
                .build();
        return Optional.of(new BizAdDeleteDraftFacebookGraphQlResponse(result));
    }

    /**
     * Returns the projected mutation result.
     *
     * @return the projected {@link BusinessAdMutationResult}, or empty when {@code data} was
     *         {@code null}
     */
    public Optional<BusinessAdMutationResult> result() {
        return Optional.ofNullable(result);
    }
}
