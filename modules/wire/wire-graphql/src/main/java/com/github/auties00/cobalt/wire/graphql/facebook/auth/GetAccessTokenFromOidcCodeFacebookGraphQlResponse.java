package com.github.auties00.cobalt.wire.graphql.facebook.auth;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.auth.FacebookOidcAccessToken;
import com.github.auties00.cobalt.wire.linked.business.auth.FacebookOidcAccessTokenBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the OpenID-Connect token-exchange mutation built by
 * {@link GetAccessTokenFromOidcCodeFacebookGraphQlRequest} into a {@link FacebookOidcAccessToken}.
 *
 * <p>Reads the linked root {@code xfb_wa_biz_get_token_from_oidc_code} and projects its scalars (the
 * minted Facebook access token and the Facebook user id it belongs to) onto the Cobalt domain model.
 *
 * @see GetAccessTokenFromOidcCodeFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebGetAccessTokenFromOIDCCodeMutation")
public final class GetAccessTokenFromOidcCodeFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed Facebook credential pair.
     */
    private final FacebookOidcAccessToken token;

    /**
     * Constructs a response wrapping the parsed credential pair.
     *
     * <p>Reserved for the static parser.
     *
     * @param token the parsed credential pair, or {@code null} when the Meta graph endpoint omitted the field
     */
    private GetAccessTokenFromOidcCodeFacebookGraphQlResponse(FacebookOidcAccessToken token) {
        this.token = token;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_wa_biz_get_token_from_oidc_code} and projects it onto a
     * {@link FacebookOidcAccessToken}; the returned {@link Optional} is empty when {@code data} or
     * the credential object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the credential object is missing
     */
    public static Optional<GetAccessTokenFromOidcCodeFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_wa_biz_get_token_from_oidc_code");
        if (node == null) {
            return Optional.empty();
        }

        var token = new FacebookOidcAccessTokenBuilder()
                .accessToken(node.getString("access_token"))
                .facebookUserId(node.getString("fb_user_id"))
                .build();
        return Optional.of(new GetAccessTokenFromOidcCodeFacebookGraphQlResponse(token));
    }

    /**
     * Returns the parsed Facebook credential pair.
     *
     * @return the parsed {@link FacebookOidcAccessToken}, never {@code null}
     */
    public FacebookOidcAccessToken token() {
        return token;
    }
}
