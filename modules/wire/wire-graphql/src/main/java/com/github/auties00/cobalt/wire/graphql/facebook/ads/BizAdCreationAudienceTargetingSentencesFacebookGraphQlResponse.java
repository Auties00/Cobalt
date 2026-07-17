package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdTargetingDescription;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdTargetingDescriptionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the targeting-sentences query built by
 * {@link BizAdCreationAudienceTargetingSentencesFacebookGraphQlRequest} into a list of
 * {@link BusinessAdTargetingDescription}.
 *
 * <p>Projects the {@code targeting_sentences} list under the {@code lwi} root onto
 * {@link BusinessAdTargetingDescription} entries, each pairing a targeting facet with its rendered
 * values and an opaque interaction token.
 *
 * @see BizAdCreationAudienceTargetingSentencesFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationAudienceTargetingSentencesQuery")
public final class BizAdCreationAudienceTargetingSentencesFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected targeting descriptions.
     */
    private final List<BusinessAdTargetingDescription> descriptions;

    /**
     * Constructs a response wrapping the projected targeting descriptions.
     *
     * <p>Reserved for the static parser.
     *
     * @param descriptions the projected targeting descriptions
     */
    private BizAdCreationAudienceTargetingSentencesFacebookGraphQlResponse(List<BusinessAdTargetingDescription> descriptions) {
        this.descriptions = descriptions;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code targeting_sentences} list onto {@link BusinessAdTargetingDescription} entries.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationAudienceTargetingSentencesFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        List<BusinessAdTargetingDescription> descriptions = List.of();
        var lwi = data.getJSONObject("lwi");
        if (lwi != null) {
            descriptions = descriptions(lwi.getJSONArray("targeting_sentences"));
        }
        return Optional.of(new BizAdCreationAudienceTargetingSentencesFacebookGraphQlResponse(descriptions));
    }

    /**
     * Projects the {@code targeting_sentences} array onto a list of
     * {@link BusinessAdTargetingDescription} entries.
     *
     * @param arr the {@code targeting_sentences} array, or {@code null}
     * @return the projected descriptions, empty when {@code arr} is {@code null}
     */
    private static List<BusinessAdTargetingDescription> descriptions(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAdTargetingDescription>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var node = arr.getJSONObject(i);
            if (node == null) {
                continue;
            }
            result.add(new BusinessAdTargetingDescriptionBuilder()
                    .category(node.getString("category_string"))
                    .values(node.getString("values"))
                    .meta(node.getString("meta"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the projected targeting descriptions.
     *
     * @return an unmodifiable view of the projected descriptions; never {@code null}, possibly empty
     */
    public List<BusinessAdTargetingDescription> descriptions() {
        return descriptions;
    }
}
