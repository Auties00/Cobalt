package com.github.auties00.cobalt.calls.transport.subscription;

import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptions;

import java.util.Objects;
import java.util.Optional;

/**
 * The cached receive subscription that the relay transport periodically resends.
 *
 * <p>The client publishes its {@link RxSubscriptions} to the selective forwarding unit
 * inside a STUN binding request and then resends the same subscription on a timer so a
 * dropped binding does not strand the receiver with stale forwarding. To avoid flooding
 * the unit with identical bindings, the resend is suppressed whenever the subscription
 * has not changed since the last publish. This state holds the last published
 * subscription and answers whether a candidate subscription differs from it.
 *
 * <p>A fresh state holds no subscription, so the first
 * {@link #shouldPublish(RxSubscriptions)} after construction always reports that a
 * publish is due. {@link #record(RxSubscriptions)} updates the cached subscription after
 * a binding is sent. This state is not thread safe: the single call transport thread
 * that drives the {@link LiveSubscriptionPublisher} owns it.
 *
 * @implNote This implementation compares the candidate against the cached subscription
 * through {@link RxSubscriptions#equals(Object)} on the reused model type, which weighs the
 * PID list and the per PID quality entries. The {@code DataChannel} affinity the model also
 * carries is irrelevant on this STUN path and does not affect equality, so a subscription
 * that differs only in that affinity is treated as unchanged and its resend is suppressed.
 */
public final class RxSubscriptionState {
    /**
     * The last receive subscription that was published, or {@code null} before the first
     * publish.
     *
     * <p>A {@code null} value means no subscription has been published yet, in which case
     * any candidate is considered changed so the first publish proceeds.
     */
    private RxSubscriptions cached;

    /**
     * Constructs an empty subscription state holding no cached subscription.
     *
     * <p>The first {@link #shouldPublish(RxSubscriptions)} after construction reports a
     * publish is due because there is nothing to compare against.
     */
    public RxSubscriptionState() {
        this.cached = null;
    }

    /**
     * Returns whether the candidate subscription differs from the cached one and should
     * therefore be published.
     *
     * <p>Reports {@code true} when no subscription has been cached yet or when the
     * candidate is not equal to the cached subscription, and {@code false} when the
     * candidate is identical to the last published subscription. This method only
     * compares; it does not update the cache, so a caller publishes the binding and then
     * calls {@link #record(RxSubscriptions)} to remember it.
     *
     * @param candidate the subscription about to be published; must not be {@code null}
     * @return {@code true} if the candidate should be published, {@code false} if it is a
     *         redundant resend
     * @throws NullPointerException if {@code candidate} is {@code null}
     */
    public boolean shouldPublish(RxSubscriptions candidate) {
        Objects.requireNonNull(candidate, "candidate cannot be null");
        return !candidate.equals(cached);
    }

    /**
     * Records the candidate as the last published subscription.
     *
     * <p>After this call a {@link #shouldPublish(RxSubscriptions)} with an equal
     * subscription reports {@code false}, suppressing the redundant resend. Callers
     * invoke this once the STUN binding carrying the subscription has been sent.
     *
     * @param candidate the subscription that was published; must not be {@code null}
     * @throws NullPointerException if {@code candidate} is {@code null}
     */
    public void record(RxSubscriptions candidate) {
        this.cached = Objects.requireNonNull(candidate, "candidate cannot be null");
    }

    /**
     * Returns the last published subscription, if any.
     *
     * @return an {@link Optional} holding the cached subscription, or empty before the
     *         first publish
     */
    public Optional<RxSubscriptions> cached() {
        return Optional.ofNullable(cached);
    }

    /**
     * Clears the cached subscription so the next publish is never suppressed.
     *
     * <p>Invoked on relay failover or call teardown so a re established transport
     * republishes the receive subscription from scratch rather than treating it as an
     * unchanged resend.
     */
    public void clear() {
        this.cached = null;
    }
}
