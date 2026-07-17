package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.subscription.BusinessSubscriptionEntryPoints;
import com.github.auties00.cobalt.wire.linked.business.subscription.BusinessSubscriptionEntryPointsBuilder;
import com.github.auties00.cobalt.wire.linked.business.subscription.SubscriptionEntryPoint;
import com.github.auties00.cobalt.wire.linked.business.subscription.SubscriptionEntryPointBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the subscription entry-points query built by
 * {@link FetchSubscriptionEntryPointsFacebookGraphQlRequest} into a {@link BusinessSubscriptionEntryPoints}.
 *
 * <p>Reads the linked root {@code xwa_subscription_entrypoints} (aliased
 * {@code waSubscriptionEntryPoints}) and projects its entries onto the Cobalt domain model: each entry
 * pairs the subscription type with its web entry-point eligibility flag and redirection URI.
 *
 * @see FetchSubscriptionEntryPointsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebFetchSubscriptionEntryPointsQuery")
public final class FetchSubscriptionEntryPointsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed subscription entry-points result.
     */
    private final BusinessSubscriptionEntryPoints entryPoints;

    /**
     * Constructs a response wrapping the parsed subscription entry-points result.
     *
     * <p>Reserved for the static parser.
     *
     * @param entryPoints the parsed result, or {@code null} when the Meta graph endpoint omitted the field
     */
    private FetchSubscriptionEntryPointsFacebookGraphQlResponse(BusinessSubscriptionEntryPoints entryPoints) {
        this.entryPoints = entryPoints;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root under its alias {@code waSubscriptionEntryPoints} and projects its
     * entries onto a {@link BusinessSubscriptionEntryPoints}; the returned {@link Optional} is empty
     * when {@code data} or the entry-points object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the entry-points object is missing
     */
    public static Optional<FetchSubscriptionEntryPointsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("waSubscriptionEntryPoints");
        if (node == null) {
            return Optional.empty();
        }

        var entryPoints = new BusinessSubscriptionEntryPointsBuilder()
                .entryPoints(parseEntryPoints(node.getJSONArray("subscriptionEntryPoints")))
                .build();
        return Optional.of(new FetchSubscriptionEntryPointsFacebookGraphQlResponse(entryPoints));
    }

    /**
     * Projects the {@code subscriptionEntryPoints} array onto a list of {@link SubscriptionEntryPoint}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<SubscriptionEntryPoint> parseEntryPoints(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<SubscriptionEntryPoint>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            var eligible = obj.getBoolean("webEntryPointEligibility");
            result.add(new SubscriptionEntryPointBuilder()
                    .subscriptionType(obj.getString("subscriptionType"))
                    .webEntryPointEligible(eligible != null && eligible)
                    .webEntryPointRedirectionUri(obj.getString("webEntryPointRedirectionUri"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed subscription entry-points result.
     *
     * @return the parsed {@link BusinessSubscriptionEntryPoints}, never {@code null}
     */
    public BusinessSubscriptionEntryPoints entryPoints() {
        return entryPoints;
    }
}
