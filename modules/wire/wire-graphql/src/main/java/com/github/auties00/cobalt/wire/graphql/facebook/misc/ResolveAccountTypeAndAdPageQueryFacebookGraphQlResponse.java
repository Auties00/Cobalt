package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsPageEligibility;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsPageEligibilityBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the resolve-account-type-and-ad-page query built by
 * {@link ResolveAccountTypeAndAdPageQueryFacebookGraphQlRequest} into a {@link WhatsAppAdsPageEligibility}.
 *
 * <p>Reads the linked root {@code page} and projects its {@code id} and the
 * {@code can_viewer_do_actions} verdict (the server evaluates against the {@code CREATE_ADS}
 * action) onto the Cobalt domain model: the resolved Facebook page id paired with the per-page
 * create-ads verdict.
 *
 * @see ResolveAccountTypeAndAdPageQueryFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebResolveAccountTypeAndAdPageQuery")
public final class ResolveAccountTypeAndAdPageQueryFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed page eligibility.
     */
    private final WhatsAppAdsPageEligibility eligibility;

    /**
     * Constructs a response wrapping the parsed page eligibility.
     *
     * <p>Reserved for the static parser.
     *
     * @param eligibility the parsed page eligibility, or {@code null} when the Meta graph endpoint
     *                    omitted the field
     */
    private ResolveAccountTypeAndAdPageQueryFacebookGraphQlResponse(WhatsAppAdsPageEligibility eligibility) {
        this.eligibility = eligibility;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code page} and projects it onto a {@link WhatsAppAdsPageEligibility};
     * the returned {@link Optional} is empty when {@code data} or the page object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the page object is missing
     */
    public static Optional<ResolveAccountTypeAndAdPageQueryFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var page = data.getJSONObject("page");
        if (page == null) {
            return Optional.empty();
        }

        var canCreateAds = page.getBoolean("can_viewer_do_actions");
        var eligibility = new WhatsAppAdsPageEligibilityBuilder()
                .pageId(page.getString("id"))
                .canCreateAds(canCreateAds != null && canCreateAds)
                .build();
        return Optional.of(new ResolveAccountTypeAndAdPageQueryFacebookGraphQlResponse(eligibility));
    }

    /**
     * Returns the parsed page eligibility.
     *
     * @return the parsed {@link WhatsAppAdsPageEligibility}, never {@code null}
     */
    public WhatsAppAdsPageEligibility eligibility() {
        return eligibility;
    }
}
