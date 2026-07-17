package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdSavedAudience;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdSavedAudienceBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the edit-saved-audience mutation built by
 * {@link BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlRequest} into a {@link BusinessAdSavedAudience}.
 *
 * <p>Projects the {@code saved_audience_edit} root, the audience the server echoes back after the
 * edit, onto the {@link BusinessAdSavedAudience} model: its display name, identifier, stored targeting
 * spec, and EU advertising-transparency flag.
 *
 * @see BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdCreationAudienceUtils_EditAudienceMutation")
public final class BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected edited audience, or {@code null} when the relay omitted the field.
     */
    private final BusinessAdSavedAudience savedAudience;

    /**
     * Constructs a response wrapping the projected edited audience.
     *
     * <p>Reserved for the static parser.
     *
     * @param savedAudience the projected edited audience, or {@code null} when the relay omitted the
     *                      field
     */
    private BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlResponse(BusinessAdSavedAudience savedAudience) {
        this.savedAudience = savedAudience;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * {@code saved_audience_edit} root onto a {@link BusinessAdSavedAudience}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var savedAudience = savedAudience(data.getJSONObject("saved_audience_edit"));
        return Optional.of(new BizAdCreationAudienceUtils_EditAudienceFacebookGraphQlResponse(savedAudience));
    }

    /**
     * Projects the {@code saved_audience_edit} object onto a {@link BusinessAdSavedAudience}.
     *
     * @param node the {@code saved_audience_edit} object, or {@code null}
     * @return the projected audience, or {@code null} when {@code stanza} is {@code null}
     */
    private static BusinessAdSavedAudience savedAudience(JSONObject node) {
        if (node == null) {
            return null;
        }

        return new BusinessAdSavedAudienceBuilder()
                .name(node.getString("name"))
                .id(node.getString("id"))
                .targetingSpec(node.getString("target_spec_string_without_placements"))
                .subjectToEuComplianceRules(Boolean.TRUE.equals(node.getBoolean("subject_to_dsa")))
                .build();
    }

    /**
     * Returns the projected edited audience.
     *
     * @return the projected {@link BusinessAdSavedAudience}, or empty when the relay omitted the field
     */
    public Optional<BusinessAdSavedAudience> savedAudience() {
        return Optional.ofNullable(savedAudience);
    }
}
