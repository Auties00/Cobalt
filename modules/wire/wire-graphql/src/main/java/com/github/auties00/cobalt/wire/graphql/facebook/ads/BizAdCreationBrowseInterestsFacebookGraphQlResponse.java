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
 * Parses the Facebook GraphQL response of the browse-interests query built by
 * {@link BizAdCreationBrowseInterestsFacebookGraphQlRequest} into a list of {@link BusinessAdInterest}.
 *
 * <p>Projects the plural {@code detailed_targeting_browse} root, the immediate children of the
 * browsed path in the targeting taxonomy, onto {@link BusinessAdInterest} entries.
 *
 * @see BizAdCreationBrowseInterestsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationBrowseInterestsQuery")
public final class BizAdCreationBrowseInterestsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected interests.
     */
    private final List<BusinessAdInterest> interests;

    /**
     * Constructs a response wrapping the projected interests.
     *
     * <p>Reserved for the static parser.
     *
     * @param interests the projected interests
     */
    private BizAdCreationBrowseInterestsFacebookGraphQlResponse(List<BusinessAdInterest> interests) {
        this.interests = interests;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the plural
     * {@code detailed_targeting_browse} root onto {@link BusinessAdInterest} entries.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationBrowseInterestsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var interests = interests(data.getJSONArray("detailed_targeting_browse"));
        return Optional.of(new BizAdCreationBrowseInterestsFacebookGraphQlResponse(interests));
    }

    /**
     * Projects the {@code detailed_targeting_browse} array onto a list of {@link BusinessAdInterest}
     * entries.
     *
     * @param arr the {@code detailed_targeting_browse} array, or {@code null}
     * @return the projected interests, empty when {@code arr} is {@code null}
     */
    private static List<BusinessAdInterest> interests(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAdInterest>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var node = arr.getJSONObject(i);
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
     * Returns the projected interests.
     *
     * @return an unmodifiable view of the projected interests; never {@code null}, possibly empty
     */
    public List<BusinessAdInterest> interests() {
        return interests;
    }
}
