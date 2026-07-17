package com.github.auties00.cobalt.wire.linked.business.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * The Facebook credentials minted by redeeming an OpenID Connect
 * authorization code.
 *
 * <p>The Click-to-WhatsApp advertising flow signs the user in to Facebook via
 * the OpenID Connect provider; the resulting authorization code is redeemed
 * server-side into the Facebook access token the WhatsApp client then uses for
 * downstream advertising-platform calls. This model is that redeemed
 * credential pair: the {@linkplain #accessToken() Facebook access token} and
 * the {@linkplain #facebookUserId() Facebook user id} it belongs to.
 */
@ProtobufMessage(name = "FacebookOidcAccessToken")
public final class FacebookOidcAccessToken {
    /**
     * Minted Facebook access token, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String accessToken;

    /**
     * Facebook user id the access token belongs to, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String facebookUserId;

    /**
     * Constructs a new {@code FacebookOidcAccessToken}. Any reference argument
     * may be {@code null} when the server omitted the corresponding field.
     *
     * @param accessToken    the minted access token, or {@code null}
     * @param facebookUserId the Facebook user id, or {@code null}
     */
    FacebookOidcAccessToken(String accessToken, String facebookUserId) {
        this.accessToken = accessToken;
        this.facebookUserId = facebookUserId;
    }

    /**
     * Returns the minted Facebook access token.
     *
     * @return the access token, or empty when the server omitted it
     */
    public Optional<String> accessToken() {
        return Optional.ofNullable(accessToken);
    }

    /**
     * Returns the Facebook user id the access token belongs to.
     *
     * @return the Facebook user id, or empty when the server omitted it
     */
    public Optional<String> facebookUserId() {
        return Optional.ofNullable(facebookUserId);
    }
}
