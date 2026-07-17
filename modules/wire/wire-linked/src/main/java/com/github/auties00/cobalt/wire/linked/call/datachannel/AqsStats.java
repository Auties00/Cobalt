package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Audio Quality Score reported in a peer-feedback packet.
 *
 * <p>WhatsApp's in-call audio runtime computes a periodic audio quality
 * score (AQS) and ships it inside a {@link PeerFeedback} on the call's
 * AppData stream. The score is an opaque integer whose scale is fixed by
 * the runtime; consumers usually compare values rather than interpret them
 * absolutely.
 */
@ProtobufMessage(name = "AQSStats")
public final class AqsStats {
    /**
     * The reported audio quality score.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final Integer aqs;

    /**
     * Constructs a new {@code AqsStats}.
     *
     * @param aqs the audio quality score, or {@code null} when absent
     */
    AqsStats(Integer aqs) {
        this.aqs = aqs;
    }

    /**
     * Returns the audio quality score.
     *
     * @return an {@link OptionalInt} carrying the score, or empty when
     *         the peer did not report one
     */
    public OptionalInt aqs() {
        return aqs == null ? OptionalInt.empty() : OptionalInt.of(aqs);
    }

    /**
     * Compares this {@code AqsStats} with another for equality.
     *
     * @param obj the object to compare to
     * @return {@code true} when {@code obj} is an {@code AqsStats}
     *         carrying the same score
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this
                || (obj instanceof AqsStats that && Objects.equals(this.aqs, that.aqs));
    }

    /**
     * Returns the hash code of this {@code AqsStats}.
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(aqs);
    }

    /**
     * Returns a debug-friendly representation.
     *
     * @return a string carrying the score
     */
    @Override
    public String toString() {
        return "AqsStats[aqs=" + aqs + ']';
    }
}
