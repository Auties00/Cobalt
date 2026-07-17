package com.github.auties00.cobalt.wire.linked.sync.action.device;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents a single paid business feature flag carried inside a
 * {@link SubscriptionsSyncV2Action}.
 *
 * <p>Each entry advertises whether a named paid feature is enabled for the
 * authenticated WhatsApp Business account, optionally bounded by a usage limit
 * (for example a monthly cap on marketing messages) and an expiration timestamp
 * after which the feature reverts to its free tier behaviour.
 *
 * <p>A business account can carry many of these entries in parallel, one per
 * feature; the containing {@link SubscriptionsSyncV2Action} replicates the full
 * list to every linked device whenever subscription state changes server side.
 */
@ProtobufMessage(name = "SyncActionValue.SubscriptionsSyncV2Action.PaidFeature")
public final class PaidFeature {
    /**
     * Canonical name of the paid feature, as defined by the server side catalog.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String name;

    /**
     * Flag indicating whether the feature is currently enabled for the account.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean enabled;

    /**
     * Optional usage limit attached to the feature, for example a maximum number
     * of messages that can be sent within a billing window.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer limit;

    /**
     * Optional expiration timestamp, expressed in seconds since the epoch, after
     * which the feature is no longer considered enabled.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    Long expirationTime;

    /**
     * Constructs a new {@code PaidFeature} from raw protobuf field values.
     *
     * @param name           the canonical feature name, possibly {@code null}
     * @param enabled        whether the feature is enabled, possibly {@code null}
     * @param limit          the optional usage limit, possibly {@code null}
     * @param expirationTime the optional expiration timestamp in seconds,
     *                       possibly {@code null}
     */
    PaidFeature(String name, Boolean enabled, Integer limit, Long expirationTime) {
        this.name = name;
        this.enabled = enabled;
        this.limit = limit;
        this.expirationTime = expirationTime;
    }

    /**
     * Returns the canonical name of this paid feature, if one was encoded.
     *
     * @return an {@link Optional} containing the feature name, or
     *         {@link Optional#empty()} if absent
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns whether this feature is currently enabled for the account.
     *
     * @return {@code true} if the feature is enabled, {@code false} otherwise
     *         (including when the field was unset on the wire)
     */
    public boolean enabled() {
        return enabled != null && enabled;
    }

    /**
     * Returns the optional usage limit attached to this feature, if one was
     * encoded.
     *
     * @return an {@link OptionalInt} containing the limit, or
     *         {@link OptionalInt#empty()} if absent
     */
    public OptionalInt limit() {
        return limit == null ? OptionalInt.empty() : OptionalInt.of(limit);
    }

    /**
     * Returns the optional expiration timestamp for this feature, expressed in
     * seconds since the epoch, if one was encoded.
     *
     * @return an {@link OptionalLong} containing the expiration timestamp, or
     *         {@link OptionalLong#empty()} if absent
     */
    public OptionalLong expirationTime() {
        return expirationTime == null ? OptionalLong.empty() : OptionalLong.of(expirationTime);
    }

    /**
     * Sets the canonical feature name.
     *
     * @param name the new feature name, or {@code null} to clear
     * @return this instance for method chaining
     */
    public PaidFeature setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the enabled flag for this feature.
     *
     * @param enabled {@code true} to enable, {@code false} to disable, or
     *                {@code null} to clear
     * @return this instance for method chaining
     */
    public PaidFeature setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the optional usage limit.
     *
     * @param limit the new usage limit, or {@code null} to clear
     * @return this instance for method chaining
     */
    public PaidFeature setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the optional expiration timestamp.
     *
     * @param expirationTime the new expiration timestamp in seconds since the
     *                       epoch, or {@code null} to clear
     * @return this instance for method chaining
     */
    public PaidFeature setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
        return this;
    }
}
