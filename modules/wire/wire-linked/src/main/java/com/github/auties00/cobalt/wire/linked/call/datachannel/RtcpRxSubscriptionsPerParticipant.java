package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * One transmitting participant's receive-subscription set inside an
 * {@link RtcpRxSubscriptions}.
 *
 * <p>Identifies the transmitting participant by {@linkplain #txPid() PID}
 * and carries the {@link RxSubscriptions} the SFU should honour when
 * forwarding that participant's media.
 */
@ProtobufMessage(name = "RtcpRxSubscriptionsPerParticipant")
public final class RtcpRxSubscriptionsPerParticipant {
    /**
     * The transmitting participant's PID.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer txPid;

    /**
     * The subscriptions to apply to this transmitter.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final RxSubscriptions rxSubscriptions;

    /**
     * Constructs a new {@code RtcpRxSubscriptionsPerParticipant}.
     *
     * @param txPid           the transmitter PID
     * @param rxSubscriptions the subscriptions to apply
     */
    RtcpRxSubscriptionsPerParticipant(Integer txPid, RxSubscriptions rxSubscriptions) {
        this.txPid = txPid;
        this.rxSubscriptions = rxSubscriptions;
    }

    /**
     * Returns the transmitting participant's PID.
     *
     * @return an {@link OptionalInt} with the PID, or empty
     */
    public OptionalInt txPid() {
        return txPid == null ? OptionalInt.empty() : OptionalInt.of(txPid);
    }

    /**
     * Returns the subscriptions to apply.
     *
     * @return an {@link Optional} with the subscriptions, or empty
     */
    public Optional<RxSubscriptions> rxSubscriptions() {
        return Optional.ofNullable(rxSubscriptions);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RtcpRxSubscriptionsPerParticipant that
                && Objects.equals(this.txPid, that.txPid)
                && Objects.equals(this.rxSubscriptions, that.rxSubscriptions));
    }

    @Override
    public int hashCode() {
        return Objects.hash(txPid, rxSubscriptions);
    }

    @Override
    public String toString() {
        return "RtcpRxSubscriptionsPerParticipant[txPid=" + txPid
                + ", rxSubscriptions=" + rxSubscriptions + ']';
    }
}
