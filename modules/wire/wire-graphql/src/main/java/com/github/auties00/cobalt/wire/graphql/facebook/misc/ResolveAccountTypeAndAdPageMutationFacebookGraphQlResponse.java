package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsAccountTypeReset;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsAccountTypeResetBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the resolve-account-type-and-ad-page mutation built by
 * {@link ResolveAccountTypeAndAdPageMutationFacebookGraphQlRequest} into a
 * {@link WhatsAppAdsAccountTypeReset}.
 *
 * <p>Reads the single scalar root {@code xfb_wa_biz_clear_oidc_preference} and projects it onto the
 * Cobalt domain model: the server's acknowledgement that the cached WhatsApp Ads account-type
 * preference was cleared so the next sign-in re-prompts for the account type and Facebook page.
 *
 * @see ResolveAccountTypeAndAdPageMutationFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebResolveAccountTypeAndAdPageMutation")
public final class ResolveAccountTypeAndAdPageMutationFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed account-type reset acknowledgement.
     */
    private final WhatsAppAdsAccountTypeReset reset;

    /**
     * Constructs a response wrapping the parsed acknowledgement.
     *
     * <p>Reserved for the static parser.
     *
     * @param reset the parsed acknowledgement, or {@code null} when the Meta graph endpoint omitted the field
     */
    private ResolveAccountTypeAndAdPageMutationFacebookGraphQlResponse(WhatsAppAdsAccountTypeReset reset) {
        this.reset = reset;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the scalar root {@code xfb_wa_biz_clear_oidc_preference} and projects it onto a
     * {@link WhatsAppAdsAccountTypeReset}; the returned {@link Optional} is empty when {@code data}
     * is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<ResolveAccountTypeAndAdPageMutationFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var reset = new WhatsAppAdsAccountTypeResetBuilder()
                .acknowledgement(data.getString("xfb_wa_biz_clear_oidc_preference"))
                .build();
        return Optional.of(new ResolveAccountTypeAndAdPageMutationFacebookGraphQlResponse(reset));
    }

    /**
     * Returns the parsed account-type reset acknowledgement.
     *
     * @return the parsed {@link WhatsAppAdsAccountTypeReset}, never {@code null}
     */
    public WhatsAppAdsAccountTypeReset reset() {
        return reset;
    }
}
