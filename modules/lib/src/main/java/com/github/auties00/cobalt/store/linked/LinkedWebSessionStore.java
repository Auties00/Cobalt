package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaAccessTokenSession;
import com.github.auties00.cobalt.wire.linked.business.webgraphql.WhatsAppWebGraphQlSession;

import java.util.Optional;

/**
 * The web-GraphQL credential state of a WhatsApp client session.
 *
 * <p>This sub-store owns the credentials the two HTTP GraphQL transports need, neither of which is
 * carried over the WhatsApp socket: the {@code http_relay} transport ({@code POST web.whatsapp.com/graphql/},
 * authenticated by a session cookie plus an {@code lsd} token) and the {@code http_comet} transport
 * ({@code POST graph.facebook.com/graphql}, authenticated by a Facebook access token). Both credential
 * sets are persisted so they survive a restart: the relay seed is re-exchanged for a fresh cookie
 * without re-pairing, and the Facebook GraphQL token is reused until a silent refresh renews it.
 *
 * <p>The credentials are acquired and refreshed automatically after a successful connection on a
 * WhatsApp Web client; embedders do not normally read or write this sub-store directly.
 */
@WhatsAppWebModule(moduleName = "WAWebRelayClient")
@WhatsAppWebModule(moduleName = "CometRelay")
public interface LinkedWebSessionStore {
    /**
     * Returns the persisted Click-to-WhatsApp Ads-Manager session used to authenticate the
     * {@code http_comet} transport.
     *
     * @return an {@link Optional} carrying the Facebook GraphQL session, or empty before one has been minted
     */
    Optional<CtwaAccessTokenSession> facebookGraphQlSession();

    /**
     * Replaces the persisted Facebook GraphQL session.
     *
     * @param facebookGraphQlSession the Facebook GraphQL session to store, or {@code null} to clear it
     * @return this sub-store, for chaining
     */
    LinkedWebSessionStore setFacebookGraphQlSession(CtwaAccessTokenSession facebookGraphQlSession);

    /**
     * Returns the persisted WhatsApp Web GraphQL session used to authenticate the {@code http_relay} transport.
     *
     * @return an {@link Optional} carrying the WhatsApp Web GraphQL session, or empty before one has been bootstrapped
     */
    Optional<WhatsAppWebGraphQlSession> whatsAppWebGraphQlSession();

    /**
     * Replaces the persisted WhatsApp Web GraphQL session.
     *
     * @param whatsAppWebGraphQlSession the WhatsApp Web GraphQL session to store, or {@code null} to clear it
     * @return this sub-store, for chaining
     */
    LinkedWebSessionStore setWhatsAppWebGraphQlSession(WhatsAppWebGraphQlSession whatsAppWebGraphQlSession);
}
