package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdMutationResultBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the pause-ad mutation built by {@link BizAdPauseFacebookGraphQlRequest} into a
 * {@link BusinessAdMutationResult}.
 *
 * <p>Projects the {@code wa_pause_boosted_component} root onto the shared mutation-result shape. The
 * root echoes the {@code id} of the ad whose delivery was paused; its presence is taken as success
 * and the echoed id is reported as the affected id.
 *
 * @see BizAdPauseFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdPauseMutation")
public final class BizAdPauseFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
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
    private BizAdPauseFacebookGraphQlResponse(BusinessAdMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code wa_pause_boosted_component} root onto a {@link BusinessAdMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdPauseFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("wa_pause_boosted_component");
        if (node == null) {
            return Optional.of(new BizAdPauseFacebookGraphQlResponse(null));
        }

        var id = node.getString("id");
        var result = new BusinessAdMutationResultBuilder()
                .success(id != null)
                .affectedIds(id == null ? List.of() : List.of(id))
                .build();
        return Optional.of(new BizAdPauseFacebookGraphQlResponse(result));
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
