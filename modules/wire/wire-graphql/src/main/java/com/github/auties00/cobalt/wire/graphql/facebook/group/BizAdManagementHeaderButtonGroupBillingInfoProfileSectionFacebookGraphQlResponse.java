package com.github.auties00.cobalt.wire.graphql.facebook.group;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdBillingActor;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdBillingActorBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the ad-management billing-info profile-section query built by
 * {@link BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlRequest} into a
 * {@link BusinessAdBillingActor}.
 *
 * <p>Reads the linked {@code me} actor root and projects its {@code __typename} discriminator,
 * its nested {@code profile_picture.downloadable_uri}, and its scalar {@code id} onto a
 * {@link BusinessAdBillingActor}.
 *
 * @see BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAdManagementHeaderButtonGroupBillingInfoProfileSectionQuery")
public final class BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed billing actor.
     */
    private final BusinessAdBillingActor actor;

    /**
     * Constructs a response wrapping the parsed billing actor.
     *
     * <p>Reserved for the static parser.
     *
     * @param actor the parsed billing actor
     */
    private BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlResponse(BusinessAdBillingActor actor) {
        this.actor = actor;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code me} actor root and projects its {@code __typename} discriminator,
     * its nested {@code profile_picture.downloadable_uri}, and its scalar {@code id} onto a
     * {@link BusinessAdBillingActor}; the returned {@link Optional} is empty when {@code data} or
     * the {@code me} root is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the {@code me} root is missing
     */
    public static Optional<BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("me");
        if (root == null) {
            return Optional.empty();
        }

        var picture = root.getJSONObject("profile_picture");
        var actor = new BusinessAdBillingActorBuilder()
                .kind(FacebookGraphQlOperation.Response.getTypename(root).orElse(null))
                .profilePictureUrl(picture == null ? null : picture.getString("downloadable_uri"))
                .id(root.getString("id"))
                .build();
        return Optional.of(new BizAdManagementHeaderButtonGroupBillingInfoProfileSectionFacebookGraphQlResponse(actor));
    }

    /**
     * Returns the parsed billing actor.
     *
     * @return the parsed {@link BusinessAdBillingActor}, never {@code null}
     */
    public BusinessAdBillingActor actor() {
        return actor;
    }
}
