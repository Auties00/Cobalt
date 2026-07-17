package com.github.auties00.cobalt.wire.linked.sync.action.device;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents per subscription metadata carried inside a
 * {@link SubscriptionsSyncV2Action}.
 *
 * <p>Each entry describes one active or historical paid subscription owned by
 * the authenticated WhatsApp Business account, including its server issued
 * identifier, its tier, current status string, the start and end timestamps of
 * the current billing window, its origin source (for example a specific app
 * store or billing provider), a flag signalling whether the billing platform
 * changed since creation, and the creation timestamp.
 *
 * <p>A business account can carry many of these entries in parallel; the
 * containing {@link SubscriptionsSyncV2Action} replicates the full list to
 * every linked device whenever subscription state changes server side.
 */
@ProtobufMessage(name = "SyncActionValue.SubscriptionsSyncV2Action.SubscriptionInfo")
public final class SubscriptionInfo {
    /**
     * Opaque subscription identifier issued by the server.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * Numeric tier of the subscription, higher values typically mapping to more
     * capable plans.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer tier;

    /**
     * Current status string for the subscription, drawn from a server defined
     * enumeration (for example active, suspended, cancelled).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String status;

    /**
     * Start timestamp of the current billing window, expressed in seconds since
     * the epoch.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    Long startTime;

    /**
     * End (expiration) timestamp of the current billing window, expressed in
     * seconds since the epoch.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    Long endTime;

    /**
     * Flag indicating whether the billing platform backing the subscription
     * changed since the subscription was created (for example a migration from
     * one app store to another).
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean isPlatformChanged;

    /**
     * Origin source string for this subscription, drawn from a server defined
     * enumeration that names the billing provider or acquisition channel.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String source;

    /**
     * Creation timestamp of the subscription, expressed in seconds since the
     * epoch.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT64)
    Long creationTime;

    /**
     * Constructs a new {@code SubscriptionInfo} from raw protobuf field values.
     *
     * @param id                the opaque subscription id, possibly {@code null}
     * @param tier              the subscription tier, possibly {@code null}
     * @param status            the subscription status string, possibly
     *                          {@code null}
     * @param startTime         the billing window start timestamp in seconds,
     *                          possibly {@code null}
     * @param endTime           the billing window end timestamp in seconds,
     *                          possibly {@code null}
     * @param isPlatformChanged whether the billing platform changed since
     *                          creation, possibly {@code null}
     * @param source            the origin source string, possibly {@code null}
     * @param creationTime      the creation timestamp in seconds, possibly
     *                          {@code null}
     */
    SubscriptionInfo(String id, Integer tier, String status, Long startTime, Long endTime, Boolean isPlatformChanged, String source, Long creationTime) {
        this.id = id;
        this.tier = tier;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isPlatformChanged = isPlatformChanged;
        this.source = source;
        this.creationTime = creationTime;
    }

    /**
     * Returns the opaque subscription identifier, if one was encoded.
     *
     * @return an {@link Optional} containing the id, or {@link Optional#empty()}
     *         if absent
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the numeric tier of the subscription, if one was encoded.
     *
     * @return an {@link OptionalInt} containing the tier, or
     *         {@link OptionalInt#empty()} if absent
     */
    public OptionalInt tier() {
        return tier == null ? OptionalInt.empty() : OptionalInt.of(tier);
    }

    /**
     * Returns the current status string for the subscription, if one was
     * encoded.
     *
     * @return an {@link Optional} containing the status, or
     *         {@link Optional#empty()} if absent
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the start timestamp of the current billing window, expressed in
     * seconds since the epoch, if one was encoded.
     *
     * @return an {@link OptionalLong} containing the start timestamp, or
     *         {@link OptionalLong#empty()} if absent
     */
    public OptionalLong startTime() {
        return startTime == null ? OptionalLong.empty() : OptionalLong.of(startTime);
    }

    /**
     * Returns the end (expiration) timestamp of the current billing window,
     * expressed in seconds since the epoch, if one was encoded.
     *
     * @return an {@link OptionalLong} containing the end timestamp, or
     *         {@link OptionalLong#empty()} if absent
     */
    public OptionalLong endTime() {
        return endTime == null ? OptionalLong.empty() : OptionalLong.of(endTime);
    }

    /**
     * Returns whether the billing platform backing the subscription changed
     * since it was created.
     *
     * @return {@code true} if the platform changed, {@code false} otherwise
     *         (including when the field was unset on the wire)
     */
    public boolean isPlatformChanged() {
        return isPlatformChanged != null && isPlatformChanged;
    }

    /**
     * Returns the origin source string for this subscription, if one was
     * encoded.
     *
     * @return an {@link Optional} containing the source, or
     *         {@link Optional#empty()} if absent
     */
    public Optional<String> source() {
        return Optional.ofNullable(source);
    }

    /**
     * Returns the creation timestamp of the subscription, expressed in seconds
     * since the epoch, if one was encoded.
     *
     * @return an {@link OptionalLong} containing the creation timestamp, or
     *         {@link OptionalLong#empty()} if absent
     */
    public OptionalLong creationTime() {
        return creationTime == null ? OptionalLong.empty() : OptionalLong.of(creationTime);
    }

    /**
     * Sets the opaque subscription identifier.
     *
     * @param id the new subscription id, or {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionInfo setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the numeric tier of the subscription.
     *
     * @param tier the new tier, or {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionInfo setTier(Integer tier) {
        this.tier = tier;
        return this;
    }

    /**
     * Sets the current status string.
     *
     * @param status the new status, or {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionInfo setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Sets the start timestamp of the current billing window.
     *
     * @param startTime the new start timestamp in seconds since the epoch, or
     *                  {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionInfo setStartTime(Long startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * Sets the end (expiration) timestamp of the current billing window.
     *
     * @param endTime the new end timestamp in seconds since the epoch, or
     *                {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionInfo setEndTime(Long endTime) {
        this.endTime = endTime;
        return this;
    }

    /**
     * Sets the flag indicating whether the billing platform changed since
     * creation.
     *
     * @param isPlatformChanged {@code true} if the platform changed,
     *                          {@code false} otherwise, or {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionInfo setPlatformChanged(Boolean isPlatformChanged) {
        this.isPlatformChanged = isPlatformChanged;
        return this;
    }

    /**
     * Sets the origin source string for this subscription.
     *
     * @param source the new source, or {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionInfo setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * Sets the creation timestamp of the subscription.
     *
     * @param creationTime the new creation timestamp in seconds since the epoch,
     *                     or {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionInfo setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
        return this;
    }
}
