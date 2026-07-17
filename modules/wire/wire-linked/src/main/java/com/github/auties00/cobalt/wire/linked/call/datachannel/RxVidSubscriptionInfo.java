package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * One participant-stream subscription entry inside an {@link RxSubscriptions}.
 *
 * <p>Identifies the publishing participant by {@linkplain #pid() PID} and
 * the requested {@linkplain #vidQuality() quality level} the subscriber
 * wants to receive. The SFU honours the highest-quality subscription that
 * fits the per-receiver bandwidth budget.
 */
@ProtobufMessage(name = "RxVidSubscriptionInfo")
public final class RxVidSubscriptionInfo {
    /**
     * The publishing participant's PID.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer pid;

    /**
     * The requested receive-side video quality.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final VideoQuality vidQuality;

    /**
     * Constructs a new {@code RxVidSubscriptionInfo}.
     *
     * @param pid        the publishing participant's PID
     * @param vidQuality the requested quality level
     */
    RxVidSubscriptionInfo(Integer pid, VideoQuality vidQuality) {
        this.pid = pid;
        this.vidQuality = vidQuality;
    }

    /**
     * Returns the publishing participant's PID.
     *
     * @return an {@link OptionalInt} with the PID, or empty
     */
    public OptionalInt pid() {
        return pid == null ? OptionalInt.empty() : OptionalInt.of(pid);
    }

    /**
     * Returns the requested receive-side video quality.
     *
     * @return an {@link Optional} with the quality, or empty
     */
    public Optional<VideoQuality> vidQuality() {
        return Optional.ofNullable(vidQuality);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RxVidSubscriptionInfo that
                && Objects.equals(this.pid, that.pid)
                && this.vidQuality == that.vidQuality);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid, vidQuality);
    }

    @Override
    public String toString() {
        return "RxVidSubscriptionInfo[pid=" + pid + ", vidQuality=" + vidQuality + ']';
    }

    /**
     * The five quality levels a subscriber may request for a participant
     * video stream.
     *
     * <p>The SFU is free to deliver a lower level than requested when the
     * per-receiver bandwidth budget cannot accommodate the requested one.
     */
    @ProtobufEnum(name = "RxVidSubscriptionInfo.VideoQuality")
    public enum VideoQuality {
        /**
         * Implementation-defined default quality.
         */
        DEFAULT(0),

        /**
         * Low quality.
         */
        LOW(1),

        /**
         * Medium quality.
         */
        MEDIUM(2),

        /**
         * High quality.
         */
        HIGH(3),

        /**
         * High-definition quality.
         */
        HD(4);

        /**
         * The protobuf wire index of this quality level.
         */
        final int index;

        VideoQuality(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire index of this quality level.
         *
         * @return the index
         */
        public int index() {
            return index;
        }
    }
}
