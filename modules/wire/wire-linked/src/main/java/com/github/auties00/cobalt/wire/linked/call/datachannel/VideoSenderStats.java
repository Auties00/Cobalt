package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Sender-side video-pipeline metrics embedded in a peer-feedback packet.
 *
 * <p>Carries the sender's current bandwidth-estimate (BWE) for the
 * outbound video stream. The receiver may use it to anticipate quality
 * changes and adjust its subscription level.
 */
@ProtobufMessage(name = "VideoSenderStats")
public final class VideoSenderStats {
    /**
     * The sender's current bandwidth estimate in bits per second.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final Integer senderBwe;

    /**
     * Constructs a new {@code VideoSenderStats}.
     *
     * @param senderBwe the sender's BWE, or {@code null} when absent
     */
    VideoSenderStats(Integer senderBwe) {
        this.senderBwe = senderBwe;
    }

    /**
     * Returns the sender's bandwidth estimate in bps.
     *
     * @return an {@link OptionalInt} with the BWE, or empty
     */
    public OptionalInt senderBwe() {
        return senderBwe == null ? OptionalInt.empty() : OptionalInt.of(senderBwe);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof VideoSenderStats that
                && Objects.equals(this.senderBwe, that.senderBwe));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(senderBwe);
    }

    @Override
    public String toString() {
        return "VideoSenderStats[senderBwe=" + senderBwe + ']';
    }
}
