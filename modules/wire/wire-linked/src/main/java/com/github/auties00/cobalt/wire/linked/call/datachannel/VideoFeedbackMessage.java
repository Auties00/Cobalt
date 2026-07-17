package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Receiver-side video-pipeline feedback embedded in a peer-feedback packet.
 *
 * <p>Carries the receiver's classification of the inbound video stream's
 * freeze state. The {@linkplain #freezeState() freeze state} encodes the
 * current degradation level the receiver's renderer is experiencing, on a
 * runtime-defined scale; the sender uses it as one input to its bitrate
 * adaptation loop.
 */
@ProtobufMessage(name = "VideoFeedbackMessage")
public final class VideoFeedbackMessage {
    /**
     * The receiver-reported freeze classification value.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final Integer freezeState;

    /**
     * Constructs a new {@code VideoFeedbackMessage}.
     *
     * @param freezeState the freeze classification, or {@code null}
     */
    VideoFeedbackMessage(Integer freezeState) {
        this.freezeState = freezeState;
    }

    /**
     * Returns the freeze classification value.
     *
     * @return an {@link OptionalInt} with the value, or empty
     */
    public OptionalInt freezeState() {
        return freezeState == null ? OptionalInt.empty() : OptionalInt.of(freezeState);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof VideoFeedbackMessage that
                && Objects.equals(this.freezeState, that.freezeState));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(freezeState);
    }

    @Override
    public String toString() {
        return "VideoFeedbackMessage[freezeState=" + freezeState + ']';
    }
}
