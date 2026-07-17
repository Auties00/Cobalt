package com.github.auties00.cobalt.calls.transport.congestion.bwe;

import com.github.auties00.cobalt.calls.transport.congestion.bwe.shaping.FastRampController;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Estimates WhatsApp's audio send bitrate with a sender side AIMD control loop, ramping the bitrate up
 * while the link is healthy and backing it off in proportion to loss and round trip time growth.
 *
 * <p>Each feedback round supplies the packet loss ratio, the round trip time, the latest remote
 * receiver estimate, and the per round {@code min_remote_bitrate_estimate}.
 * {@link #update(double, long, long, long, long)} first decides congestion: the link is congested when
 * the round trip time exceeds {@link #FIRST_RTT_CONGESTION_MULTIPLIER} times the first observed
 * round trip time, exceeds the absolute {@link #ABSOLUTE_RTT_CONGESTION_NS} ceiling, or the loss ratio
 * crosses the configured high threshold. When not congested the estimate grows by
 * {@code factor * (sender_bwe + ADDITIVE_FLOOR_BPS)} with {@code factor} of {@link #INCREASE_FACTOR_LOW}
 * while the remote estimate is absent or below the latched minimum remote bitrate floor and
 * {@link #INCREASE_FACTOR_HIGH} otherwise. That floor is seeded once, lazily, from the first nonzero
 * per round {@code min_remote_bitrate_estimate} and then held. When congested the estimate is multiplied
 * by {@code max(DECREASE_FLOOR, 1 - DECREASE_LOSS_SLOPE * plr)}. The result is clamped into the
 * configured bitrate range.
 *
 * <p>Instances are not thread safe; the call session drives one estimator from the single transport
 * thread.
 *
 * @implNote This implementation adds {@link #ADDITIVE_FLOOR_BPS} to the estimate before the increase
 * multiply so a minimum absolute ramp applies even at very low estimates. The increase factors
 * {@value #INCREASE_FACTOR_LOW} and {@value #INCREASE_FACTOR_HIGH}, the {@value #ADDITIVE_FLOOR_BPS} bps
 * additive floor, the {@value #DECREASE_LOSS_SLOPE} loss slope, the {@value #DECREASE_FLOOR} decrease
 * floor, and the {@value #FIRST_RTT_CONGESTION_MULTIPLIER} first round trip multiplier are WhatsApp's
 * constants. WhatsApp's {@code voip_settings} enable this estimator and set the loss thresholds through
 * {@code sbwe_loss_high} (30 percent) and {@code sbwe_loss_low} (10 percent).
 */
public final class AudioSenderBandwidthEstimator {
    /**
     * The logger for {@link AudioSenderBandwidthEstimator}.
     */
    private static final System.Logger LOGGER = Log.get(AudioSenderBandwidthEstimator.class);

    /**
     * Increase factor applied when the remote estimate is absent or below the minimum remote bitrate.
     */
    static final double INCREASE_FACTOR_LOW = 1.05;

    /**
     * Increase factor applied when the remote estimate is at or above the minimum remote bitrate.
     */
    static final double INCREASE_FACTOR_HIGH = 1.10;

    /**
     * Additive floor, in bits per second, added to the estimate before the increase multiply.
     *
     * <p>Adding this before multiplying guarantees a minimum absolute ramp even at very low estimates.
     */
    static final long ADDITIVE_FLOOR_BPS = 1000;

    /**
     * Loss proportional decrease slope applied on congestion.
     *
     * <p>The decrease multiplier is {@code 1 - DECREASE_LOSS_SLOPE * plr}, floored at
     * {@link #DECREASE_FLOOR}.
     */
    static final double DECREASE_LOSS_SLOPE = 0.5;

    /**
     * Hard floor on the loss proportional decrease multiplier.
     *
     * <p>The decrease never drops the estimate below this fraction of its current value in one step,
     * reached once the loss ratio passes roughly two thirds.
     */
    static final double DECREASE_FLOOR = 0.67;

    /**
     * Multiplier on the first observed round trip time above which the link is judged congested.
     */
    static final double FIRST_RTT_CONGESTION_MULTIPLIER = 3.0;

    /**
     * Absolute round trip time ceiling, in nanoseconds, above which the link is judged congested.
     *
     * <p>One millisecond expressed in nanoseconds; this absolute gate applies in addition to the first
     * round trip multiple.
     */
    static final long ABSOLUTE_RTT_CONGESTION_NS = 1_000_000;

    /**
     * Staleness window, in milliseconds, past which absent feedback is treated as congestion.
     */
    static final long NO_FEEDBACK_CONGESTION_MS = 500;

    /**
     * Lower bound, in bits per second, the estimate is clamped to.
     */
    private final long minBitrateBps;

    /**
     * Upper bound, in bits per second, the estimate is clamped to.
     */
    private final long maxBitrateBps;

    /**
     * High loss ratio threshold, in {@code [0, 1]}, above which the link is judged congested.
     *
     * <p>Seeded from WhatsApp's {@code sbwe_loss_high} percent value.
     */
    private final double lossHighThreshold;

    /**
     * Current audio sender estimate, in bits per second.
     */
    private long senderBweBps;

    /**
     * First observed round trip time, in nanoseconds, or {@code -1} when none has been observed.
     *
     * <p>The congestion test compares the current round trip time against
     * {@link #FIRST_RTT_CONGESTION_MULTIPLIER} times this baseline.
     */
    private long firstRttNs = -1;

    /**
     * Highest round trip time, in nanoseconds, observed so far.
     *
     * <p>Retained for diagnostics reporting.
     */
    private long highestRttNs = 0;

    /**
     * Latched minimum remote bitrate, in bits per second, gating the healthy link increase factor, or
     * {@code 0} until the first nonzero per round value seeds it.
     *
     * <p>This floor is written once while it is {@code 0}: the first feedback round whose supplied
     * {@code minRemoteBitrateEstimateBps} is nonzero seeds it, and it then holds for the rest of the
     * call regardless of later rounds. While it is {@code 0} the increase factor is
     * {@link #INCREASE_FACTOR_LOW} whenever the remote estimate is absent, exactly as before any feedback
     * supplies the floor. The per round value that seeds it is itself the minimum, across all connected
     * participants, of each participant's reported remote bandwidth estimate; it is not a field of a
     * single inbound RTCP packet.
     */
    private long minRemoteBweBps = 0;

    /**
     * Constructs an audio sender estimator with the given bounds, loss threshold, and start estimate.
     *
     * @param minBitrateBps     the lower bound on the estimate, in bits per second; must be positive
     * @param maxBitrateBps     the upper bound on the estimate, in bits per second; must be at least
     *                          the minimum
     * @param startBitrateBps   the initial estimate, in bits per second; clamped into the bounds
     * @param lossHighThreshold the high loss ratio threshold, in {@code [0, 1]}, gating congestion
     * @throws IllegalArgumentException if the bounds are not a positive ordered pair
     */
    public AudioSenderBandwidthEstimator(long minBitrateBps, long maxBitrateBps, long startBitrateBps,
                                         double lossHighThreshold) {
        if (minBitrateBps <= 0) {
            throw new IllegalArgumentException("minBitrateBps must be positive: " + minBitrateBps);
        }
        if (maxBitrateBps < minBitrateBps) {
            throw new IllegalArgumentException("maxBitrateBps below minBitrateBps: " + maxBitrateBps);
        }
        this.minBitrateBps = minBitrateBps;
        this.maxBitrateBps = maxBitrateBps;
        this.lossHighThreshold = Math.clamp(lossHighThreshold, 0.0, 1.0);
        this.senderBweBps = Math.clamp(startBitrateBps, minBitrateBps, maxBitrateBps);
    }

    /**
     * Updates the audio sender estimate from one feedback round and returns the new estimate.
     *
     * <p>Records the first and highest round trip times, lazily seeds the latched minimum remote bitrate
     * floor while it is still {@code 0}, decides congestion from the round trip and loss inputs, then
     * applies either the healthy link increase or the loss proportional decrease and clamps the result
     * into the configured range. The healthy link increase factor is {@link #INCREASE_FACTOR_LOW} while
     * the remote estimate is absent or below the latched floor and {@link #INCREASE_FACTOR_HIGH}
     * otherwise.
     *
     * @param plr                the packet loss ratio, in {@code [0, 1]}
     * @param rttNs              the current round trip time, in nanoseconds; ignored when not positive
     * @param remoteBweBps       the latest remote receiver estimate, in bits per second; {@code 0} when
     *                           none has arrived
     * @param minRemoteBitrateEstimateBps the per round {@code min_remote_bitrate_estimate}, in bits per
     *                           second: the minimum across all connected participants of each
     *                           participant's reported remote bandwidth estimate, or {@code 0} when none
     *                           is available; it seeds the latched floor once while that floor is still
     *                           {@code 0} and is otherwise ignored
     * @param noFeedbackElapsedMs the time, in milliseconds, since the last feedback was received; a
     *                           value past the configured staleness window is treated as congestion
     * @return the updated estimate, in bits per second
     */
    public long update(double plr, long rttNs, long remoteBweBps, long minRemoteBitrateEstimateBps,
                       long noFeedbackElapsedMs) {
        if (rttNs > 0) {
            if (firstRttNs < 0) {
                firstRttNs = rttNs;
            }
            if (rttNs > highestRttNs) {
                highestRttNs = rttNs;
            }
        }
        if (minRemoteBweBps == 0 && minRemoteBitrateEstimateBps != 0) {
            minRemoteBweBps = minRemoteBitrateEstimateBps;
        }
        var congested = isCongested(plr, rttNs, noFeedbackElapsedMs);
        if (congested) {
            var factor = Math.max(DECREASE_FLOOR, 1.0 - DECREASE_LOSS_SLOPE * plr);
            senderBweBps = (long) (senderBweBps * factor);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "audio sender bwe congested: plr={0}, rttNs={1}, decreaseFactor={2}, newEstimate={3}bps",
                        plr, rttNs, factor, senderBweBps);
            }
        } else {
            // TODO: wire FastRampController: instantiate it with the aboveMin, slope, and loss thresholds, call onRxRtp(rttMs, lossRatio, nowMs) per received RTP, and let isRampActive() extend the additive ramp here
            var factor = remoteBweBps == 0 || remoteBweBps < minRemoteBweBps
                    ? INCREASE_FACTOR_LOW
                    : INCREASE_FACTOR_HIGH;
            senderBweBps = (long) (factor * (senderBweBps + ADDITIVE_FLOOR_BPS));
        }
        senderBweBps = Math.clamp(senderBweBps, minBitrateBps, maxBitrateBps);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "audio sender bwe update: congested={0}, remoteBweBps={1}, newEstimate={2}bps",
                    congested, remoteBweBps, senderBweBps);
        }
        return senderBweBps;
    }

    /**
     * Decides whether the link is congested for this feedback round.
     *
     * <p>The link is congested when the round trip time exceeds
     * {@link #FIRST_RTT_CONGESTION_MULTIPLIER} times the first observed value, exceeds the absolute
     * {@link #ABSOLUTE_RTT_CONGESTION_NS} ceiling, the loss ratio is at or above the high threshold, or
     * no feedback has arrived within the supplied staleness window.
     *
     * @param plr                 the packet loss ratio, in {@code [0, 1]}
     * @param rttNs               the current round trip time, in nanoseconds; ignored when not positive
     * @param noFeedbackElapsedMs the time, in milliseconds, since the last feedback, with a negative
     *                            value meaning feedback just arrived
     * @return {@code true} when the link is judged congested
     */
    private boolean isCongested(double plr, long rttNs, long noFeedbackElapsedMs) {
        if (plr >= lossHighThreshold) {
            return true;
        }
        if (rttNs > 0 && firstRttNs > 0
                && (rttNs > FIRST_RTT_CONGESTION_MULTIPLIER * firstRttNs
                || rttNs > ABSOLUTE_RTT_CONGESTION_NS)) {
            return true;
        }
        return noFeedbackElapsedMs >= 0 && noFeedbackElapsedMs > NO_FEEDBACK_CONGESTION_MS;
    }

    /**
     * Returns the current audio sender estimate.
     *
     * @return the estimate, in bits per second
     */
    public long senderBweBps() {
        return senderBweBps;
    }

    /**
     * Returns the highest round trip time observed so far.
     *
     * @return the highest round trip time, in nanoseconds
     */
    public long highestRttNs() {
        return highestRttNs;
    }

    /**
     * Resets the estimator to its initial estimate and clears the round trip time history and the latched
     * minimum remote bitrate floor.
     *
     * <p>Clearing the latched floor mirrors a fresh estimator: the next round whose
     * {@code min_remote_bitrate_estimate} is nonzero seeds it again, so the increase factor returns to
     * {@link #INCREASE_FACTOR_LOW} until the roster supplies a floor again.
     *
     * @param startBitrateBps the estimate to restart from, in bits per second; clamped into the bounds
     */
    public void reset(long startBitrateBps) {
        senderBweBps = Math.clamp(startBitrateBps, minBitrateBps, maxBitrateBps);
        firstRttNs = -1;
        highestRttNs = 0;
        minRemoteBweBps = 0;
    }
}
