package com.github.auties00.cobalt.wire.linked.business.subscription;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * The subscriptions the caller's WhatsApp account holds and the features they
 * unlock.
 *
 * <p>A WhatsApp account can hold paid subscriptions (such as Meta Verified),
 * and each unlocks a set of features. This model is the account's full
 * subscription state as the server reports it: the list of held
 * {@linkplain BusinessSubscription subscriptions} together with the
 * {@linkplain SubscriptionFeatureFlag feature flags} they unlock, each with
 * its enabled state, limit, and expiry.
 */
@ProtobufMessage(name = "BusinessSubscriptions")
public final class BusinessSubscriptions {
    /**
     * Subscriptions the account holds, in the order the server returned them.
     * Never {@code null}, possibly empty when the account holds none.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<BusinessSubscription> subscriptions;

    /**
     * Feature flags the held subscriptions unlock, in the order the server
     * returned them. Never {@code null}, possibly empty when none are
     * reported.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<SubscriptionFeatureFlag> featureFlags;

    /**
     * Constructs a new {@code BusinessSubscriptions}. A {@code null}
     * {@code subscriptions} or {@code featureFlags} is coerced to an empty
     * list.
     *
     * @param subscriptions the held subscriptions; {@code null} treated as empty
     * @param featureFlags  the unlocked feature flags; {@code null} treated as empty
     */
    BusinessSubscriptions(List<BusinessSubscription> subscriptions, List<SubscriptionFeatureFlag> featureFlags) {
        this.subscriptions = subscriptions == null ? List.of() : subscriptions;
        this.featureFlags = featureFlags == null ? List.of() : featureFlags;
    }

    /**
     * Returns the subscriptions the account holds.
     *
     * @return an unmodifiable view of the held subscriptions; never
     *         {@code null}, possibly empty
     */
    public List<BusinessSubscription> subscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }

    /**
     * Returns the feature flags the held subscriptions unlock.
     *
     * @return an unmodifiable view of the unlocked feature flags; never
     *         {@code null}, possibly empty
     */
    public List<SubscriptionFeatureFlag> featureFlags() {
        return Collections.unmodifiableList(featureFlags);
    }
}
