package com.github.auties00.cobalt.calls.transport.congestion.ratecontrol;

import com.github.auties00.cobalt.calls.media.video.codec.VideoCodecParams;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Turns the combined bandwidth estimate target into the video encoder's target bitrate and forward
 * error correction ratios, clamping the rate when the SCTP send buffer is congested.
 *
 * <p>Each {@link #apply(long, double, long, long, VideoCodecParams)} round takes the combiner's target
 * bitrate for the video stream, clamps it against the {@link SctpBufferCongestionController} so a filling
 * data channel buffer backs the encoder off, and then funds forward error correction from the remaining
 * budget in proportion to the measured loss. The forward error correction ratio is split between key
 * frames and delta frames, with key frames protected more heavily because a lost key frame freezes the
 * stream until the next one. The method returns an updated {@link VideoCodecParams} carrying the new
 * target bitrate clamped to the codec's configured floor and ceiling, alongside the key frame and delta
 * frame protection ratios the packetizer applies.
 *
 * <p>The controller holds the SCTP buffer congestion controller across rounds and is otherwise
 * stateless. It holds no clock; the caller supplies a current time reading each round so the buffer
 * congestion feedback recency is computed correctly. Instances are not thread safe; the single rate
 * control thread that owns one drives all rounds.
 *
 * @implNote This implementation clamps the combiner output twice, first to the codec bitrate window
 * reported by the supplied {@link VideoCodecParams} and then through the {@link SctpBufferCongestionController},
 * so a congested data channel buffer takes precedence over the raw bandwidth estimate. The key frame and
 * delta frame protection ratios are funded from the remaining budget in proportion to the measured loss,
 * bounded by {@link #MAX_KEY_FEC_RATIO} and {@link #MAX_DELTA_FEC_RATIO}.
 */
public final class VideoRateController {
    /**
     * The logger for {@link VideoRateController}.
     */
    private static final System.Logger LOGGER = Log.get(VideoRateController.class);

    /**
     * The maximum forward error correction ratio applied to key frames, in {@code [0, 1]}.
     *
     * <p>A key frame may be protected with up to this fraction of redundant data because losing one
     * freezes the stream until the next key frame.
     *
     * @implNote This implementation uses the compiled default of half rate: the server enables key frame
     * protection but does not push an explicit ceiling for it, so this bound governs how much redundancy a
     * lossy round can add to a key frame.
     */
    private static final double MAX_KEY_FEC_RATIO = 0.5;

    /**
     * The maximum forward error correction ratio applied to delta frames, in {@code [0, 1]}.
     *
     * <p>Delta frames are protected more lightly than key frames since a lost delta frame degrades only
     * until the next frame. The ceiling is zero, so loss driven delta frame protection is disabled and
     * only key frames carry redundancy.
     *
     * @implNote This implementation uses the value the server pushes for the general (non key) frame
     * ratio bound, which is zero, disabling delta frame forward error correction entirely.
     */
    private static final double MAX_DELTA_FEC_RATIO = 0.0;

    /**
     * The SCTP send buffer congestion controller that clamps the video target when the buffer fills.
     */
    private final SctpBufferCongestionController sctpBufferController;

    /**
     * Constructs a video rate controller over the given SCTP buffer congestion controller.
     *
     * @param sctpBufferController the SCTP buffer congestion controller; never {@code null}
     */
    public VideoRateController(SctpBufferCongestionController sctpBufferController) {
        this.sctpBufferController = sctpBufferController;
    }

    /**
     * Constructs a video rate controller with a default configured SCTP buffer congestion controller.
     */
    public VideoRateController() {
        this(SctpBufferCongestionController.defaults());
    }

    /**
     * Computes the video encoder settings for the combined target and the latest network and buffer
     * measurements.
     *
     * <p>Clamps the target to the codec bitrate window, advances the SCTP buffer congestion controller
     * with the current occupancy and the last feedback time and applies its clamp, then funds the key
     * frame and delta frame forward error correction ratios from the measured loss. Returns the supplied
     * parameters with the new target bitrate applied and clamped, alongside the protection ratios.
     *
     * @param combinedTargetBps   the combiner's target bitrate for the video stream, in bits per second
     * @param plr                 the measured packet loss ratio over the recent window, in {@code [0, 1]}
     * @param sctpBufferOccupancy the current SCTP send buffer occupancy in bytes
     * @param lastFeedbackMs      the time of the most recent feedback in milliseconds, from a monotonic
     *                            source
     * @param params              the current codec parameters to re target; never {@code null}
     * @return the video rate result carrying the updated parameters and the protection ratios
     */
    public VideoRateResult apply(long combinedTargetBps, double plr, long sctpBufferOccupancy,
                                 long lastFeedbackMs, VideoCodecParams params) {
        var nowMs = System.nanoTime() / 1_000_000L;
        var windowClamped = Math.clamp(combinedTargetBps, params.minBitrate(), params.maxBitrate());

        sctpBufferController.update(sctpBufferOccupancy, lastFeedbackMs, nowMs);
        var bufferClamped = sctpBufferController.clampRate(windowClamped);
        var targetBps = Math.clamp(bufferClamped, params.minBitrate(), params.maxBitrate());

        var keyFecRatio = Math.clamp(plr * 2.0, 0.0, MAX_KEY_FEC_RATIO);
        var deltaFecRatio = Math.clamp(plr, 0.0, MAX_DELTA_FEC_RATIO);

        var newTarget = (int) targetBps;
        var updated = newTarget == params.targetBitrate() ? params : params.withTargetBitrate(newTarget);
        var congested = sctpBufferController.isCongested();
        if (congested && Log.WARNING) {
            LOGGER.log(Level.WARNING, "video rate clamped: sctp send buffer congested, target={0}bps", newTarget);
        } else if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "video rate applied: target={0}bps keyFec={1} deltaFec={2}",
                    newTarget, keyFecRatio, deltaFecRatio);
        }
        return new VideoRateResult(updated, keyFecRatio, deltaFecRatio, congested);
    }

    /**
     * Returns whether the SCTP send buffer is currently congested.
     *
     * @return {@code true} when the buffer controller reports congestion
     */
    public boolean sctpBufferCongested() {
        return sctpBufferController.isCongested();
    }

    /**
     * The outcome of one video rate control round: the re targeted codec parameters, the forward error
     * correction ratios, and whether the SCTP buffer is congested.
     *
     * <p>The {@link #params()} are ready to hand to a codec reconfigure; the {@link #keyFecRatio()} and
     * {@link #deltaFecRatio()} drive the packetizer's redundancy, and {@link #sctpBufferCongested()} is
     * surfaced for telemetry.
     *
     * @param params              the updated codec parameters with the new target bitrate; never
     *                            {@code null}
     * @param keyFecRatio         the forward error correction ratio for key frames, in {@code [0, 1]}
     * @param deltaFecRatio       the forward error correction ratio for delta frames, in {@code [0, 1]}
     * @param sctpBufferCongested whether the SCTP send buffer was congested this round
     */
    public record VideoRateResult(VideoCodecParams params, double keyFecRatio, double deltaFecRatio,
                                  boolean sctpBufferCongested) {
    }
}
