package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdInterest;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdInterestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the suggested-interests query built by
 * {@link BizAdCreationSuggestedInterestsFacebookGraphQlRequest} into a list of {@link BusinessAdInterest}.
 *
 * <p>Projects the suggested detailed-targeting interests, reached through the
 * {@code ad_account.detailed_targeting_suggestions} connection, onto {@link BusinessAdInterest}
 * entries.
 *
 * @see BizAdCreationSuggestedInterestsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSuggestedInterestsQuery")
public final class BizAdCreationSuggestedInterestsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected suggested interests.
     */
    private final List<BusinessAdInterest> interests;

    /**
     * Constructs a response wrapping the projected suggested interests.
     *
     * <p>Reserved for the static parser.
     *
     * @param interests the projected suggested interests
     */
    private BizAdCreationSuggestedInterestsFacebookGraphQlResponse(List<BusinessAdInterest> interests) {
        this.interests = interests;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * suggested detailed-targeting interests onto {@link BusinessAdInterest} entries.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationSuggestedInterestsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        List<BusinessAdInterest> interests = List.of();
        var adAccount = data.getJSONObject("ad_account");
        if (adAccount != null) {
            var suggestions = adAccount.getJSONObject("detailed_targeting_suggestions");
            if (suggestions != null) {
                interests = interests(suggestions.getJSONArray("nodes"));
            }
        }
        return Optional.of(new BizAdCreationSuggestedInterestsFacebookGraphQlResponse(interests));
    }

    /**
     * Projects the {@code nodes} array onto a list of {@link BusinessAdInterest} entries.
     *
     * @param nodes the {@code nodes} array, or {@code null}
     * @return the projected interests, empty when {@code nodes} is {@code null}
     */
    private static List<BusinessAdInterest> interests(JSONArray nodes) {
        if (nodes == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAdInterest>(nodes.size());
        for (var i = 0; i < nodes.size(); i++) {
            var node = nodes.getJSONObject(i);
            if (node == null) {
                continue;
            }
            result.add(new BusinessAdInterestBuilder()
                    .id(node.getString("id"))
                    .name(node.getString("name"))
                    .path(node.getString("path"))
                    .targetType(node.getString("target_type"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the projected suggested interests.
     *
     * @return an unmodifiable view of the projected interests; never {@code null}, possibly empty
     */
    public List<BusinessAdInterest> interests() {
        return interests;
    }
}
