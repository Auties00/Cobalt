package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.auth.BusinessPlatformAuthToken;
import com.github.auties00.cobalt.wire.linked.business.auth.BusinessPlatformAuthTokenBuilder;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the Business Platform token-exchange mutation built by
 * {@link BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlRequest} into a {@link BusinessPlatformAuthToken}.
 *
 * <p>Reads the linked root {@code xwa_bp_access_token_and_session_cookies} and projects its scalars
 * (exchange status, minted access token and its type, serialized session cookies, Business Platform
 * account id, and email attribute) onto the Cobalt domain model.
 *
 * @see BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBPAccessTokenAndSessionCookiesMutation")
public final class BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed Business Platform credential bundle.
     */
    private final BusinessPlatformAuthToken token;

    /**
     * Constructs a response wrapping the parsed credential bundle.
     *
     * <p>Reserved for the static parser.
     *
     * @param token the parsed credential bundle, or {@code null} when the relay omitted the field
     */
    private BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlResponse(BusinessPlatformAuthToken token) {
        this.token = token;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_bp_access_token_and_session_cookies} and projects it onto a
     * {@link BusinessPlatformAuthToken}; the returned {@link Optional} is empty when {@code data} or
     * the credential object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the credential object is missing
     */
    public static Optional<BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xwa_bp_access_token_and_session_cookies");
        if (node == null) {
            return Optional.empty();
        }

        var token = new BusinessPlatformAuthTokenBuilder()
                .status(node.getString("status"))
                .accessToken(node.getString("access_token"))
                .sessionCookies(node.getString("session_cookies"))
                .businessPlatformId(node.getString("bp_id"))
                .accessTokenType(node.getString("access_token_type"))
                .emailAttribute(node.getString("email_attr"))
                .build();
        return Optional.of(new BpAccessTokenAndSessionCookiesWhatsAppWebGraphQlResponse(token));
    }

    /**
     * Returns the parsed Business Platform credential bundle.
     *
     * @return the parsed {@link BusinessPlatformAuthToken}, never {@code null}
     */
    public BusinessPlatformAuthToken token() {
        return token;
    }
}
