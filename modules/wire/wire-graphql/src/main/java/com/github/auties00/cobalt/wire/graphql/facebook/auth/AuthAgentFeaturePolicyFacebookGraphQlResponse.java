package com.github.auties00.cobalt.wire.graphql.facebook.auth;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.auth.AuthorizedAgentFeaturePolicy;
import com.github.auties00.cobalt.wire.linked.business.auth.AuthorizedAgentFeaturePolicyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the authorized-agent feature-policy query built by
 * {@link AuthAgentFeaturePolicyFacebookGraphQlRequest} into an {@link AuthorizedAgentFeaturePolicy}.
 *
 * <p>Reads the linked root {@code whatsapp_authorized_agent_feature_policy} and projects its
 * {@code disabled_features} array onto the Cobalt domain model. WhatsApp Web treats a missing policy
 * object as a signal that the session is not an authorized agent.
 *
 * @see AuthAgentFeaturePolicyFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebAuthAgentFeaturePolicyQuery")
public final class AuthAgentFeaturePolicyFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed authorized-agent feature policy.
     */
    private final AuthorizedAgentFeaturePolicy policy;

    /**
     * Constructs a response wrapping the parsed feature policy.
     *
     * <p>Reserved for the static parser.
     *
     * @param policy the parsed feature policy, or {@code null} when the Meta graph endpoint omitted the field
     */
    private AuthAgentFeaturePolicyFacebookGraphQlResponse(AuthorizedAgentFeaturePolicy policy) {
        this.policy = policy;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code whatsapp_authorized_agent_feature_policy} and projects it onto
     * an {@link AuthorizedAgentFeaturePolicy}; the returned {@link Optional} is empty when
     * {@code data} or the policy object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the policy object is missing
     */
    public static Optional<AuthAgentFeaturePolicyFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("whatsapp_authorized_agent_feature_policy");
        if (node == null) {
            return Optional.empty();
        }

        var policy = new AuthorizedAgentFeaturePolicyBuilder()
                .disabledFeatures(parseFeatureNames(node.getJSONArray("disabled_features")))
                .build();
        return Optional.of(new AuthAgentFeaturePolicyFacebookGraphQlResponse(policy));
    }

    /**
     * Projects the {@code disabled_features} array onto an immutable list of feature-name strings.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<String> parseFeatureNames(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<String>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var feature = arr.getString(i);
            if (feature != null) {
                result.add(feature);
            }
        }
        return result;
    }

    /**
     * Returns the parsed authorized-agent feature policy.
     *
     * @return the parsed {@link AuthorizedAgentFeaturePolicy}, never {@code null}
     */
    public AuthorizedAgentFeaturePolicy policy() {
        return policy;
    }
}
