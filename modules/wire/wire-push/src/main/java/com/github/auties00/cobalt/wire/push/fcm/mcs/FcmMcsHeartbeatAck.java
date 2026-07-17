package com.github.auties00.cobalt.wire.push.fcm.mcs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Models an outbound heartbeat ack on the MCS stream.
 *
 * <p>A heartbeat ack is sent in response to an incoming server ping; it carries
 * the same stream cursor as {@link FcmMcsHeartbeatPing} plus a status field, so
 * the server can confirm liveness and advance its view of the client cursor.
 *
 * @implNote This implementation carries the MCS frame tag {@code 1}; the tag is
 * written as the frame's length-prefixed type byte by the connection layer, not
 * by this message. The {@link #status} field is always {@code 0}, matching the
 * native client.
 */
@ProtobufMessage(name = "FcmMcsHeartbeatAck")
public final class FcmMcsHeartbeatAck {
    /**
     * Holds the last stream id the client has observed.
     *
     * <p>This lets the server infer the cursor without parsing any stanza
     * payload.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    long lastStreamIdReceived;

    /**
     * Holds the heartbeat status.
     *
     * @implNote This implementation always sends {@code 0}, matching the native
     * client, which never populates a non-zero status on an ack.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    long status;

    /**
     * Constructs a new ack with the given cursor and status.
     *
     * @param lastStreamIdReceived the last stream id observed
     * @param status               the heartbeat status
     */
    FcmMcsHeartbeatAck(long lastStreamIdReceived, long status) {
        this.lastStreamIdReceived = lastStreamIdReceived;
        this.status = status;
    }
}
