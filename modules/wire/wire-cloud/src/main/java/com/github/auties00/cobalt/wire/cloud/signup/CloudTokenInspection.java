package com.github.auties00.cobalt.wire.cloud.signup;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The introspection result of Facebook Login's {@code /debug_token} endpoint.
 *
 * <p>This model projects the {@code data} object returned for a single inspected token: the app and
 * user it belongs to, whether it is currently valid, its issue and expiry instants, and the
 * permissions it grants. It is typically used after Embedded Signup to confirm the granted token
 * carries the required WhatsApp scopes before storing it.
 */
public final class CloudTokenInspection {
    /**
     * The id of the app the token was issued for, always present on a {@code /debug_token} response.
     */
    private final String appId;

    /**
     * The token type (for example {@code USER}), or {@code null} when not returned.
     */
    private final String type;

    /**
     * The display name of the app, or {@code null} when not returned.
     */
    private final String application;

    /**
     * Whether the token is currently valid.
     */
    private final boolean valid;

    /**
     * The instant the token was issued, or {@code null} when not returned.
     */
    private final Instant issuedAt;

    /**
     * The instant the token expires, or {@code null} when the token does not expire.
     */
    private final Instant expiresAt;

    /**
     * The permissions the token grants.
     */
    private final List<String> scopes;

    /**
     * The id of the user the token belongs to, or {@code null} when not returned.
     */
    private final String userId;

    /**
     * Constructs a new token inspection result.
     *
     * @param appId       the app id
     * @param type        the token type, or {@code null}
     * @param application the app display name, or {@code null}
     * @param valid       whether the token is valid
     * @param issuedAt    the issue instant, or {@code null}
     * @param expiresAt   the expiry instant, or {@code null} when the token does not expire
     * @param scopes      the granted scopes, or {@code null} for none
     * @param userId      the user id, or {@code null}
     * @throws NullPointerException if {@code appId} is {@code null}
     */
    public CloudTokenInspection(String appId, String type, String application, boolean valid,
                                Instant issuedAt, Instant expiresAt, List<String> scopes, String userId) {
        this.appId = Objects.requireNonNull(appId, "appId must not be null");
        this.type = type;
        this.application = application;
        this.valid = valid;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.scopes = scopes == null ? List.of() : List.copyOf(scopes);
        this.userId = userId;
    }

    /**
     * Returns the id of the app the token was issued for.
     *
     * @return the app id, always present
     */
    public String appId() {
        return appId;
    }

    /**
     * Returns the token type.
     *
     * @return an {@link Optional} carrying the type, or empty when not returned
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the display name of the app.
     *
     * @return an {@link Optional} carrying the application name, or empty when not returned
     */
    public Optional<String> application() {
        return Optional.ofNullable(application);
    }

    /**
     * Returns whether the token is currently valid.
     *
     * @return {@code true} when the token is valid
     */
    public boolean valid() {
        return valid;
    }

    /**
     * Returns the instant the token was issued.
     *
     * @return an {@link Optional} carrying the issue instant, or empty when not returned
     */
    public Optional<Instant> issuedAt() {
        return Optional.ofNullable(issuedAt);
    }

    /**
     * Returns the instant the token expires.
     *
     * @return an {@link Optional} carrying the expiry instant, or empty when the token does not expire
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    /**
     * Returns the permissions the token grants.
     *
     * @return an unmodifiable list of scopes, empty when none were returned
     */
    public List<String> scopes() {
        return scopes;
    }

    /**
     * Returns the id of the user the token belongs to.
     *
     * @return an {@link Optional} carrying the user id, or empty when not returned
     */
    public Optional<String> userId() {
        return Optional.ofNullable(userId);
    }
}
