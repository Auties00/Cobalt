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
 * Parses the Facebook GraphQL response of the search-interests query built by
 * {@link BizAdCreationSearchInterestsFacebookGraphQlRequest} into a list of {@link BusinessAdInterest}.
 *
 * <p>Projects the matching detailed-targeting interests, reached through the
 * {@code entities_named.ads_targeting.detailed_targetings} connection, onto {@link BusinessAdInterest}
 * entries.
 *
 * @see BizAdCreationSearchInterestsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationSearchInterestsQuery")
public final class BizAdCreationSearchInterestsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected matching interests.
     */
    private final List<BusinessAdInterest> interests;

    /**
     * Constructs a response wrapping the projected matching interests.
     *
     * <p>Reserved for the static parser.
     *
     * @param interests the projected matching interests
     */
    private BizAdCreationSearchInterestsFacebookGraphQlResponse(List<BusinessAdInterest> interests) {
        this.interests = interests;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * matching detailed-targeting interests onto {@link BusinessAdInterest} entries.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationSearchInterestsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        List<BusinessAdInterest> interests = List.of();
        var entitiesNamed = data.getJSONObject("entities_named");
        if (entitiesNamed != null) {
            var adsTargeting = entitiesNamed.getJSONObject("ads_targeting");
            if (adsTargeting != null) {
                var detailedTargetings = adsTargeting.getJSONObject("detailed_targetings");
                if (detailedTargetings != null) {
                    interests = interests(detailedTargetings.getJSONArray("edges"));
                }
            }
        }
        return Optional.of(new BizAdCreationSearchInterestsFacebookGraphQlResponse(interests));
    }

    /**
     * Projects the {@code edges} array onto a list of {@link BusinessAdInterest} entries by reading
     * each edge's {@code stanza}.
     *
     * @param edges the {@code edges} array, or {@code null}
     * @return the projected interests, empty when {@code edges} is {@code null}
     */
    private static List<BusinessAdInterest> interests(JSONArray edges) {
        if (edges == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAdInterest>(edges.size());
        for (var i = 0; i < edges.size(); i++) {
            var edge = edges.getJSONObject(i);
            if (edge == null) {
                continue;
            }
            var node = edge.getJSONObject("node");
            if (node == null) {
                continue;
            }
            result.add(new BusinessAdInterestBuilder()
                    .id(node.getString("id"))
                    .name(node.getString("name"))
                    .path(node.getString("path"))
                    .rawName(node.getString("raw_name"))
                    .targetType(node.getString("target_type"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the projected matching interests.
     *
     * @return an unmodifiable view of the projected interests; never {@code null}, possibly empty
     */
    public List<BusinessAdInterest> interests() {
        return interests;
    }
}
