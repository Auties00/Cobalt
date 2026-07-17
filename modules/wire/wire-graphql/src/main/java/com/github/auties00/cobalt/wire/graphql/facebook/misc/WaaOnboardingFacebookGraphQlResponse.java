package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsAdAccount;
import com.github.auties00.cobalt.wire.linked.business.waa.WhatsAppAdsAdAccountBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the WhatsApp Ads onboarding mutation built by
 * {@link WaaOnboardingFacebookGraphQlRequest} into a {@link WhatsAppAdsAdAccount}.
 *
 * <p>Reads the linked root {@code create_or_onboard_wa_ad_account} and projects its
 * {@code ad_account_id} (which doubles as the success signal: the WhatsApp client treats a missing
 * id as an onboarding failure) and {@code status} onto the Cobalt domain model.
 *
 * @see WaaOnboardingFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebWAAOnboardingMutation")
public final class WaaOnboardingFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed ad account.
     */
    private final WhatsAppAdsAdAccount adAccount;

    /**
     * Constructs a response wrapping the parsed ad account.
     *
     * <p>Reserved for the static parser.
     *
     * @param adAccount the parsed ad account, or {@code null} when the Meta graph endpoint omitted the field
     */
    private WaaOnboardingFacebookGraphQlResponse(WhatsAppAdsAdAccount adAccount) {
        this.adAccount = adAccount;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code create_or_onboard_wa_ad_account} and projects it onto a
     * {@link WhatsAppAdsAdAccount}; the returned {@link Optional} is empty when {@code data} or the
     * ad-account object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the ad-account object is missing
     */
    public static Optional<WaaOnboardingFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("create_or_onboard_wa_ad_account");
        if (node == null) {
            return Optional.empty();
        }

        var adAccount = new WhatsAppAdsAdAccountBuilder()
                .accountId(node.getString("ad_account_id"))
                .status(node.getString("status"))
                .build();
        return Optional.of(new WaaOnboardingFacebookGraphQlResponse(adAccount));
    }

    /**
     * Returns the parsed ad account.
     *
     * @return the parsed {@link WhatsAppAdsAdAccount}, never {@code null}
     */
    public WhatsAppAdsAdAccount adAccount() {
        return adAccount;
    }
}
