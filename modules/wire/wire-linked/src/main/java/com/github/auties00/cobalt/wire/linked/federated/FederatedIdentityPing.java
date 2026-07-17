package com.github.auties00.cobalt.wire.linked.federated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Reply payload of a federated-identity ("Waffle") liveness ping.
 *
 * <p>Once a WhatsApp account is linked to a Meta-side identity the bridge
 * needs a periodic heartbeat to know the client is still alive and the link
 * is still valid. The client sends an encrypted ping carrying the linked
 * Facebook id, and the relay replies with the seconds-between-pings cadence
 * the client should respect from now on. The cadence is bridge-driven so the
 * relay can dampen traffic during maintenance windows or accelerate it when
 * a state transition is in flight.
 */
@ProtobufMessage(name = "FederatedIdentityPing")
public final class FederatedIdentityPing {
    /**
     * Seconds the client should wait before issuing the next ping. The value
     * is always positive and is set by the relay; clients must never override
     * it with a shorter cadence.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    int pingInterval;

    /**
     * Constructs a new {@code FederatedIdentityPing} reply.
     *
     * @param pingInterval the relay-chosen seconds between pings
     */
    FederatedIdentityPing(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    /**
     * Returns the seconds the client should wait before the next ping.
     *
     * @return the ping interval, in seconds
     */
    public int pingInterval() {
        return pingInterval;
    }

    /**
     * Replaces the seconds between pings.
     *
     * @param pingInterval the new ping interval, in seconds
     */
    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }
}
