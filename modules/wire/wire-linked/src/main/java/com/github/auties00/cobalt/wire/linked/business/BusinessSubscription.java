package com.github.auties00.cobalt.wire.linked.business;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A model representing a single paid WhatsApp Business subscription owned by
 * this account.
 *
 * <p>WhatsApp Business sells paid add-ons (catalog hosting, marketing-message
 * tier, premium support) on top of the free tier. Each subscription tracked
 * here describes one such add-on through its lifecycle: a stable
 * {@linkplain #id() identifier}, the latest server-reported
 * {@linkplain #status() status string}, the {@linkplain #expiration()
 * expiration} timestamp and the {@linkplain #createdAt() creation} timestamp.
 *
 * <p>This record collapses three previously-separate stores (status,
 * expiration, creation time) into a single coherent entity, ensuring that the
 * three fields stay in sync and that subscription queries can be answered
 * with a single lookup.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class BusinessSubscription {
    /**
     * The non-{@code null} stable identifier of this subscription assigned by
     * the WhatsApp Business backend. Used as the primary key by Cobalt's
     * store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * The latest server-reported lifecycle status string for this
     * subscription, or {@code null} when the server has not yet reported a
     * status.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String status;

    /**
     * The instant at which this subscription expires, or {@code null} when no
     * expiration has been received from the server.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant expiration;

    /**
     * The instant at which this subscription was created, or {@code null}
     * when no creation time has been received from the server.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant createdAt;

    /**
     * Constructs a new business subscription with the given fields.
     *
     * @param id         the non-{@code null} subscription identifier
     * @param status     the latest lifecycle status string, or {@code null}
     * @param expiration the expiration instant, or {@code null}
     * @param createdAt  the creation instant, or {@code null}
     */
    BusinessSubscription(String id, String status, Instant expiration, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.status = status;
        this.expiration = expiration;
        this.createdAt = createdAt;
    }

    /**
     * Returns the non-{@code null} subscription identifier.
     *
     * @return the subscription identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the latest server-reported lifecycle status of this
     * subscription.
     *
     * @return an {@code Optional} containing the status string, or empty if
     *         the server has not yet reported a status
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Updates the lifecycle status of this subscription.
     *
     * @param status the new status string, or {@code null} to clear it
     * @return this subscription instance for method chaining
     */
    public BusinessSubscription setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Returns the expiration instant of this subscription.
     *
     * @return an {@code Optional} containing the expiration instant, or empty
     *         when no expiration has been received
     */
    public Optional<Instant> expiration() {
        return Optional.ofNullable(expiration);
    }

    /**
     * Updates the expiration instant of this subscription.
     *
     * @param expiration the new expiration instant, or {@code null} to clear
     *                   it
     * @return this subscription instance for method chaining
     */
    public BusinessSubscription setExpiration(Instant expiration) {
        this.expiration = expiration;
        return this;
    }

    /**
     * Returns the creation instant of this subscription.
     *
     * @return an {@code Optional} containing the creation instant, or empty
     *         when no creation time has been received
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Updates the creation instant of this subscription.
     *
     * @param createdAt the new creation instant, or {@code null} to clear it
     * @return this subscription instance for method chaining
     */
    public BusinessSubscription setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Returns a hash code derived from this subscription's {@linkplain #id() identifier}.
     *
     * @return the hash code of the subscription identifier
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Returns whether this subscription is equal to the given object.
     *
     * <p>Two subscriptions are considered equal when they share the same
     * {@linkplain #id() identifier}, regardless of their other fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code BusinessSubscription}
     *         with the same identifier
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessSubscription that && Objects.equals(this.id, that.id);
    }
}
