package com.github.auties00.cobalt.wire.linked.business.subscription;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * The subscription upsell entry points eligible for the current session.
 *
 * <p>WhatsApp advertises subscriptions (such as Meta Verified) through a set
 * of entry points, each gated by the session's eligibility. This model is the
 * list of {@linkplain SubscriptionEntryPoint entry points} the server reports
 * the session may surface, with each entry point's eligibility flag and the
 * URI the web client opens to begin the subscription flow.
 */
@ProtobufMessage(name = "BusinessSubscriptionEntryPoints")
public final class BusinessSubscriptionEntryPoints {
    /**
     * Subscription entry points eligible for the session, in the order the
     * server returned them. Never {@code null}, possibly empty when the server
     * returned none.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<SubscriptionEntryPoint> entryPoints;

    /**
     * Constructs a new {@code BusinessSubscriptionEntryPoints}. A {@code null}
     * {@code entryPoints} is coerced to an empty list.
     *
     * @param entryPoints the subscription entry points; {@code null} treated as empty
     */
    BusinessSubscriptionEntryPoints(List<SubscriptionEntryPoint> entryPoints) {
        this.entryPoints = entryPoints == null ? List.of() : entryPoints;
    }

    /**
     * Returns the subscription entry points eligible for the session.
     *
     * @return an unmodifiable view of the entry points; never {@code null},
     *         possibly empty
     */
    public List<SubscriptionEntryPoint> entryPoints() {
        return Collections.unmodifiableList(entryPoints);
    }
}
