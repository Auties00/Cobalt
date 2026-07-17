package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Periodic peer-to-peer feedback summary exchanged during a call.
 *
 * <p>A {@code PeerFeedback} bundles four optional sub-reports:
 * {@linkplain #videoSenderStats() sender-side video stats},
 * {@linkplain #videoFbMsg() receiver-side video feedback},
 * {@linkplain #aqsStats() audio quality stats}, and a single
 * {@linkplain #noiseSuppressionUiStatus() noise-suppression UI flag}
 * communicating whether the peer is showing a noise-suppression status
 * indicator. The runtime publishes one feedback message every few
 * seconds on the AppData stream so both ends keep their adaptation loops
 * in sync.
 */
@ProtobufMessage(name = "PeerFeedback")
public final class PeerFeedback {
    /**
     * The sender-side video pipeline metrics.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final VideoSenderStats videoSenderStats;

    /**
     * The receiver-side video feedback message.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final VideoFeedbackMessage videoFbMsg;

    /**
     * The audio quality stats sub-report.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final AqsStats aqsStats;

    /**
     * Whether the peer is currently showing a noise-suppression UI indicator.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final Boolean noiseSuppressionUiStatus;

    /**
     * Constructs a new {@code PeerFeedback}.
     *
     * @param videoSenderStats         the sender-side video metrics
     * @param videoFbMsg               the receiver-side video feedback
     * @param aqsStats                 the audio quality stats
     * @param noiseSuppressionUiStatus the noise-suppression UI flag
     */
    PeerFeedback(VideoSenderStats videoSenderStats,
                 VideoFeedbackMessage videoFbMsg,
                 AqsStats aqsStats,
                 Boolean noiseSuppressionUiStatus) {
        this.videoSenderStats = videoSenderStats;
        this.videoFbMsg = videoFbMsg;
        this.aqsStats = aqsStats;
        this.noiseSuppressionUiStatus = noiseSuppressionUiStatus;
    }

    /**
     * Returns the sender-side video pipeline metrics.
     *
     * @return an {@link Optional} with the metrics, or empty
     */
    public Optional<VideoSenderStats> videoSenderStats() {
        return Optional.ofNullable(videoSenderStats);
    }

    /**
     * Returns the receiver-side video feedback.
     *
     * @return an {@link Optional} with the feedback, or empty
     */
    public Optional<VideoFeedbackMessage> videoFbMsg() {
        return Optional.ofNullable(videoFbMsg);
    }

    /**
     * Returns the audio quality stats.
     *
     * @return an {@link Optional} with the stats, or empty
     */
    public Optional<AqsStats> aqsStats() {
        return Optional.ofNullable(aqsStats);
    }

    /**
     * Returns whether the peer is showing a noise-suppression UI indicator.
     *
     * @return {@code true} when the flag was set
     */
    public boolean noiseSuppressionUiStatus() {
        return noiseSuppressionUiStatus != null && noiseSuppressionUiStatus;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof PeerFeedback that
                && Objects.equals(this.videoSenderStats, that.videoSenderStats)
                && Objects.equals(this.videoFbMsg, that.videoFbMsg)
                && Objects.equals(this.aqsStats, that.aqsStats)
                && Objects.equals(this.noiseSuppressionUiStatus, that.noiseSuppressionUiStatus));
    }

    @Override
    public int hashCode() {
        return Objects.hash(videoSenderStats, videoFbMsg, aqsStats, noiseSuppressionUiStatus);
    }

    @Override
    public String toString() {
        return "PeerFeedback[videoSenderStats=" + videoSenderStats
                + ", videoFbMsg=" + videoFbMsg
                + ", aqsStats=" + aqsStats
                + ", noiseSuppressionUiStatus=" + noiseSuppressionUiStatus + ']';
    }
}
