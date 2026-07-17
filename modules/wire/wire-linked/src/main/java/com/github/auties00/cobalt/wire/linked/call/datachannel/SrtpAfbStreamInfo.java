package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * One stream's entry in the SRTP application-layer feedback (AFB) report.
 *
 * <p>Carries the per-stream synchronization watermark the receiver uses
 * to validate that its SRTP replay window matches the sender's: an SSRC,
 * the highest RTCP packet index seen, and the highest RTP packet index
 * seen. The {@linkplain #isValid() is_valid} flag is the sender's
 * affirmation that the SSRC was active during the report window.
 */
@ProtobufMessage(name = "SrtpAfbStreamInfo")
public final class SrtpAfbStreamInfo {
    /**
     * Whether the reporting endpoint considered this stream active.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final Boolean isValid;

    /**
     * The synchronization-source identifier of the stream.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final Integer ssrc;

    /**
     * The highest RTCP packet index observed for this stream.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    final Integer rtcpIndex;

    /**
     * The highest RTP packet index observed for this stream.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    final Long rtpIndex;

    /**
     * Constructs a new {@code SrtpAfbStreamInfo}.
     *
     * @param isValid   whether the stream was active
     * @param ssrc      the stream's SSRC
     * @param rtcpIndex the highest RTCP index seen
     * @param rtpIndex  the highest RTP index seen
     */
    SrtpAfbStreamInfo(Boolean isValid, Integer ssrc, Integer rtcpIndex, Long rtpIndex) {
        this.isValid = isValid;
        this.ssrc = ssrc;
        this.rtcpIndex = rtcpIndex;
        this.rtpIndex = rtpIndex;
    }

    /**
     * Returns whether this stream was reported as active.
     *
     * @return {@code true} when the {@code is_valid} flag was set
     */
    public boolean isValid() {
        return isValid != null && isValid;
    }

    /**
     * Returns the stream's synchronization-source identifier.
     *
     * @return an {@link OptionalInt} with the SSRC, or empty
     */
    public OptionalInt ssrc() {
        return ssrc == null ? OptionalInt.empty() : OptionalInt.of(ssrc);
    }

    /**
     * Returns the highest RTCP packet index observed.
     *
     * @return an {@link OptionalInt} with the index, or empty
     */
    public OptionalInt rtcpIndex() {
        return rtcpIndex == null ? OptionalInt.empty() : OptionalInt.of(rtcpIndex);
    }

    /**
     * Returns the highest RTP packet index observed.
     *
     * @return an {@link OptionalLong} with the index, or empty
     */
    public OptionalLong rtpIndex() {
        return rtpIndex == null ? OptionalLong.empty() : OptionalLong.of(rtpIndex);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof SrtpAfbStreamInfo that
                && Objects.equals(this.isValid, that.isValid)
                && Objects.equals(this.ssrc, that.ssrc)
                && Objects.equals(this.rtcpIndex, that.rtcpIndex)
                && Objects.equals(this.rtpIndex, that.rtpIndex));
    }

    @Override
    public int hashCode() {
        return Objects.hash(isValid, ssrc, rtcpIndex, rtpIndex);
    }

    @Override
    public String toString() {
        return "SrtpAfbStreamInfo[isValid=" + isValid + ", ssrc=" + ssrc
                + ", rtcpIndex=" + rtcpIndex + ", rtpIndex=" + rtpIndex + ']';
    }
}
