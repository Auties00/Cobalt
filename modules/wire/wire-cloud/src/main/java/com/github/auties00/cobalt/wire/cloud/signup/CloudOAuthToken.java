package com.github.auties00.cobalt.wire.cloud.signup;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * An access token returned by Facebook Login's {@code /oauth/access_token} endpoint.
 *
 * <p>This model carries the token produced during the server-side leg of Embedded Signup, both when
 * exchanging an authorization code for a token and when exchanging a short-lived token for a
 * long-lived one. The expiry is optional because some grants return a non-expiring token and omit it.
 */
public final class CloudOAuthToken {
    /**
     * The access token value.
     */
    private final String accessToken;

    /**
     * The token type, typically {@code bearer}, or {@code null} when not returned.
     */
    private final String tokenType;

    /**
     * The token lifetime, or {@code null} when the token does not expire.
     */
    private final Duration expiresIn;

    /**
     * Constructs a new OAuth token.
     *
     * @param accessToken the access token value
     * @param tokenType   the token type, or {@code null}
     * @param expiresIn   the lifetime, or {@code null} when the token does not expire
     * @throws NullPointerException if {@code accessToken} is {@code null}
     */
    public CloudOAuthToken(String accessToken, String tokenType, Duration expiresIn) {
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    /**
     * Returns the access token value.
     *
     * @return the access token
     */
    public String accessToken() {
        return accessToken;
    }

    /**
     * Returns the token type.
     *
     * @return an {@link Optional} carrying the token type, or empty when not returned
     */
    public Optional<String> tokenType() {
        return Optional.ofNullable(tokenType);
    }

    /**
     * Returns the token lifetime.
     *
     * @return an {@link Optional} carrying the lifetime as a {@link Duration}, or empty when the token
     *         does not expire
     */
    public Optional<Duration> expiresIn() {
        return Optional.ofNullable(expiresIn);
    }
}
