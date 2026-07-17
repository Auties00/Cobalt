package com.github.auties00.cobalt.calls.media.video.jitter;

import com.github.auties00.cobalt.calls.util.RttEstimator;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Computes the render time of each video frame from the jitter estimate plus decode, render, and round
 * trip margins, and drives the audio and video synchronization correction.
 *
 * <p>The controller turns a frame's capture timestamp into the local clock instant the renderer should
 * display it. The target playout delay is the {@link VideoJitterEstimator} jitter allowance, plus the
 * measured decode time, plus a fixed render delay, plus a bounded fraction of the round trip time, all
 * clamped to a configured minimum and maximum. {@link #renderTimeMs(long, long, int)} adds that delay to
 * the frame's capture timestamp mapped into the local clock, so the {@link VideoJitterBuffer} can hold a
 * frame until its render instant. {@link #onFrameDecoded(long, long)} folds the observed decode duration
 * into the decode time estimate, and {@link #onRttSample(double, long)} folds a round trip sample into the
 * round trip estimator.
 *
 * <p>{@link #updateAvSync(long, long, AvSyncFeedbackSink, long)} measures the relative delay between the
 * video frame about to render and the audio sample playing at the same local clock, and when that offset
 * exceeds the tolerance emits a weighted, bounded {@link AvSyncFeedback} through the supplied sink so
 * audio slides back into sync with video. The controller holds no clock and starts no thread; the caller
 * supplies every time reading so the timing math stays deterministic and unit testable. Instances are not
 * thread safe; the single video receive thread that owns one drives all updates and reads.
 *
 * @implNote This implementation computes the target delay as
 * {@code jitter + decode + render + RTT * RTT_MULTIPLIER} clamped to
 * {@code [MIN_PLAYOUT_DELAY_MS, MAX_PLAYOUT_DELAY_MS]}. The numeric constants are WhatsApp's compiled in
 * defaults, applied when the server pushed {@code voip_settings} omits the corresponding key. The decode
 * time and round trip estimates reuse the shared {@link RttEstimator} exponential smoothing, and capture
 * timestamps are converted to milliseconds through {@link #RTP_CLOCK_HZ}.
 */
public final class VideoTimingController {
    /**
     * The video RTP timestamp clock rate, in hertz.
     *
     * <p>Video RTP timestamps advance at ninety kilohertz; capture timestamps are converted to
     * milliseconds by dividing the timestamp delta by ninety.
     */
    public static final long RTP_CLOCK_HZ = 90_000;

    /**
     * The fraction of the round trip time added to the target playout delay.
     *
     * <p>A retransmitted frame arrives roughly one round trip late, so a fraction of the round trip time
     * is reserved to let a NACK round complete before the frame is needed.
     *
     * @implNote This implementation uses {@code 0.9}, WhatsApp's default {@code wavtc_rtt_multiplier}
     * applied when the server pushed {@code voip_settings} omits the key. WebRTC upstream reserves a full
     * round trip ({@code 1.0}); WhatsApp reserves this smaller fraction.
     */
    private static final double RTT_MULTIPLIER = 0.9;

    /**
     * The cap, in milliseconds, on the round trip time contribution to the target delay.
     *
     * <p>Bounds the retransmission margin so a very high round trip time does not push the playout delay
     * past the maximum.
     *
     * @implNote This implementation uses {@code 200}, WhatsApp's default {@code wavtc_rtt_mult_add_cap_ms}
     * applied when the server pushed {@code voip_settings} omits the key.
     */
    private static final double RTT_MULT_ADD_CAP_MS = 200.0;

    /**
     * The fixed render delay added to the target playout delay, in milliseconds.
     *
     * <p>Models the time between handing a frame to the renderer and the frame appearing on screen.
     *
     * @implNote This implementation uses {@code 10}, WhatsApp's default {@code wavtc_render_delay_ms}
     * applied when the server pushed {@code voip_settings} omits the key.
     */
    private static final double RENDER_DELAY_MS = 10.0;

    /**
     * The lower bound on the target playout delay, in milliseconds.
     *
     * <p>The minimum playout floor; WhatsApp adds no override, so the floor stays at zero.
     *
     * @implNote This implementation uses {@code 0}, the minimum playout delay the timing module resets to
     * on a bad render timing.
     */
    private static final double MIN_PLAYOUT_DELAY_MS = 0.0;

    /**
     * The upper bound on the target playout delay, in milliseconds.
     *
     * <p>The maximum video delay ceiling; beyond it a frame is rendered late rather than held further.
     *
     * @implNote This implementation uses {@code 10000}, the maximum video delay ceiling the timing module
     * restores on reset; WhatsApp adds no override.
     */
    private static final double MAX_PLAYOUT_DELAY_MS = 10_000.0;

    /**
     * The smoothing factor applied to decode time and round trip time samples.
     *
     * <p>A small factor so the decode and round trip estimates track the running average rather than any
     * single sample.
     */
    private static final double SMOOTHING_ALPHA = 0.1;

    /**
     * The weight applied to the measured relative delay when computing an A/V sync correction.
     *
     * <p>Only a fraction of the measured offset is corrected per round so audio slides into sync
     * gradually rather than stepping audibly.
     *
     * @implNote This implementation uses {@code 0.25}, WhatsApp's default
     * {@code avsync_feedback_to_audio_weight} applied when the server pushed {@code voip_settings} omits
     * the key, bounded to {@code [0.0, 1.0]}.
     */
    private static final double AV_SYNC_WEIGHT = 0.25;

    /**
     * The maximum audio playout delay correction applied per A/V sync round, in milliseconds.
     *
     * <p>Clamps the per round correction so a large measured offset is corrected over several rounds.
     *
     * @implNote This implementation uses {@code 500}, WhatsApp's default
     * {@code avsync_feedback_to_audio_max_threshold_ms} applied when the server pushed
     * {@code voip_settings} omits the key.
     */
    private static final double MAX_AUDIO_SYNC_DELAY_MS = 500.0;

    /**
     * The relative delay tolerance below which no A/V sync correction is emitted, in milliseconds.
     *
     * <p>Within this band the streams are considered in sync and the correction is zero.
     *
     * @implNote This implementation uses {@code 100}, WhatsApp's default
     * {@code avsync_feedback_to_audio_min_threshold_ms} applied when the server pushed
     * {@code voip_settings} omits the key.
     */
    private static final double AV_SYNC_TOLERANCE_MS = 100.0;

    /**
     * The logger for {@link VideoTimingController}.
     */
    private static final System.Logger LOGGER = Log.get(VideoTimingController.class);

    /**
     * The jitter estimator whose allowance feeds the target playout delay.
     */
    private final VideoJitterEstimator jitterEstimator;

    /**
     * The round trip time estimator feeding the retransmission margin.
     */
    private final RttEstimator rttEstimator;

    /**
     * The exponentially averaged decode time, in milliseconds.
     */
    private double decodeTimeMs;

    /**
     * The mapping offset, in milliseconds, from the frame capture clock to the local clock.
     *
     * <p>Captured on the first frame as the local arrival time minus the capture timestamp in
     * milliseconds, so a later frame's render time is computed in the local clock.
     */
    private double captureToLocalOffsetMs;

    /**
     * Whether the capture to local clock offset has been seeded from a first frame.
     */
    private boolean clockMapped;

    /**
     * Constructs a timing controller over the given jitter estimator.
     *
     * <p>The decode time estimate starts at zero and the round trip estimator uninitialized; the clock
     * mapping is seeded by the first {@link #renderTimeMs(long, long, int)}.
     *
     * @param jitterEstimator the jitter estimator whose allowance feeds the target delay; never
     *                        {@code null}
     */
    public VideoTimingController(VideoJitterEstimator jitterEstimator) {
        this.jitterEstimator = jitterEstimator;
        this.rttEstimator = new RttEstimator();
        this.decodeTimeMs = 0.0;
        this.captureToLocalOffsetMs = 0.0;
        this.clockMapped = false;
    }

    /**
     * Returns the local clock instant, in milliseconds, at which a frame should be rendered.
     *
     * <p>Seeds the capture to local clock mapping from the first frame, then maps the frame's capture
     * timestamp into the local clock and adds the target playout delay. The frame should be held until the
     * returned instant.
     *
     * @param captureRtpTimestamp the frame's capture RTP timestamp
     * @param nowMs               the local arrival time of the frame in milliseconds, from a monotonic
     *                            source
     * @param pendingNackCount    the number of frames currently awaiting retransmission, gating the round
     *                            trip margin
     * @return the local clock render instant in milliseconds
     */
    public long renderTimeMs(long captureRtpTimestamp, long nowMs, int pendingNackCount) {
        return renderTimeMs(captureRtpTimestamp, nowMs, targetDelayPrefixMs(), pendingNackCount);
    }

    /**
     * Returns the local clock render instant, in milliseconds, using a precomputed target delay prefix.
     *
     * <p>Seeds the capture to local clock mapping from the first frame, maps the frame's capture timestamp
     * into the local clock, and adds the target playout delay formed from {@code delayPrefixMs} plus the size
     * dependent {@linkplain #rttMarginMs(int) round trip margin}, clamped to the playout bounds. The
     * {@code delayPrefixMs} is {@link #targetDelayPrefixMs()}, hoisted out of a poll's per frame loop so it is
     * computed once per poll rather than once per frame.
     *
     * @param captureRtpTimestamp the frame's capture RTP timestamp
     * @param nowMs               the local arrival time of the frame in milliseconds, from a monotonic
     *                            source
     * @param delayPrefixMs       the poll invariant {@link #targetDelayPrefixMs()} prefix
     * @param pendingNackCount    the number of frames currently awaiting retransmission, gating the round
     *                            trip margin
     * @return the local clock render instant in milliseconds
     */
    public long renderTimeMs(long captureRtpTimestamp, long nowMs, double delayPrefixMs, int pendingNackCount) {
        var captureMs = captureRtpTimestamp * 1000.0 / RTP_CLOCK_HZ;
        if (!clockMapped) {
            captureToLocalOffsetMs = nowMs - captureMs;
            clockMapped = true;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "video timing controller: clock mapping seeded, offsetMs={0}", captureToLocalOffsetMs);
            }
        }
        var localCaptureMs = captureMs + captureToLocalOffsetMs;
        var target = clamp(delayPrefixMs + rttMarginMs(pendingNackCount), MIN_PLAYOUT_DELAY_MS, MAX_PLAYOUT_DELAY_MS);
        var renderTimeMs = Math.round(localCaptureMs + target);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "video timing controller: renderTimeMs={0} pendingNack={1}", renderTimeMs, pendingNackCount);
        }
        return renderTimeMs;
    }

    /**
     * Returns the poll invariant prefix of the target playout delay, in milliseconds.
     *
     * <p>The jitter allowance plus the decode time estimate plus the fixed render delay, without the size
     * dependent round trip margin and before the playout clamp. It does not change while a single
     * {@link VideoJitterBuffer} poll walks its frames, so the caller computes it once per poll and passes it
     * to {@link #renderTimeMs(long, long, double, int)} for each frame rather than recomputing it per frame.
     *
     * @return the jitter, decode, and render delay prefix in milliseconds
     */
    public double targetDelayPrefixMs() {
        return jitterEstimator.jitterEstimateMs() + decodeTimeMs + RENDER_DELAY_MS;
    }

    /**
     * Returns the bounded round trip retransmission margin, in milliseconds, for a pending NACK count.
     *
     * <p>The smoothed round trip estimate scaled by {@link #RTT_MULTIPLIER} and capped at
     * {@link #RTT_MULT_ADD_CAP_MS}, or {@code 0} when the pending NACK count exceeds the jitter estimator's
     * limit. This is the size dependent term the invariant {@link #targetDelayPrefixMs()} omits.
     *
     * @param pendingNackCount the number of frames currently awaiting retransmission
     * @return the round trip margin in milliseconds
     */
    private double rttMarginMs(int pendingNackCount) {
        return jitterEstimator.frameNackCount(pendingNackCount)
                ? Math.min(rttEstimator.estimate() * RTT_MULTIPLIER, RTT_MULT_ADD_CAP_MS)
                : 0.0;
    }

    /**
     * Returns the current target playout delay, in milliseconds.
     *
     * <p>The {@linkplain #targetDelayPrefixMs() invariant prefix} plus the bounded
     * {@linkplain #rttMarginMs(int) round trip margin}, clamped to {@link #MIN_PLAYOUT_DELAY_MS} and
     * {@link #MAX_PLAYOUT_DELAY_MS}.
     *
     * @param pendingNackCount the number of frames currently awaiting retransmission
     * @return the clamped target playout delay in milliseconds
     */
    public double targetDelayMs(int pendingNackCount) {
        return clamp(targetDelayPrefixMs() + rttMarginMs(pendingNackCount),
                MIN_PLAYOUT_DELAY_MS, MAX_PLAYOUT_DELAY_MS);
    }

    /**
     * Folds an observed decode duration into the decode time estimate.
     *
     * <p>Updates the exponential average of the decode time so the target delay reserves enough time to
     * decode the frame before its render instant.
     *
     * @param decodeDurationMs the wall clock duration the decoder took for the frame, in milliseconds;
     *                         ignored when negative
     * @param nowMs            the current time in milliseconds, from a monotonic source
     */
    public void onFrameDecoded(long decodeDurationMs, long nowMs) {
        if (decodeDurationMs < 0) {
            return;
        }
        if (decodeTimeMs == 0.0) {
            decodeTimeMs = decodeDurationMs;
        } else {
            decodeTimeMs = (1.0 - SMOOTHING_ALPHA) * decodeTimeMs + SMOOTHING_ALPHA * decodeDurationMs;
        }
    }

    /**
     * Folds a round trip time sample into the round trip estimator.
     *
     * <p>Updates the smoothed round trip estimate the retransmission margin reads.
     *
     * @param rttMs the round trip time measurement in milliseconds; ignored when not strictly positive
     * @param nowMs the current time in milliseconds, from a monotonic source
     */
    public void onRttSample(double rttMs, long nowMs) {
        rttEstimator.update(rttMs, SMOOTHING_ALPHA);
    }

    /**
     * Measures audio and video relative delay and emits a bounded synchronization correction.
     *
     * <p>Computes the offset between the video frame about to render and the audio sample playing at the
     * same local clock as the difference of their playout instants. When the offset is within
     * {@link #AV_SYNC_TOLERANCE_MS} a zero correction is emitted as a heartbeat; otherwise the offset is
     * weighted by {@link #AV_SYNC_WEIGHT} and clamped to {@link #MAX_AUDIO_SYNC_DELAY_MS}, and the
     * resulting {@link AvSyncFeedback} is delivered through the sink.
     *
     * @param videoRenderMs  the local clock render instant of the next video frame, in milliseconds
     * @param audioPlayoutMs the local clock playout instant of the audio sample currently playing, in
     *                       milliseconds
     * @param sink           the sink that ingests the correction into the audio jitter buffer; never
     *                       {@code null}
     * @param nowMs          the current time in milliseconds, from a monotonic source
     * @return the feedback emitted, carrying the measured relative delay and the bounded correction
     */
    public AvSyncFeedback updateAvSync(long videoRenderMs, long audioPlayoutMs, AvSyncFeedbackSink sink, long nowMs) {
        var feedback = computeAvSync(videoRenderMs, audioPlayoutMs);
        sink.applyAvSyncFeedback(feedback);
        if (feedback.correctionMs() != 0.0 && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "video timing controller: av sync correction, relativeDelayMs={0} correctionMs={1}",
                    feedback.relativeDelayMs(), feedback.correctionMs());
        }
        return feedback;
    }

    /**
     * Computes the bounded audio and video synchronization correction without delivering it.
     *
     * <p>Measures the offset between the video frame about to render and the audio sample playing at the same
     * local clock as the difference of their playout instants. When the offset is within
     * {@link #AV_SYNC_TOLERANCE_MS} the correction is zero (a heartbeat); otherwise it is weighted by
     * {@link #AV_SYNC_WEIGHT} and clamped to {@link #MAX_AUDIO_SYNC_DELAY_MS}. The caller delivers the
     * returned feedback to the audio buffer; {@link VideoJitterBuffer} computes it under its lock and pushes
     * it after the frame is released, so the audio sink is not notified while the video lock is held.
     *
     * @param videoRenderMs  the local clock render instant of the next video frame, in milliseconds
     * @param audioPlayoutMs the local clock playout instant of the audio sample currently playing, in
     *                       milliseconds
     * @return the feedback carrying the measured relative delay and the bounded correction
     */
    public AvSyncFeedback computeAvSync(long videoRenderMs, long audioPlayoutMs) {
        var relativeDelayMs = (double) (videoRenderMs - audioPlayoutMs);
        double correctionMs;
        if (Math.abs(relativeDelayMs) < AV_SYNC_TOLERANCE_MS) {
            correctionMs = 0.0;
        } else {
            correctionMs = clamp(relativeDelayMs * AV_SYNC_WEIGHT, -MAX_AUDIO_SYNC_DELAY_MS, MAX_AUDIO_SYNC_DELAY_MS);
        }
        return new AvSyncFeedback(relativeDelayMs, correctionMs);
    }

    /**
     * Returns the current decode time estimate, in milliseconds.
     *
     * @return the smoothed decode time
     */
    public double decodeTimeMs() {
        return decodeTimeMs;
    }

    /**
     * Returns the current round trip time estimate, in milliseconds.
     *
     * @return the smoothed round trip estimate, or {@code 0} before any sample
     */
    public long rttMs() {
        return rttEstimator.estimate();
    }

    /**
     * Clears the decode time estimate and the clock mapping, returning the controller to its start state.
     *
     * <p>Used when the jitter buffer resets after a bad render timing so the next frame reseeds the clock
     * mapping.
     */
    public void reset() {
        decodeTimeMs = 0.0;
        captureToLocalOffsetMs = 0.0;
        clockMapped = false;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "video timing controller: reset");
    }

    /**
     * Clamps a value into the inclusive range.
     *
     * @param value the value to clamp
     * @param min   the lower bound
     * @param max   the upper bound
     * @return the value confined to {@code [min, max]}
     */
    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
