package com.github.auties00.cobalt.wire.linked.business.subscription;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * One feature unlocked by the caller's WhatsApp subscriptions.
 *
 * <p>A held subscription unlocks a set of features, each reported as a named
 * flag. A flag carries its {@linkplain #name() name}, whether it is currently
 * {@linkplain #enabled() enabled}, an optional {@linkplain #expirationTime()
 * expiry}, and an optional numeric {@linkplain #limit() limit} bounding how
 * much of the feature the account may use.
 *
 * <p>This model is one such feature flag as the server reports it. The flag
 * name is exposed as a raw marker because the value set is server-defined.
 */
@ProtobufMessage(name = "SubscriptionFeatureFlag")
public final class SubscriptionFeatureFlag {
    /**
     * Name of the feature flag, as a server-defined marker. {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Whether the feature flag is enabled. Reported by the server as a flag;
     * {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean enabled;

    /**
     * Time the feature flag expires, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant expirationTime;

    /**
     * Numeric limit bounding the feature's use, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final Long limit;

    /**
     * Constructs a new {@code SubscriptionFeatureFlag}. The reference arguments
     * may be {@code null} when the server omitted them.
     *
     * @param name           the flag name marker, or {@code null}
     * @param enabled        whether the flag is enabled
     * @param expirationTime the expiry time, or {@code null}
     * @param limit          the numeric limit, or {@code null}
     */
    SubscriptionFeatureFlag(String name, boolean enabled, Instant expirationTime, Long limit) {
        this.name = name;
        this.enabled = enabled;
        this.expirationTime = expirationTime;
        this.limit = limit;
    }

    /**
     * Returns the name of the feature flag.
     *
     * @return the flag name marker, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns whether the feature flag is enabled.
     *
     * @return {@code true} when the server reported the flag enabled,
     *         {@code false} otherwise
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Returns the time the feature flag expires.
     *
     * @return the expiry time, or empty when the server omitted it
     */
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    /**
     * Returns the numeric limit bounding the feature's use.
     *
     * @return the numeric limit, or empty when the server omitted it
     */
    public OptionalLong limit() {
        return limit == null ? OptionalLong.empty() : OptionalLong.of(limit);
    }
}
