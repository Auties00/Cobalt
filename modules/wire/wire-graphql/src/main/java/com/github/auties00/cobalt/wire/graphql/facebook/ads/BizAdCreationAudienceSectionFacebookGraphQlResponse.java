package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdAudienceSection;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdAudienceSectionBuilder;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdSavedAudience;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdSavedAudienceBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the audience-section query built by
 * {@link BizAdCreationAudienceSectionFacebookGraphQlRequest} into a {@link BusinessAdAudienceSection}.
 *
 * <p>Projects the suggested ready-made audiences and the template targeting spec from the {@code lwi}
 * boosted-component root, and the merchant's previously saved audiences from the {@code ad_account}
 * root, onto a single {@link BusinessAdAudienceSection}. Both the suggested and the saved audiences
 * are projected onto {@link BusinessAdSavedAudience} entries.
 *
 * @see BizAdCreationAudienceSectionFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationAudienceSectionQuery")
public final class BizAdCreationAudienceSectionFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected audience section, or {@code null} when the relay omitted both roots.
     */
    private final BusinessAdAudienceSection audienceSection;

    /**
     * Constructs a response wrapping the projected audience section.
     *
     * <p>Reserved for the static parser.
     *
     * @param audienceSection the projected audience section, or {@code null} when the relay omitted
     *                        both roots
     */
    private BizAdCreationAudienceSectionFacebookGraphQlResponse(BusinessAdAudienceSection audienceSection) {
        this.audienceSection = audienceSection;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * suggested audiences, template targeting spec, and saved audiences onto a
     * {@link BusinessAdAudienceSection}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationAudienceSectionFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        List<BusinessAdSavedAudience> suggested = List.of();
        String templateSpec = null;
        var lwi = data.getJSONObject("lwi");
        if (lwi != null) {
            var boostedComponent = lwi.getJSONObject("boosted_component");
            if (boostedComponent != null) {
                var options = boostedComponent.getJSONObject("options");
                if (options != null) {
                    suggested = suggestedAudiences(options.getJSONArray("audiences_v2"));
                    var templateTargetSpec = options.getJSONObject("template_target_spec");
                    if (templateTargetSpec != null) {
                        templateSpec = templateTargetSpec.getString("target_spec_string_without_placements");
                    }
                }
            }
        }

        List<BusinessAdSavedAudience> saved = List.of();
        var adAccount = data.getJSONObject("adAccount");
        if (adAccount != null) {
            saved = savedAudiences(adAccount.getJSONObject("savedAudiences"));
        }

        var audienceSection = new BusinessAdAudienceSectionBuilder()
                .suggestedAudiences(suggested)
                .templateTargetingSpec(templateSpec)
                .savedAudiences(saved)
                .build();
        return Optional.of(new BizAdCreationAudienceSectionFacebookGraphQlResponse(audienceSection));
    }

    /**
     * Projects the {@code audiences_v2} array onto a list of {@link BusinessAdSavedAudience} entries.
     *
     * @param arr the {@code audiences_v2} array, or {@code null}
     * @return the projected suggested audiences, empty when {@code arr} is {@code null}
     */
    private static List<BusinessAdSavedAudience> suggestedAudiences(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAdSavedAudience>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var node = arr.getJSONObject(i);
            if (node != null) {
                result.add(savedAudience(node));
            }
        }
        return result;
    }

    /**
     * Projects the {@code savedAudiences} connection onto a list of {@link BusinessAdSavedAudience}
     * entries by reading each edge's {@code stanza}.
     *
     * @param connection the {@code savedAudiences} connection, or {@code null}
     * @return the projected saved audiences, empty when {@code connection} is {@code null}
     */
    private static List<BusinessAdSavedAudience> savedAudiences(JSONObject connection) {
        if (connection == null) {
            return List.of();
        }

        var edges = connection.getJSONArray("edges");
        if (edges == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAdSavedAudience>(edges.size());
        for (var i = 0; i < edges.size(); i++) {
            var edge = edges.getJSONObject(i);
            if (edge == null) {
                continue;
            }
            var node = edge.getJSONObject("node");
            if (node != null) {
                result.add(savedAudience(node));
            }
        }
        return result;
    }

    /**
     * Projects a single audience object onto a {@link BusinessAdSavedAudience}.
     *
     * @param node the audience object
     * @return the projected audience
     */
    private static BusinessAdSavedAudience savedAudience(JSONObject node) {
        return new BusinessAdSavedAudienceBuilder()
                .name(node.getString("name"))
                .id(node.getString("id"))
                .targetingSpec(node.getString("target_spec_string_without_placements"))
                .subjectToEuComplianceRules(Boolean.TRUE.equals(node.getBoolean("subject_to_dsa")))
                .build();
    }

    /**
     * Returns the projected audience section.
     *
     * @return the projected {@link BusinessAdAudienceSection}, or empty when the relay omitted both
     *         roots
     */
    public Optional<BusinessAdAudienceSection> audienceSection() {
        return Optional.ofNullable(audienceSection);
    }
}
