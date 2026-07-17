package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.whatsappWeb.business.BizAiRuleProjection;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiRule;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the create-business-AI-rule mutation built by
 * {@link BizAiRuleCreateFacebookGraphQlRequest} into a {@link BusinessAiRule}.
 *
 * <p>Projects the linked {@code xfb_meta_ai_biz_agent_wa_create_rule} field: when the Meta graph endpoint reports
 * success and echoes the created rule, the rule's identifier, kind, custom-instruction text, and the
 * emoji-frequency and price-sharing markers are projected onto a {@link BusinessAiRule}. The
 * projection is absent when the Meta graph endpoint reported failure or echoed no rule.
 *
 * @see BizAiRuleCreateFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiRuleCreateMutation")
public final class BizAiRuleCreateFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected created rule, or {@code null} when the Meta graph endpoint reported failure or echoed no
     * rule.
     */
    private final BusinessAiRule rule;

    /**
     * Constructs a response wrapping the projected created rule.
     *
     * <p>Reserved for the static parser.
     *
     * @param rule the projected created rule, or {@code null} when the Meta graph endpoint reported failure or echoed
     *             no rule
     */
    private BizAiRuleCreateFacebookGraphQlResponse(BusinessAiRule rule) {
        this.rule = rule;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * created rule onto a {@link BusinessAiRule}.
     *
     * <p>Reads the linked root {@code xfb_meta_ai_biz_agent_wa_create_rule}; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiRuleCreateFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_meta_ai_biz_agent_wa_create_rule");
        if (node == null || !Boolean.TRUE.equals(node.getBoolean("success"))) {
            return Optional.of(new BizAiRuleCreateFacebookGraphQlResponse(null));
        }

        var rule = BizAiRuleProjection.of(node.getJSONObject("rule"));
        return Optional.of(new BizAiRuleCreateFacebookGraphQlResponse(rule));
    }

    /**
     * Returns the projected created rule.
     *
     * @return the projected {@link BusinessAiRule}, or empty when the Meta graph endpoint reported failure or echoed
     *         no rule
     */
    public Optional<BusinessAiRule> rule() {
        return Optional.ofNullable(rule);
    }
}
