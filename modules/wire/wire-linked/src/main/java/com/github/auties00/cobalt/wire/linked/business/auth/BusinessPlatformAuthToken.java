package com.github.auties00.cobalt.wire.linked.business.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * The credentials minted by redeeming a Business Platform authorization code.
 *
 * <p>A business operator that has just completed the Business Platform sign-in
 * flow receives a short-lived authorization code; the WhatsApp client redeems
 * that code into a durable access token plus the browser session cookies the
 * Business Platform surfaces expect on subsequent requests. This model is the
 * full redeemed credential bundle: the {@linkplain #status() exchange status},
 * the {@linkplain #accessToken() access token} and its
 * {@linkplain #accessTokenType() type}, the serialized
 * {@linkplain #sessionCookies() session cookies}, the
 * {@linkplain #businessPlatformId() Business Platform account id}, and the
 * {@linkplain #emailAttribute() email attribute} associated with the
 * authorization code.
 */
@ProtobufMessage(name = "BusinessPlatformAuthToken")
public final class BusinessPlatformAuthToken {
    /**
     * Server-reported exchange status, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String status;

    /**
     * Minted access token, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String accessToken;

    /**
     * Serialized Business Platform session cookies the client must echo on
     * subsequent requests, or {@code null} when the server omitted them.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String sessionCookies;

    /**
     * Server-issued Business Platform account identifier, or {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String businessPlatformId;

    /**
     * Access token type discriminator, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String accessTokenType;

    /**
     * Email attribute associated with the redeemed authorization code, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String emailAttribute;

    /**
     * Constructs a new {@code BusinessPlatformAuthToken}. Any reference
     * argument may be {@code null} when the server omitted the corresponding
     * field.
     *
     * @param status             the exchange status, or {@code null}
     * @param accessToken        the minted access token, or {@code null}
     * @param sessionCookies     the serialized session cookies, or
     *                           {@code null}
     * @param businessPlatformId the Business Platform account identifier, or
     *                           {@code null}
     * @param accessTokenType    the access token type discriminator, or
     *                           {@code null}
     * @param emailAttribute     the associated email attribute, or
     *                           {@code null}
     */
    BusinessPlatformAuthToken(String status, String accessToken, String sessionCookies,
                              String businessPlatformId, String accessTokenType, String emailAttribute) {
        this.status = status;
        this.accessToken = accessToken;
        this.sessionCookies = sessionCookies;
        this.businessPlatformId = businessPlatformId;
        this.accessTokenType = accessTokenType;
        this.emailAttribute = emailAttribute;
    }

    /**
     * Returns the server-reported exchange status.
     *
     * @return the exchange status, or empty when the server omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the minted access token.
     *
     * @return the access token, or empty when the server omitted it
     */
    public Optional<String> accessToken() {
        return Optional.ofNullable(accessToken);
    }

    /**
     * Returns the serialized session cookies.
     *
     * @return the session cookies, or empty when the server omitted them
     */
    public Optional<String> sessionCookies() {
        return Optional.ofNullable(sessionCookies);
    }

    /**
     * Returns the server-issued Business Platform account identifier.
     *
     * @return the account identifier, or empty when the server omitted it
     */
    public Optional<String> businessPlatformId() {
        return Optional.ofNullable(businessPlatformId);
    }

    /**
     * Returns the access token type discriminator.
     *
     * @return the access token type, or empty when the server omitted it
     */
    public Optional<String> accessTokenType() {
        return Optional.ofNullable(accessTokenType);
    }

    /**
     * Returns the email attribute associated with the redeemed authorization
     * code.
     *
     * @return the email attribute, or empty when the server omitted it
     */
    public Optional<String> emailAttribute() {
        return Optional.ofNullable(emailAttribute);
    }
}
