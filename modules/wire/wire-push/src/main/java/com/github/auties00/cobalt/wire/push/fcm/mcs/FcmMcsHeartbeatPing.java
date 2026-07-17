package com.github.auties00.cobalt.wire.push.fcm.mcs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Models a heartbeat ping on the MCS stream, in either direction.
 *
 * <p>A ping is sent outbound on the periodic heartbeat timer to keep the TLS
 * stream alive, and is also received inbound when the server probes the client.
 * Either side answers a ping with {@link FcmMcsHeartbeatAck}, which shares this
 * field shape plus a status field.
 *
 * @implNote This implementation carries the MCS frame tag {@code 0}; the tag is
 * written as the frame's length-prefixed type byte by the connection layer, not
 * by this message.
 */
@ProtobufMessage(name = "FcmMcsHeartbeatPing")
public final class FcmMcsHeartbeatPing {
    /**
     * Holds the last stream id the sender has observed.
     *
     * <p>This lets the peer infer the cursor without parsing any stanza
     * payload.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    long lastStreamIdReceived;

    /**
     * Constructs a new ping advertising the given cursor.
     *
     * @param lastStreamIdReceived the last stream id observed
     */
    FcmMcsHeartbeatPing(long lastStreamIdReceived) {
        this.lastStreamIdReceived = lastStreamIdReceived;
    }

    /**
     * Returns the last stream id the sender has observed.
     *
     * @return the cursor
     */
    public long lastStreamIdReceived() {
        return lastStreamIdReceived;
    }
}
