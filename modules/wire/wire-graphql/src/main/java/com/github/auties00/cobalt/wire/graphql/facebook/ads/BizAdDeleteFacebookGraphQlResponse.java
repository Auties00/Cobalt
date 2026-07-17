package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the delete-ad mutation built by {@link BizAdDeleteFacebookGraphQlRequest} into a
 * {@link BusinessAdMutationResult}.
 *
 * <p>Projects the {@code wa_delete_boosted_component} root, whose {@code success} flag reports whether
 * the live ad was deleted, onto the shared mutation-result shape.
 *
 * @see BizAdDeleteFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdDeleteMutation")
public final class BizAdDeleteFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
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
    private BizAdDeleteFacebookGraphQlResponse(BusinessAdMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code wa_delete_boosted_component} root onto a {@link BusinessAdMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdDeleteFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("wa_delete_boosted_component");
        if (node == null) {
            return Optional.of(new BizAdDeleteFacebookGraphQlResponse(null));
        }

        var result = new BusinessAdMutationResultBuilder()
                .success(Boolean.TRUE.equals(node.getBoolean("success")))
                .build();
        return Optional.of(new BizAdDeleteFacebookGraphQlResponse(result));
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
