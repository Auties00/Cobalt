package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The set of sender-side subscription extensions for every active sender.
 *
 * <p>Published alongside {@link StreamDescriptors} so receivers learn the
 * SSRC-to-PID layout for every participant emitting media. Carries one
 * {@link SenderSubscriptionExt} per sender.
 */
@ProtobufMessage(name = "SenderSubscriptions")
public final class SenderSubscriptions {
    /**
     * The per-sender subscription extensions.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<SenderSubscriptionExt> subscriptions;

    /**
     * Constructs a new {@code SenderSubscriptions}.
     *
     * @param subscriptions the per-sender entries
     */
    SenderSubscriptions(List<SenderSubscriptionExt> subscriptions) {
        this.subscriptions = subscriptions;
    }

    /**
     * Returns the per-sender subscription extensions.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<SenderSubscriptionExt> subscriptions() {
        return subscriptions == null ? List.of() : Collections.unmodifiableList(subscriptions);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof SenderSubscriptions that
                && Objects.equals(this.subscriptions, that.subscriptions));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subscriptions);
    }

    @Override
    public String toString() {
        return "SenderSubscriptions[subscriptions=" + subscriptions() + ']';
    }
}
