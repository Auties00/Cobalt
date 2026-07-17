package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the fetch-OIDC-state query built by
 * {@link FetchOidcStateFacebookGraphQlRequest}.
 *
 * <p>Exposes the single scalar field {@code xfb_wa_biz_get_oidc_state}, the OpenID Connect state
 * blob the Meta graph endpoint returns for the authenticated WhatsApp Business account.
 *
 * @see FetchOidcStateFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebFetchOIDCStateQuery")
public final class FetchOidcStateFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the OIDC state blob returned under {@code xfb_wa_biz_get_oidc_state}.
     */
    private final String oidcState;

    /**
     * Constructs a response wrapping the parsed OIDC state blob.
     *
     * <p>Reserved for the static parser.
     *
     * @param oidcState the OIDC state blob, or {@code null} when the Meta graph endpoint omitted the field
     */
    private FetchOidcStateFacebookGraphQlResponse(String oidcState) {
        this.oidcState = oidcState;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the scalar root {@code xfb_wa_biz_get_oidc_state}; the returned {@link Optional} is
     * empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<FetchOidcStateFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var oidcState = data.getString("xfb_wa_biz_get_oidc_state");
        return Optional.of(new FetchOidcStateFacebookGraphQlResponse(oidcState));
    }

    /**
     * Returns the OpenID Connect state blob.
     *
     * @return the OIDC state blob, or empty when the Meta graph endpoint omitted the field
     */
    public Optional<String> oidcState() {
        return Optional.ofNullable(oidcState);
    }
}
