package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RTCP-feedback envelope carrying receive subscriptions for every
 * transmitter the subscriber wants media from.
 *
 * <p>Published as an RTCP application-feedback packet so the SFU learns,
 * in a single message, the receiver's subscription model across every
 * other participant in the call.
 */
@ProtobufMessage(name = "RtcpRxSubscriptions")
public final class RtcpRxSubscriptions {
    /**
     * The per-transmitter subscription entries.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<RtcpRxSubscriptionsPerParticipant> rxSubscriptions;

    /**
     * Constructs a new {@code RtcpRxSubscriptions}.
     *
     * @param rxSubscriptions the per-transmitter entries
     */
    RtcpRxSubscriptions(List<RtcpRxSubscriptionsPerParticipant> rxSubscriptions) {
        this.rxSubscriptions = rxSubscriptions;
    }

    /**
     * Returns the per-transmitter subscription entries.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<RtcpRxSubscriptionsPerParticipant> rxSubscriptions() {
        return rxSubscriptions == null ? List.of() : Collections.unmodifiableList(rxSubscriptions);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RtcpRxSubscriptions that
                && Objects.equals(this.rxSubscriptions, that.rxSubscriptions));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rxSubscriptions);
    }

    @Override
    public String toString() {
        return "RtcpRxSubscriptions[rxSubscriptions=" + rxSubscriptions() + ']';
    }
}
