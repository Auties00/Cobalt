package com.github.auties00.cobalt.calls.media.video.jitter;

import com.github.auties00.cobalt.calls.media.video.codec.EncodedVideoFrame;
import com.github.auties00.cobalt.calls.util.RateCalculator;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Buffers received video frames and releases each at its scheduled render time so the renderer sees a
 * smooth, jitter absorbed stream.
 *
 * <p>The jitter buffer is the video counterpart of the audio {@code NetEq}: received frames arrive out
 * of order and with variable delay, and the buffer holds them until the {@link VideoTimingController}
 * says each is due. {@link #insert(EncodedVideoFrame, long, long, int)} adds a received frame with its
 * arrival time, capture timestamp, and sequence number, feeding the jitter estimate. {@link #poll(long)}
 * is called once per render tick and returns the frame due now, or {@code null} when the next frame is
 * not yet due. The buffer reconciles the released frame's render time with audio through the
 * {@link AvSyncFeedbackSink} supplied at construction so lip sync holds.
 *
 * <p>Frames are held in a map ordered by capture timestamp; {@link #insert(EncodedVideoFrame, long, long,
 * int)} computes the interframe delay against the previously inserted frame and feeds it with the frame
 * size to the {@link VideoJitterEstimator}, then records the sequence gap for the loss estimate.
 * {@link #poll(long)} releases the earliest frame whose render time has arrived, dropping a frame whose
 * render time has fallen more than the maximum video delay into the past and resetting the estimator and
 * timing controller when render timing collapses.
 *
 * <p>Insertion runs on the transport receive thread and polling on the render thread; a single
 * {@link ReentrantLock} serialises the two so the ordered map, the estimator, and the loss counters stay
 * consistent. The lock is held only for the bounded map and arithmetic work of one insert or one poll,
 * never across a blocking call, so neither thread stalls the other.
 *
 * @implNote This implementation computes each frame's render time from the timing controller plus the
 * jitter estimate, reconciles it against audio for lip sync, drops a frame whose render time has fallen
 * more than {@link #MAX_VIDEO_DELAY_MS} into the past, and resets the jitter estimator and timing
 * controller when render timing becomes implausible. The {@link TreeMap} keyed by capture timestamp
 * stands in for a native frame buffer. The loss ratio reuses two {@link RateCalculator} sliding windows,
 * one counting lost frames and one counting received frames, whose ratio is the receiver packet loss
 * ratio fed back to the sender's rate control.
 */
public final class VideoJitterBuffer {
    /**
     * The maximum age, in milliseconds, by which a frame's render time may have passed before the frame
     * is dropped rather than rendered.
     *
     * <p>A frame whose render time has passed by more than this ceiling is discarded so the stream does
     * not stall trying to render stale frames. The bound is {@code 10000} milliseconds.
     */
    public static final long MAX_VIDEO_DELAY_MS = 10_000;

    /**
     * The per bucket duration, in milliseconds, of the loss ratio sliding windows.
     *
     * <p>The {@link RateCalculator} divides each window into ten buckets, so this duration times ten is
     * the loss scan window over which the ratio is measured: ten buckets of {@code 50} milliseconds give
     * the {@code 500} millisecond loss window.
     */
    private static final long LOSS_WINDOW_BUCKET_MS = 50;

    /**
     * The minimum number of filled loss window buckets before the loss ratio is reported.
     *
     * <p>Until this many buckets have accumulated the ratio is considered too sparse and is reported as
     * zero.
     */
    private static final int LOSS_WINDOW_MIN_BUCKETS = 2;

    /**
     * The logger for {@link VideoJitterBuffer}.
     */
    private static final System.Logger LOGGER = Log.get(VideoJitterBuffer.class);

    /**
     * The jitter estimator fed each frame's size and interframe delay.
     */
    private final VideoJitterEstimator jitterEstimator;

    /**
     * The timing controller computing each frame's render time and driving A/V sync.
     */
    private final VideoTimingController timingController;

    /**
     * The sink the released frame's render time is reconciled against the audio playout instant through.
     */
    private final AvSyncFeedbackSink avSyncSink;

    /**
     * Serialises insertion on the transport thread against polling on the render thread.
     */
    private final ReentrantLock lock;

    /**
     * Holds the buffered frames ordered by capture RTP timestamp, earliest first.
     */
    private final TreeMap<Long, Buffered> frames;

    /**
     * The sliding window counting frames received, the denominator of the loss ratio.
     *
     * <p>Cleared in place on {@link #reset()} through {@link RateCalculator#clear()}; a fresh window
     * starts the loss measurement over.
     */
    private final RateCalculator receivedFrames;

    /**
     * The sliding window counting frames detected lost from sequence gaps, the numerator of the loss
     * ratio.
     *
     * <p>Cleared in place on {@link #reset()} alongside {@link #receivedFrames} so the two windows stay
     * aligned over the same scan interval.
     */
    private final RateCalculator lostFrames;

    /**
     * The capture RTP timestamp of the previously inserted frame, for the interframe delay, or
     * {@code -1} before the first insert.
     */
    private long lastInsertCaptureTimestamp;

    /**
     * The local arrival time of the previously inserted frame, for the interframe delay, in
     * milliseconds.
     */
    private long lastInsertArrivalMs;

    /**
     * The RTP sequence number of the highest frame seen, for gap detection, or {@code -1} before the
     * first insert.
     */
    private int highestSequenceNumber;

    /**
     * The capture RTP timestamp of the most recently released frame, below which a later insert is
     * treated as already released and discarded, or {@code -1} before the first release.
     */
    private long lastReleasedCaptureTimestamp;

    /**
     * The audio playout instant, in local clock milliseconds, the next A/V sync reconciliation measures
     * against, or {@code -1} when no audio reference has been supplied.
     *
     * <p>Volatile so the audio playback thread can publish it through {@link #setAudioPlayoutMs(long)}
     * without taking the poll lock; {@link #poll(long)} reads it once into a local snapshot under its lock,
     * so a release still reconciles against a single consistent value.
     */
    private volatile long audioPlayoutMs;

    /**
     * The current local clock time supplied by the render thread, used for the A/V sync measurement, in
     * milliseconds.
     */
    private long currentNowMs;

    /**
     * Constructs a video jitter buffer with the given estimator, timing controller, and A/V sync sink.
     *
     * @param jitterEstimator  the jitter estimator fed each frame; never {@code null}
     * @param timingController  the timing controller computing render times; never {@code null}
     * @param avSyncSink        the sink the audio buffer reconciles against; never {@code null}, pass
     *                          {@link AvSyncFeedbackSink#noop()} to disable lip sync correction
     */
    public VideoJitterBuffer(VideoJitterEstimator jitterEstimator,
                             VideoTimingController timingController,
                             AvSyncFeedbackSink avSyncSink) {
        this.jitterEstimator = Objects.requireNonNull(jitterEstimator, "jitterEstimator cannot be null");
        this.timingController = Objects.requireNonNull(timingController, "timingController cannot be null");
        this.avSyncSink = Objects.requireNonNull(avSyncSink, "avSyncSink cannot be null");
        this.lock = new ReentrantLock();
        this.frames = new TreeMap<>();
        this.receivedFrames = new RateCalculator(LOSS_WINDOW_BUCKET_MS, LOSS_WINDOW_MIN_BUCKETS);
        this.lostFrames = new RateCalculator(LOSS_WINDOW_BUCKET_MS, LOSS_WINDOW_MIN_BUCKETS);
        this.lastInsertCaptureTimestamp = -1;
        this.lastInsertArrivalMs = 0;
        this.highestSequenceNumber = -1;
        this.lastReleasedCaptureTimestamp = -1;
        this.audioPlayoutMs = -1;
        this.currentNowMs = 0;
    }

    /**
     * Inserts a received frame, recording its timing and feeding the jitter estimate.
     *
     * <p>The frame is ordered into the buffer by its capture timestamp; a duplicate of a frame already
     * buffered or already released is discarded. The arrival time and capture timestamp update the jitter
     * estimator, and the sequence number tracks gaps for the retransmission gate.
     *
     * @param frame              the received compressed frame; never {@code null}
     * @param arrivalMs          the local arrival time of the frame in milliseconds, from a monotonic
     *                           source
     * @param captureRtpTimestamp the frame's 90 kHz capture RTP timestamp
     * @param sequenceNumber     the frame's RTP sequence number, used to detect gaps
     * @return {@code true} if the frame was buffered, {@code false} if it was discarded as a duplicate
     *         or as already released
     * @implNote Computes the interframe delay as the arrival gap minus the timestamp gap converted to
     * milliseconds at {@link VideoTimingController#RTP_CLOCK_HZ}, feeds it with the frame size to the
     * jitter estimator, records the sequence gap against the highest sequence number into the loss
     * windows, and orders the frame into the map.
     */
    public boolean insert(EncodedVideoFrame frame, long arrivalMs, long captureRtpTimestamp, int sequenceNumber) {
        Objects.requireNonNull(frame, "frame cannot be null");
        lock.lock();
        try {
            if (lastReleasedCaptureTimestamp >= 0 && captureRtpTimestamp <= lastReleasedCaptureTimestamp) {
                if (Log.TRACE) {
                    LOGGER.log(Level.TRACE, "video jitter buffer: discarding already released frame, seq={0}", sequenceNumber);
                }
                return false;
            }
            if (frames.containsKey(captureRtpTimestamp)) {
                if (Log.TRACE) {
                    LOGGER.log(Level.TRACE, "video jitter buffer: discarding duplicate frame, seq={0}", sequenceNumber);
                }
                return false;
            }

            if (lastInsertCaptureTimestamp >= 0) {
                var timestampGapMs = (captureRtpTimestamp - lastInsertCaptureTimestamp) * 1000.0
                        / VideoTimingController.RTP_CLOCK_HZ;
                var arrivalGapMs = (double) (arrivalMs - lastInsertArrivalMs);
                var frameDelayMs = arrivalGapMs - timestampGapMs;
                jitterEstimator.updateEstimate(frameDelayMs, frame.payload().length, arrivalMs);
            } else {
                jitterEstimator.updateEstimate(0.0, frame.payload().length, arrivalMs);
            }
            lastInsertCaptureTimestamp = captureRtpTimestamp;
            lastInsertArrivalMs = arrivalMs;

            recordSequence(sequenceNumber, arrivalMs);
            frames.put(captureRtpTimestamp, new Buffered(frame, arrivalMs, captureRtpTimestamp, sequenceNumber));
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the frame due for rendering at the given time, or {@code null} when none is due.
     *
     * <p>Releases the earliest buffered frame whose render time computed by the timing controller has
     * arrived; returns {@code null} when the buffer is empty or the earliest frame is not yet due. A
     * frame whose render time has passed by more than the maximum video delay is dropped and the next is
     * considered.
     *
     * @param nowMs the current render thread time in milliseconds, from a monotonic source
     * @return the released frame and its render time, or {@code null} when no frame is due
     * @implNote Releases the earliest frame whose render time has arrived; drops a frame past
     * {@link #MAX_VIDEO_DELAY_MS} and, when its render time is implausibly far in the future, resets the
     * estimator and timing module. After a release reconciles the render time against
     * {@link #audioPlayoutMs} through the A/V sync sink.
     */
    public ReleasedFrame poll(long nowMs) {
        AvSyncFeedback pendingAvSync = null;
        ReleasedFrame released = null;
        lock.lock();
        try {
            currentNowMs = nowMs;
            // The target delay prefix (jitter, decode, and render delay) does not change within this poll,
            // so it is hoisted out of the per frame renderTimeMs call, which then adds only the size
            // dependent NACK gated round trip margin per frame.
            var delayPrefixMs = timingController.targetDelayPrefixMs();
            var iterator = frames.entrySet().iterator();
            while (iterator.hasNext()) {
                var buffered = iterator.next().getValue();
                var renderTimeMs = timingController.renderTimeMs(
                        buffered.captureRtpTimestamp(), buffered.arrivalMs(), delayPrefixMs, frames.size());

                if (renderTimeMs - nowMs > MAX_VIDEO_DELAY_MS) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING,
                                "video jitter buffer: render time implausibly far in future, renderTimeMs={0} nowMs={1}, resetting",
                                renderTimeMs, nowMs);
                    }
                    resetLocked();
                    return null;
                }
                if (nowMs - renderTimeMs > MAX_VIDEO_DELAY_MS) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "video jitter buffer: dropping stale frame, ageMs={0}", nowMs - renderTimeMs);
                    }
                    iterator.remove();
                    lastReleasedCaptureTimestamp = buffered.captureRtpTimestamp();
                    continue;
                }
                if (nowMs < renderTimeMs) {
                    return null;
                }

                iterator.remove();
                lastReleasedCaptureTimestamp = buffered.captureRtpTimestamp();
                var audioPlayout = audioPlayoutMs;
                if (audioPlayout >= 0) {
                    // Compute the A/V sync correction under the lock but push it to the sink after release,
                    // so the video lock is not held across the audio sink notification (a video to audio
                    // lock order hop).
                    pendingAvSync = timingController.computeAvSync(renderTimeMs, audioPlayout);
                }
                if (Log.TRACE) {
                    LOGGER.log(Level.TRACE, "video jitter buffer: releasing frame, renderTimeMs={0} buffered={1}",
                            renderTimeMs, frames.size());
                }
                released = new ReleasedFrame(buffered.frame(), renderTimeMs);
                break;
            }
        } finally {
            lock.unlock();
        }
        if (pendingAvSync != null) {
            avSyncSink.applyAvSyncFeedback(pendingAvSync);
        }
        return released;
    }

    /**
     * Records the local clock playout instant of the audio sample currently playing for the next
     * A/V sync reconciliation.
     *
     * <p>Supplied by the audio playback path so the buffer can measure the video minus audio offset when
     * it releases a frame; until called, A/V sync corrections are not emitted.
     *
     * @param playoutMs the audio playout instant in local clock milliseconds
     */
    public void setAudioPlayoutMs(long playoutMs) {
        this.audioPlayoutMs = playoutMs;
    }

    /**
     * Feeds a round trip time sample to the timing controller's retransmission margin.
     *
     * @param rttMs the round trip time measurement in milliseconds
     */
    public void onRttSample(double rttMs) {
        lock.lock();
        try {
            timingController.onRttSample(rttMs, currentNowMs);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Feeds an observed decode duration to the timing controller's decode time estimate.
     *
     * @param decodeDurationMs the wall clock duration the decoder took for a frame, in milliseconds
     */
    public void onFrameDecoded(long decodeDurationMs) {
        lock.lock();
        try {
            timingController.onFrameDecoded(decodeDurationMs, currentNowMs);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reports the number of frames currently buffered awaiting their render time.
     *
     * @return the count of buffered frames
     */
    public int bufferedFrameCount() {
        lock.lock();
        try {
            return frames.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reports the receiver side packet loss ratio estimate, in the range {@code [0, 1]}.
     *
     * <p>Derived from the sequence number gaps the buffer observes; the transport feeds it back to the
     * sender's rate control.
     *
     * @return the observed loss ratio over the recent scan window
     * @implNote The ratio of the lost frame sliding window to the sum of the lost and received windows,
     * both measured over the same scan duration so the per second scaling cancels; zero when no frames
     * have been received.
     */
    public double packetLossRatio() {
        lock.lock();
        try {
            var lost = lostFrames.rate(currentNowMs);
            var received = receivedFrames.rate(currentNowMs);
            var total = lost + received;
            if (total <= 0) {
                return 0.0;
            }
            return (double) lost / total;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Drops all buffered frames and resets the jitter estimate and timing controller.
     *
     * <p>Called on a stream reset, a resolution change that invalidates the estimate, or render timing
     * that cannot be satisfied.
     */
    public void reset() {
        lock.lock();
        try {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "video jitter buffer: reset, buffered={0}", frames.size());
            resetLocked();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Records a received frame's sequence number into the loss windows, counting any gap as lost.
     *
     * <p>When the sequence number advances the highest seen by more than one, the intervening frames are
     * counted as lost; a sequence number at or below the highest seen is a reordered or duplicate frame
     * and adds only to the received count. The highest sequence number is advanced to the new value when
     * it is newer.
     *
     * @param sequenceNumber the frame's RTP sequence number
     * @param nowMs          the current time in milliseconds, advancing the loss windows
     */
    private void recordSequence(int sequenceNumber, long nowMs) {
        receivedFrames.update(nowMs, 1);
        if (highestSequenceNumber < 0) {
            highestSequenceNumber = sequenceNumber;
            return;
        }
        var gap = sequenceDistance(sequenceNumber, highestSequenceNumber);
        if (gap > 1) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "video jitter buffer: sequence gap detected, lost={0}", gap - 1);
            lostFrames.update(nowMs, gap - 1);
        }
        if (gap > 0) {
            highestSequenceNumber = sequenceNumber;
        }
    }

    /**
     * Returns the forward distance from a reference sequence number to a newer one, accounting for
     * 16 bit wrap.
     *
     * <p>A positive result is the number of sequence steps the new number is ahead of the reference; a
     * non positive result means the new number is not newer (reordered or duplicate).
     *
     * @param sequenceNumber the candidate newer sequence number
     * @param reference      the highest sequence number seen so far
     * @return the wrap aware forward distance, non positive when not newer
     */
    private static int sequenceDistance(int sequenceNumber, int reference) {
        return (short) (sequenceNumber - reference);
    }

    /**
     * Clears all buffered frames and resets the estimator, timing controller, and loss windows.
     *
     * <p>The body held under the lock of {@link #reset()} and the bad render timing path of
     * {@link #poll(long)}.
     */
    private void resetLocked() {
        frames.clear();
        jitterEstimator.reset();
        timingController.reset();
        receivedFrames.clear();
        lostFrames.clear();
        lastInsertCaptureTimestamp = -1;
        lastInsertArrivalMs = 0;
        highestSequenceNumber = -1;
        lastReleasedCaptureTimestamp = -1;
    }

    /**
     * One frame released from the buffer together with the render instant it was scheduled for.
     *
     * <p>The {@link #renderTimeMs()} is the local clock instant the renderer should display the frame;
     * a release at or after that instant is on time, and the renderer may present immediately.
     *
     * @param frame        the released compressed frame; never {@code null}
     * @param renderTimeMs the local clock render instant the frame was scheduled for, in milliseconds
     */
    public record ReleasedFrame(EncodedVideoFrame frame, long renderTimeMs) {
    }

    /**
     * One buffered frame together with the timing recorded when it was inserted.
     *
     * @param frame               the compressed frame; never {@code null}
     * @param arrivalMs           the local arrival time in milliseconds
     * @param captureRtpTimestamp the 90 kHz capture RTP timestamp, the map key
     * @param sequenceNumber      the RTP sequence number
     */
    private record Buffered(EncodedVideoFrame frame, long arrivalMs, long captureRtpTimestamp, int sequenceNumber) {
    }
}
