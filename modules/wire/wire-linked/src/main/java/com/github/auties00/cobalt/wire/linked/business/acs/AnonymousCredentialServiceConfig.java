package com.github.auties00.cobalt.wire.linked.business.acs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Server-published parameters the WhatsApp anonymous-credential service
 * binds to one credential-issuance project.
 *
 * <p>WhatsApp uses anonymous credentials (privacy-preserving tokens) for
 * surfaces where a client needs to prove it is entitled to do something
 * without revealing which user did it. Each surface is keyed off a named
 * project and pins its own cipher suite, public key, evaluation and
 * redemption limits per token, and the lifetimes of both the
 * configuration itself and the issued tokens. The client fetches these
 * parameters before blinding tokens to send for issuance.
 *
 * <p>This model is that per-project configuration.
 */
@ProtobufMessage(name = "AnonymousCredentialServiceConfig")
public final class AnonymousCredentialServiceConfig {
    /**
     * Server-issued identifier for this configuration, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Server-defined cipher-suite marker for this configuration, or
     * {@code null} when the server omitted it. The full marker set is not
     * recoverable from the WhatsApp client, so the raw marker is exposed
     * as a string.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String cipherSuite;

    /**
     * Base64url-encoded public key clients use to blind tokens for this
     * configuration, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String publicKey;

    /**
     * Maximum number of evaluations the server will run per issued token,
     * or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final Long evaluationLimit;

    /**
     * Maximum number of redemptions the server will accept per issued
     * token, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    final Long redemptionLimit;

    /**
     * Instant at which this configuration expires, encoded as an
     * epoch-millisecond. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64)
    final Long expiresAtEpochMilli;

    /**
     * Lifetime of an issued token, in milliseconds, or {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64)
    final Long tokenLifetimeMillis;

    /**
     * Constructs a new {@code AnonymousCredentialServiceConfig}. Any
     * reference argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param id                  the configuration identifier, or
     *                            {@code null}
     * @param cipherSuite         the cipher-suite marker, or {@code null}
     * @param publicKey           the base64url-encoded public key, or
     *                            {@code null}
     * @param evaluationLimit     the per-token evaluation limit, or
     *                            {@code null}
     * @param redemptionLimit     the per-token redemption limit, or
     *                            {@code null}
     * @param expiresAtEpochMilli the configuration expiry epoch
     *                            millisecond, or {@code null}
     * @param tokenLifetimeMillis the issued-token lifetime in
     *                            milliseconds, or {@code null}
     */
    AnonymousCredentialServiceConfig(String id, String cipherSuite, String publicKey, Long evaluationLimit,
                                     Long redemptionLimit, Long expiresAtEpochMilli, Long tokenLifetimeMillis) {
        this.id = id;
        this.cipherSuite = cipherSuite;
        this.publicKey = publicKey;
        this.evaluationLimit = evaluationLimit;
        this.redemptionLimit = redemptionLimit;
        this.expiresAtEpochMilli = expiresAtEpochMilli;
        this.tokenLifetimeMillis = tokenLifetimeMillis;
    }

    /**
     * Returns the server-issued configuration identifier.
     *
     * @return an {@code Optional} carrying the identifier, or empty when
     *         the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the server-defined cipher-suite marker.
     *
     * @return an {@code Optional} carrying the cipher-suite marker, or
     *         empty when the server omitted it
     */
    public Optional<String> cipherSuite() {
        return Optional.ofNullable(cipherSuite);
    }

    /**
     * Returns the base64url-encoded public key clients use to blind
     * tokens for this configuration.
     *
     * @return an {@code Optional} carrying the public key, or empty when
     *         the server omitted it
     */
    public Optional<String> publicKey() {
        return Optional.ofNullable(publicKey);
    }

    /**
     * Returns the maximum number of evaluations per issued token.
     *
     * @return an {@code OptionalLong} carrying the limit, or empty when
     *         the server omitted it
     */
    public OptionalLong evaluationLimit() {
        return evaluationLimit != null ? OptionalLong.of(evaluationLimit) : OptionalLong.empty();
    }

    /**
     * Returns the maximum number of redemptions per issued token.
     *
     * @return an {@code OptionalLong} carrying the limit, or empty when
     *         the server omitted it
     */
    public OptionalLong redemptionLimit() {
        return redemptionLimit != null ? OptionalLong.of(redemptionLimit) : OptionalLong.empty();
    }

    /**
     * Returns the instant at which this configuration expires.
     *
     * @return an {@code Optional} carrying the expiry instant, or empty
     *         when the server omitted it
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAtEpochMilli).map(Instant::ofEpochMilli);
    }

    /**
     * Returns the lifetime of an issued token.
     *
     * @return an {@code Optional} carrying the token lifetime as a
     *         {@link Duration}, or empty when the server omitted it
     */
    public Optional<Duration> tokenLifetime() {
        return Optional.ofNullable(tokenLifetimeMillis).map(Duration::ofMillis);
    }
}
