package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAbility;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAbilityBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAgentHome;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAgentHomeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the business-AI-abilities query built by
 * {@link BizAiAbilitiesFacebookGraphQlRequest} into a {@link BusinessAiAgentHome}.
 *
 * <p>Projects the linked {@code xfb_meta_ai_biz_agent_wa_ai_home} field, which carries the
 * {@code abilities} list, onto the {@link BusinessAiAgentHome#abilities()} facet; the other facets of
 * the home view are left empty because this query does not request them.
 *
 * @see BizAiAbilitiesFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiAbilitiesQuery")
public final class BizAiAbilitiesFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected agent-home view, or {@code null} when the Meta graph endpoint omitted the field.
     */
    private final BusinessAiAgentHome agentHome;

    /**
     * Constructs a response wrapping the projected agent-home view.
     *
     * <p>Reserved for the static parser.
     *
     * @param agentHome the projected agent-home view, or {@code null} when the Meta graph endpoint omitted the field
     */
    private BizAiAbilitiesFacebookGraphQlResponse(BusinessAiAgentHome agentHome) {
        this.agentHome = agentHome;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code abilities} list onto a {@link BusinessAiAgentHome}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiAbilitiesFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var home = data.getJSONObject("xfb_meta_ai_biz_agent_wa_ai_home");
        if (home == null) {
            return Optional.of(new BizAiAbilitiesFacebookGraphQlResponse(null));
        }

        var abilities = parseAbilities(home.getJSONArray("abilities"));
        var agentHome = new BusinessAiAgentHomeBuilder()
                .abilities(abilities)
                .build();
        return Optional.of(new BizAiAbilitiesFacebookGraphQlResponse(agentHome));
    }

    /**
     * Projects the {@code abilities} array onto a list of {@link BusinessAiAbility} values.
     *
     * @param arr the {@code abilities} array, possibly {@code null}
     * @return the projected abilities, never {@code null}
     */
    private static List<BusinessAiAbility> parseAbilities(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiAbility>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            result.add(new BusinessAiAbilityBuilder()
                    .type(obj.getString("type"))
                    .status(obj.getString("status"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the projected agent-home view carrying the available abilities.
     *
     * @return the projected {@link BusinessAiAgentHome}, or empty when the Meta graph endpoint omitted the field
     */
    public Optional<BusinessAiAgentHome> agentHome() {
        return Optional.ofNullable(agentHome);
    }
}
