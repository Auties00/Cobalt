package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the ad-account feature-flag query built by
 * {@link AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlRequest}.
 *
 * <p>Projects the conditional {@code lwi.expose_ad_account_for_qe_bool} scalar, which carries the
 * resolved boolean feature-flag value for the requested ad account. That branch is present only when
 * the request asked the server to fetch it; it is absent otherwise, in which case the flag is treated
 * as {@code false}.
 *
 * @see AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "useAdsLWIExposeAdAccountBoolUniverseHookQuery")
public final class AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the resolved feature-flag value, or {@code null} when the relay omitted it.
     */
    private final Boolean featureFlag;

    /**
     * Constructs a response wrapping the parsed feature-flag value.
     *
     * <p>Reserved for the static parser.
     *
     * @param featureFlag the resolved feature-flag value, or {@code null} when the relay omitted it
     */
    private AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlResponse(Boolean featureFlag) {
        this.featureFlag = featureFlag;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the conditional {@code lwi.expose_ad_account_for_qe_bool} scalar; the returned
     * {@link Optional} is empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var lwi = data.getJSONObject("lwi");
        var featureFlag = lwi == null ? null : lwi.getBoolean("expose_ad_account_for_qe_bool");
        return Optional.of(new AdsLwiExposeAdAccountBoolUniverseHookFacebookGraphQlResponse(featureFlag));
    }

    /**
     * Returns the resolved feature-flag value.
     *
     * <p>Returns {@code false} when the relay omitted the field, for example because the request did
     * not ask the server to fetch it.
     *
     * @return {@code true} when the flag resolved to {@code true}, {@code false} otherwise
     */
    public boolean featureFlag() {
        return featureFlag != null && featureFlag;
    }
}
