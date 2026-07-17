package com.github.auties00.cobalt.wire.linked.business.subscription;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * One subscription the caller's WhatsApp account currently holds.
 *
 * <p>A WhatsApp account can hold paid subscriptions such as Meta Verified.
 * Each subscription carries a stable {@linkplain #id() identifier}, its current
 * {@linkplain #status() status}, the {@linkplain #tier() tier} it grants, the
 * {@linkplain #source() channel} it was purchased through, and a flag noting
 * whether it {@linkplain #platformChanged() changed platform}. Its lifecycle
 * is bounded by a {@linkplain #creationTime() creation},
 * {@linkplain #startTime() start}, and {@linkplain #endTime() end} time.
 *
 * <p>This model is one such held subscription as the server reports it. The
 * status, tier, and source are exposed as raw markers because their value sets
 * are server-defined and not closed in the client.
 */
@ProtobufMessage(name = "BusinessSubscription")
public final class BusinessSubscription {
    /**
     * Stable identifier of the subscription. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Current status of the subscription, as a server-defined marker.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String status;

    /**
     * Time the subscription ends, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant endTime;

    /**
     * Time the subscription was created, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant creationTime;

    /**
     * Tier the subscription grants, as a server-defined marker. {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String tier;

    /**
     * Channel the subscription was purchased through, as a server-defined
     * marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String source;

    /**
     * Whether the subscription changed platform. Reported by the server as a
     * flag; {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    final boolean platformChanged;

    /**
     * Time the subscription starts, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant startTime;

    /**
     * Constructs a new {@code BusinessSubscription}. The reference arguments
     * may be {@code null} when the server omitted them.
     *
     * @param id              the subscription identifier, or {@code null}
     * @param status          the status marker, or {@code null}
     * @param endTime         the end time, or {@code null}
     * @param creationTime    the creation time, or {@code null}
     * @param tier            the tier marker, or {@code null}
     * @param source          the source marker, or {@code null}
     * @param platformChanged whether the subscription changed platform
     * @param startTime       the start time, or {@code null}
     */
    BusinessSubscription(String id, String status, Instant endTime, Instant creationTime, String tier,
                         String source, boolean platformChanged, Instant startTime) {
        this.id = id;
        this.status = status;
        this.endTime = endTime;
        this.creationTime = creationTime;
        this.tier = tier;
        this.source = source;
        this.platformChanged = platformChanged;
        this.startTime = startTime;
    }

    /**
     * Returns the stable identifier of the subscription.
     *
     * @return the subscription identifier, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the current status of the subscription.
     *
     * @return the status marker, or empty when the server omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the time the subscription ends.
     *
     * @return the end time, or empty when the server omitted it
     */
    public Optional<Instant> endTime() {
        return Optional.ofNullable(endTime);
    }

    /**
     * Returns the time the subscription was created.
     *
     * @return the creation time, or empty when the server omitted it
     */
    public Optional<Instant> creationTime() {
        return Optional.ofNullable(creationTime);
    }

    /**
     * Returns the tier the subscription grants.
     *
     * @return the tier marker, or empty when the server omitted it
     */
    public Optional<String> tier() {
        return Optional.ofNullable(tier);
    }

    /**
     * Returns the channel the subscription was purchased through.
     *
     * @return the source marker, or empty when the server omitted it
     */
    public Optional<String> source() {
        return Optional.ofNullable(source);
    }

    /**
     * Returns whether the subscription changed platform.
     *
     * @return {@code true} when the server flagged a platform change,
     *         {@code false} otherwise
     */
    public boolean platformChanged() {
        return platformChanged;
    }

    /**
     * Returns the time the subscription starts.
     *
     * @return the start time, or empty when the server omitted it
     */
    public Optional<Instant> startTime() {
        return Optional.ofNullable(startTime);
    }
}
