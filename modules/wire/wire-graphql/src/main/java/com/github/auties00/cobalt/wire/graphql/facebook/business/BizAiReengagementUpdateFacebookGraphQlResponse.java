package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the re-engagement-update mutation built by
 * {@link BizAiReengagementUpdateFacebookGraphQlRequest} into a {@link BusinessAiMutationResult}.
 *
 * <p>Projects the linked {@code xfb_meta_ai_biz_agent_wa_update_reengagement} field onto the shared
 * mutation-result shape. The Meta graph endpoint echoes the persisted re-engagement settings rather than a status
 * flag, so the presence of the echoed stanza is read as success.
 *
 * @see BizAiReengagementUpdateFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiReengagementUpdateMutation")
public final class BizAiReengagementUpdateFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected mutation result, or {@code null} when the Meta graph endpoint omitted the field.
     */
    private final BusinessAiMutationResult result;

    /**
     * Constructs a response wrapping the projected mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected mutation result, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizAiReengagementUpdateFacebookGraphQlResponse(BusinessAiMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the echoed
     * re-engagement settings onto a {@link BusinessAiMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiReengagementUpdateFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_meta_ai_biz_agent_wa_update_reengagement");
        if (node == null) {
            return Optional.of(new BizAiReengagementUpdateFacebookGraphQlResponse(null));
        }

        var result = new BusinessAiMutationResultBuilder()
                .success(true)
                .build();
        return Optional.of(new BizAiReengagementUpdateFacebookGraphQlResponse(result));
    }

    /**
     * Returns the projected mutation result.
     *
     * @return the projected {@link BusinessAiMutationResult}, or empty when the Meta graph endpoint omitted the field
     */
    public Optional<BusinessAiMutationResult> result() {
        return Optional.ofNullable(result);
    }
}
