package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResultBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the reply-bot-enabled-time-update mutation built by
 * {@link BizAiReplyBotEnabledTimeUpdateFacebookGraphQlRequest} into a {@link BusinessAiMutationResult}.
 *
 * <p>Projects the linked {@code xfb_meta_ai_biz_agent_wa_update_bot_enabled_time} field, whose single
 * {@code success} scalar reports whether the active-hours window update was applied, onto the shared
 * mutation-result shape.
 *
 * @see BizAiReplyBotEnabledTimeUpdateFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiReplyBotEnabledTimeUpdateMutation")
public final class BizAiReplyBotEnabledTimeUpdateFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
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
    private BizAiReplyBotEnabledTimeUpdateFacebookGraphQlResponse(BusinessAiMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code success} scalar onto a {@link BusinessAiMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiReplyBotEnabledTimeUpdateFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_meta_ai_biz_agent_wa_update_bot_enabled_time");
        if (node == null) {
            return Optional.of(new BizAiReplyBotEnabledTimeUpdateFacebookGraphQlResponse(null));
        }

        var result = new BusinessAiMutationResultBuilder()
                .success(Boolean.TRUE.equals(node.getBoolean("success")))
                .build();
        return Optional.of(new BizAiReplyBotEnabledTimeUpdateFacebookGraphQlResponse(result));
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
