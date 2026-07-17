package com.github.auties00.cobalt.wire.linked.business.webgraphql;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Authenticated WhatsApp Web GraphQL session bundle used to drive the
 * {@code http_relay} GraphQL transport ({@code POST web.whatsapp.com/graphql/}).
 *
 * <p>Unlike the WhatsApp socket, which authenticates with the Noise session, the WhatsApp Web GraphQL transport is a
 * same-origin browser-style HTTP call authenticated by the WhatsApp Web session cookie plus the
 * {@code lsd} anti-CSRF token. WhatsApp Web obtains both by exchanging the canonical-registration
 * credentials recovered at pairing for an HttpOnly session cookie at the {@code /auth/token/} endpoint.
 *
 * <p>This bundle separates the two halves of that credential set. The {@linkplain #sessionCookie()
 * session cookie} and {@linkplain #lsdToken() lsd token} are the live values replayed on every relay
 * request; they are short-lived and re-derived on each connection. The
 * {@linkplain #canonicalAccessToken() canonical access token} and {@linkplain #fbid() Facebook account
 * id} are the durable seed: they survive a restart and let the client re-run the {@code /auth/token/}
 * exchange to mint a fresh cookie without re-pairing.
 *
 * <p>Any field may be {@code null} or zero before the WhatsApp Web GraphQL session has been established or while it is
 * being refreshed; the durable seed and the live values are populated at different points in the
 * connection lifecycle.
 */
@ProtobufMessage(name = "WhatsAppWebGraphQlSession")
public final class WhatsAppWebGraphQlSession {
    /**
     * The rendered {@code Cookie} header value replayed on every WhatsApp Web GraphQL request, or {@code null} before
     * the session cookie has been minted. The value is the whole captured cookie jar joined as
     * {@code name=value; name=value}; the HttpOnly session cookie is never read by name.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String sessionCookie;

    /**
     * The {@code lsd} anti-CSRF token sent as the {@code lsd} body field and the {@code X-FB-LSD}
     * header on every WhatsApp Web GraphQL request, or {@code null} before it has been scraped from the bootstrap
     * page.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String lsdToken;

    /**
     * The durable canonical access token recovered from the {@code <pair-success/>} canonical-nonce
     * blob, or {@code null} when the server deferred minting. Together with {@link #fbid()} it is the
     * seed that lets the WhatsApp Web GraphQL session be re-bootstrapped after a restart without re-pairing.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String canonicalAccessToken;

    /**
     * The Facebook account id ({@code user_id}) the canonical credentials authenticate, or {@code 0}
     * before pairing has seeded it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    long fbid;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param sessionCookie        the rendered {@code Cookie} header value, or {@code null}
     * @param lsdToken             the {@code lsd} anti-CSRF token, or {@code null}
     * @param canonicalAccessToken the durable canonical access token, or {@code null}
     * @param fbid                 the Facebook account id, or {@code 0}
     */
    WhatsAppWebGraphQlSession(String sessionCookie, String lsdToken, String canonicalAccessToken, long fbid) {
        this.sessionCookie = sessionCookie;
        this.lsdToken = lsdToken;
        this.canonicalAccessToken = canonicalAccessToken;
        this.fbid = fbid;
    }

    /**
     * Returns the rendered {@code Cookie} header value replayed on every WhatsApp Web GraphQL request.
     *
     * @return the cookie header, or {@code null} before the session cookie has been minted
     */
    public String sessionCookie() {
        return sessionCookie;
    }

    /**
     * Returns the {@code lsd} anti-CSRF token.
     *
     * @return the {@code lsd} token, or {@code null} before it has been scraped
     */
    public String lsdToken() {
        return lsdToken;
    }

    /**
     * Returns the durable canonical access token that seeds relay re-bootstrap.
     *
     * @return the canonical access token, or {@code null} when the server deferred minting
     */
    public String canonicalAccessToken() {
        return canonicalAccessToken;
    }

    /**
     * Returns the Facebook account id the canonical credentials authenticate.
     *
     * @return the Facebook account id, or {@code 0} before pairing has seeded it
     */
    public long fbid() {
        return fbid;
    }
}
