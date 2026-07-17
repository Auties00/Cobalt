package com.github.auties00.cobalt.wire.graphql.whatsappWeb.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiRule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiRuleBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Projects the wire-level {@code XFBMetaAIBusinessAgentWhatsAppRule} shape onto the domain
 * {@link BusinessAiRule}.
 *
 * <p>The create, update, and generate rule mutations all return the same rule shape, so this helper
 * centralises the flattening of a rule stanza (its identifier, kind, custom-instruction text, and the
 * nested {@code emojis_config} and {@code price_config} sub-objects) onto one {@link BusinessAiRule}.
 */
public final class BizAiRuleProjection {
    /**
     * Prevents instantiation of this projection helper.
     */
    private BizAiRuleProjection() {

    }

    /**
     * Projects a single {@code rule} stanza onto a {@link BusinessAiRule}.
     *
     * @param obj the rule stanza, possibly {@code null}
     * @return the projected rule, or {@code null} when {@code obj} is {@code null}
     */
    public static BusinessAiRule of(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        var emojisConfig = obj.getJSONObject("emojis_config");
        var emojiFrequency = emojisConfig == null ? null : emojisConfig.getString("emojis_freq");
        var priceConfig = obj.getJSONObject("price_config");
        var priceSharing = priceConfig == null ? null : priceConfig.getString("price_sharing");
        return new BusinessAiRuleBuilder()
                .id(obj.getString("id"))
                .ruleType(obj.getString("rule_type"))
                .customRule(obj.getString("custom_rule"))
                .emojiFrequency(emojiFrequency)
                .priceSharing(priceSharing)
                .build();
    }

    /**
     * Projects a {@code rules} array onto a list of {@link BusinessAiRule}.
     *
     * @param arr the {@code rules} array, possibly {@code null}
     * @return the projected rules, never {@code null}
     */
    public static List<BusinessAiRule> ofArray(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiRule>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var rule = of(arr.getJSONObject(i));
            if (rule != null) {
                result.add(rule);
            }
        }
        return result;
    }
}
