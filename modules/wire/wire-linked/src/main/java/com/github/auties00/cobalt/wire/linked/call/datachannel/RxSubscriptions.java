package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One participant's set of receive-side subscriptions for a group call.
 *
 * <p>Comes in two parts: a fast PID-only list of every participant the
 * subscriber wants to receive any video from
 * ({@linkplain #vidRxPids() vid_rx_pids}), and a per-PID
 * {@link RxVidSubscriptionInfo} list specifying the requested quality
 * level ({@linkplain #vidSubscriptions() vid_subscriptions}). The SFU
 * uses these to compute the {@link BwaStream} bitmap.
 */
@ProtobufMessage(name = "RxSubscriptions")
public final class RxSubscriptions {
    /**
     * The PIDs the subscriber wants any video from.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32, packed = true)
    final List<Integer> vidRxPids;

    /**
     * The per-PID quality subscriptions.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<RxVidSubscriptionInfo> vidSubscriptions;

    /**
     * Constructs a new {@code RxSubscriptions}.
     *
     * @param vidRxPids        the PIDs the subscriber wants any video from
     * @param vidSubscriptions the per-PID quality entries
     */
    RxSubscriptions(List<Integer> vidRxPids, List<RxVidSubscriptionInfo> vidSubscriptions) {
        this.vidRxPids = vidRxPids;
        this.vidSubscriptions = vidSubscriptions;
    }

    /**
     * Returns the PIDs the subscriber wants any video from.
     *
     * @return an unmodifiable list of PIDs, never {@code null}
     */
    public List<Integer> vidRxPids() {
        return vidRxPids == null ? List.of() : Collections.unmodifiableList(vidRxPids);
    }

    /**
     * Returns the per-PID quality subscriptions.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<RxVidSubscriptionInfo> vidSubscriptions() {
        return vidSubscriptions == null ? List.of() : Collections.unmodifiableList(vidSubscriptions);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RxSubscriptions that
                && Objects.equals(this.vidRxPids, that.vidRxPids)
                && Objects.equals(this.vidSubscriptions, that.vidSubscriptions));
    }

    @Override
    public int hashCode() {
        return Objects.hash(vidRxPids, vidSubscriptions);
    }

    @Override
    public String toString() {
        return "RxSubscriptions[vidRxPids=" + vidRxPids()
                + ", vidSubscriptions=" + vidSubscriptions() + ']';
    }
}
