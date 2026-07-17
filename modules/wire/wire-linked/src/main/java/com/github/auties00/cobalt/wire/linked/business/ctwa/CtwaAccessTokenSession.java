package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Authenticated Click-to-WhatsApp (CTWA) Ads-Manager session bundle minted
 * by the relay in response to a successful email-recovery code exchange.
 *
 * <p>When a WhatsApp Business user wants to manage their Click-to-WhatsApp
 * advertising campaigns from the Facebook Ads Manager, the relay drives an
 * email-recovery handshake: the user receives a verification code by email,
 * types it into the WhatsApp client, and the client trades it for a
 * Facebook Graph API bearer token plus a JSON blob of session cookies that
 * the embedded Ads-Manager web view can install. The bundle also carries
 * the Facebook business-person identifier the token is scoped to, so the
 * client knows which advertiser identity it is acting on behalf of.
 *
 * <p>The optional {@linkplain #tokenStrength() token strength} marker
 * tells the client whether the bundle was minted from a fresh recovery
 * code (strong) or derived from a longer-lived session (weak); when the
 * relay omits the marker it is up to the caller to assume the conservative
 * default.
 */
@ProtobufMessage(name = "CtwaAccessTokenSession")
public final class CtwaAccessTokenSession {
    /**
     * The Facebook Graph API bearer token. The web view installs this
     * value as the {@code access_token} query parameter or, for native
     * Graph API calls, as the {@code Authorization: Bearer ...} header.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String accessToken;

    /**
     * JSON-encoded blob of session cookies for the Facebook Ads Manager
     * web UI. The client deserialises the blob and replays each
     * {@code (name, value, domain, path)} tuple into the embedded web
     * view's cookie jar before navigating to the Ads Manager URL.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String sessionCookies;

    /**
     * The Facebook business-person identifier the token is scoped to.
     * Identifies the advertiser identity that owns the campaigns this
     * session is allowed to mutate.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String businessPersonId;

    /**
     * Optional strength classifier supplied by the relay. When present,
     * indicates whether the token was minted from a fresh recovery code
     * ({@link CtwaAdTokenStrength#STRONG}) or derived from a longer-lived
     * session ({@link CtwaAdTokenStrength#WEAK}).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    CtwaAdTokenStrength tokenStrength;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param accessToken      the Facebook Graph API bearer token
     * @param sessionCookies   the JSON-encoded session-cookies blob
     * @param businessPersonId the Facebook business-person identifier
     * @param tokenStrength    the optional token-strength marker; may be
     *                         {@code null}
     */
    CtwaAccessTokenSession(String accessToken, String sessionCookies,
                           String businessPersonId, CtwaAdTokenStrength tokenStrength) {
        this.accessToken = accessToken;
        this.sessionCookies = sessionCookies;
        this.businessPersonId = businessPersonId;
        this.tokenStrength = tokenStrength;
    }

    /**
     * Returns the Facebook Graph API bearer token.
     *
     * @return the bearer token; never {@code null} for a session minted
     *         by a successful recovery code exchange
     */
    public String accessToken() {
        return accessToken;
    }

    /**
     * Returns the JSON-encoded session-cookies blob.
     *
     * @return the cookies blob; never {@code null}
     */
    public String sessionCookies() {
        return sessionCookies;
    }

    /**
     * Returns the Facebook business-person identifier the token is scoped
     * to.
     *
     * @return the identifier; never {@code null}
     */
    public String businessPersonId() {
        return businessPersonId;
    }

    /**
     * Returns the optional token-strength marker.
     *
     * @return an {@link Optional} carrying the marker, or empty when the
     *         relay omitted it
     */
    public Optional<CtwaAdTokenStrength> tokenStrength() {
        return Optional.ofNullable(tokenStrength);
    }
}
