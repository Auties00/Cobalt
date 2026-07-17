package com.github.auties00.cobalt.calls.transport.congestion.ratecontrol;

import com.github.auties00.cobalt.calls.util.RttEstimator;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Drives the six state audio quality machine from three hysteresis signals (packet loss ratio, round
 * trip time, receiver estimated maximum bitrate) and reports the forward error correction overhead and
 * the per state target bitrate the audio path should use.
 *
 * <p>Each {@link #update(double, double, long, long)} round folds the latest measurements in, evaluates
 * the three signals against the active state's lower and upper thresholds with a trend slope gate,
 * aggregates the three verdicts into a single move, and advances the {@link UaqcState} along the ladder
 * from {@link UaqcState#HIGH_QUALITY} (best) to {@link UaqcState#ULTRA_LOW_BANDWIDTH} (worst) with
 * {@link UaqcState#PROBING} as the entry state: any {@link UaqcSignal#NON_CONGESTED} signal can move
 * toward a better state, any {@link UaqcSignal#CONGESTED} signal moves toward a worse state, and all
 * {@link UaqcSignal#HOLD} keeps the state, each subject to the per state legal moves. {@link #state()}
 * reports the current quality state, {@link #targetBitrateBps()} the per state target bitrate, and
 * {@link #fecOverheadFraction()} the loss driven forward error correction overhead the
 * {@link AudioRateController} subtracts from the audio target while {@link UaqcState#PROBING}.
 *
 * <p>The control holds smoothed trend state across rounds: an exponential round trip estimate and floor,
 * an exponentially smoothed packet loss ratio, and a smoothed receiver estimate value with a derived
 * slope. It holds no clock; the caller supplies a current time reading each round so the trend slopes are
 * computed against real elapsed time. Instances are not thread safe; the single rate control thread that
 * owns one drives all updates.
 *
 * @implNote This implementation evaluates the three signals independently: the packet loss signal reads
 * the exponentially smoothed loss; the round trip signal gates on an absolute threshold first, then on a
 * current over minimum ratio combined with the trend slope; the receiver estimate signal combines a level
 * test with a trend slope. Every threshold, smoothing factor, target bitrate, slide window, and
 * transition hesitation value lives in {@link Config} and is a per state server voip parameter: the
 * server pushes the operative values in the {@code <voip_settings>} stanza and {@link Config#defaults()}
 * wires them in, keeping the compiled defaults only for the fields the server omits.
 */
public final class UnifiedAudioQualityControl {
    /**
     * The logger for {@link UnifiedAudioQualityControl}.
     */
    private static final System.Logger LOGGER = Log.get(UnifiedAudioQualityControl.class);

    /**
     * Holds the per state lower and upper thresholds and trend slopes the three signals are evaluated
     * against in a given quality state.
     *
     * <p>Each signal has a lower threshold below which it is {@link UaqcSignal#NON_CONGESTED} and an upper
     * threshold at or above which it is {@link UaqcSignal#CONGESTED}; the round trip and receiver estimate
     * signals additionally gate on a trend slope. The active state's threshold block is passed to the
     * signal evaluators, so a different state evaluates the same measurements against different bounds.
     *
     * @param plrLower               the packet loss ratio lower threshold, in {@code [0, 1]}
     * @param plrUpper               the packet loss ratio upper threshold, in {@code [0, 1]}
     * @param rttAbsoluteThresholdMs the absolute round trip threshold above which it is congested
     * @param rttRatioThreshold      the current over minimum round trip ratio congestion threshold
     * @param rttUpSlopeMsPerS       the round trip up slope, in ms per second, gating congestion
     * @param rttDownSlopeMsPerS     the round trip down slope, in ms per second, gating recovery
     * @param rembLowerBps           the receiver estimate lower threshold, in bits per second
     * @param rembUpperBps           the receiver estimate upper threshold, in bits per second
     * @param rembUpSlopeBpsPerS     the receiver estimate up slope gating headroom
     */
    public record StateThresholds(
            double plrLower,
            double plrUpper,
            double rttAbsoluteThresholdMs,
            double rttRatioThreshold,
            double rttUpSlopeMsPerS,
            double rttDownSlopeMsPerS,
            long rembLowerBps,
            long rembUpperBps,
            double rembUpSlopeBpsPerS
    ) {
    }

    /**
     * Holds the full configuration of the quality machine: the per state thresholds, the smoothing
     * factors, the per state target bitrates, the slide window size, the transition hesitation, and the
     * probing exit and overhead.
     *
     * @param highQuality              the {@link UaqcState#HIGH_QUALITY} state thresholds
     * @param bandwidthManaged         the {@link UaqcState#BANDWIDTH_MANAGED} state thresholds
     * @param lossy                    the {@link UaqcState#LOSSY} state thresholds
     * @param drain                    the {@link UaqcState#DRAIN} state thresholds
     * @param ultraLowBandwidth        the {@link UaqcState#ULTRA_LOW_BANDWIDTH} state thresholds
     * @param plrEmaAlpha              the smoothing factor for the packet loss ratio average
     * @param rttEmaAlpha              the smoothing factor for the round trip estimate
     * @param rembEmaAlpha             the smoothing factor for the receiver estimate value and slope
     * @param highQualityTargetBps     the {@link UaqcState#HIGH_QUALITY} target bitrate, in bits per second
     * @param bandwidthManagedTargetBps the {@link UaqcState#BANDWIDTH_MANAGED} target bitrate, in bits per second
     * @param lossyTargetBps           the {@link UaqcState#LOSSY} target bitrate, in bits per second
     * @param drainTargetBps           the {@link UaqcState#DRAIN} target bitrate, in bits per second
     * @param ultraLowBandwidthTargetBps the {@link UaqcState#ULTRA_LOW_BANDWIDTH} target bitrate, in bits per second
     * @param probingTargetBps         the {@link UaqcState#PROBING} target bitrate, in bits per second
     * @param slideWindowSize          the number of rounds in the signal smoothing slide window
     * @param transitionHesitationMs   the minimum time, in milliseconds, between state transitions
     * @param probingExitRembBps       the receiver estimate above which probing exits
     * @param probingFecOverheadPct    the forward error correction overhead percentage applied in probing
     */
    public record Config(
            StateThresholds highQuality,
            StateThresholds bandwidthManaged,
            StateThresholds lossy,
            StateThresholds drain,
            StateThresholds ultraLowBandwidth,
            double plrEmaAlpha,
            double rttEmaAlpha,
            double rembEmaAlpha,
            long highQualityTargetBps,
            long bandwidthManagedTargetBps,
            long lossyTargetBps,
            long drainTargetBps,
            long ultraLowBandwidthTargetBps,
            long probingTargetBps,
            int slideWindowSize,
            long transitionHesitationMs,
            long probingExitRembBps,
            double probingFecOverheadPct
    ) {
        /**
         * Returns a default configuration seeded with the live per state thresholds, target bitrates,
         * slide window, and transition hesitation.
         *
         * <p>The threshold fields the server pushes are wired to their pushed values; the smoothing
         * factors and the omitted per state threshold fields keep their compiled defaults. The per state
         * target bitrates are the live values (high quality twenty five kilobits, bandwidth managed twenty
         * four kilobits, probing and drain fifteen kilobits, ultra low twelve kilobits), the slide window
         * is thirty two rounds, the transition hesitation is zero, and probing exits once the receiver
         * estimate clears twenty two kilobits.
         *
         * @return the default unified audio quality control configuration
         * @implNote This implementation wires the {@link UaqcState#HIGH_QUALITY},
         * {@link UaqcState#BANDWIDTH_MANAGED}, {@link UaqcState#DRAIN}, and
         * {@link UaqcState#ULTRA_LOW_BANDWIDTH} threshold families and the per state target bitrates to the
         * values the server pushes in {@code <voip_settings>}; {@link UaqcState#LOSSY} reuses the
         * {@link UaqcState#BANDWIDTH_MANAGED} bounds, and the lower loss thresholds, the round trip down
         * slopes, the smoothing factors, and the probing overhead percentage keep the compiled defaults the
         * server omits.
         */
        public static Config defaults() {
            var highQuality = new StateThresholds(
                    0.02,    // plrLower: compiled default; high quality tolerates little loss
                    0.05,    // plrUpper: uaqc_high_quality_plr_upper_threshold
                    300.0,   // rttAbsoluteThresholdMs: uaqc_high_quality_rtt_threshold
                    2.0,     // rttRatioThreshold: uaqc_high_quality_rtt_ratio_threshold
                    80.0,    // rttUpSlopeMsPerS: uaqc_high_quality_rtt_trend_up_threshold
                    -5.0,    // rttDownSlopeMsPerS: compiled default
                    20_000,  // rembLowerBps: uaqc_high_quality_remb_lower_threshold
                    25_000,  // rembUpperBps: uaqc_high_quality_remb_upper_threshold
                    1.0      // rembUpSlopeBpsPerS: uaqc_high_quality_remb_trend_up_threshold
            );
            var bandwidthManaged = new StateThresholds(
                    0.05,    // plrLower: compiled default; server omits this key
                    0.12,    // plrUpper: uaqc_bandwidth_managed_plr_upper_threshold
                    500.0,   // rttAbsoluteThresholdMs: uaqc_bandwidth_managed_rtt_threshold
                    2.5,     // rttRatioThreshold: uaqc_bandwidth_managed_rtt_ratio_threshold
                    100.0,   // rttUpSlopeMsPerS: uaqc_bandwidth_managed_rtt_trend_up_threshold
                    -5.0,    // rttDownSlopeMsPerS: compiled default; server omits this key
                    16_000,  // rembLowerBps: uaqc_bandwidth_managed_remb_lower_threshold
                    24_000,  // rembUpperBps: uaqc_bandwidth_managed_remb_upper_threshold
                    1.0      // rembUpSlopeBpsPerS: uaqc_bandwidth_managed_remb_trend_up_threshold
            );
            var lossy = new StateThresholds(
                    0.05,    // plrLower: compiled default, same loss floor as bandwidth managed
                    0.12,    // plrUpper: reuses the bandwidth managed loss bounds
                    500.0,   // rttAbsoluteThresholdMs: reuses the bandwidth managed rtt bound
                    2.5,     // rttRatioThreshold: reuses the bandwidth managed ratio bound
                    100.0,   // rttUpSlopeMsPerS: reuses the bandwidth managed rtt up slope
                    -5.0,    // rttDownSlopeMsPerS: compiled default
                    16_000,  // rembLowerBps: reuses the bandwidth managed remb lower bound
                    24_000,  // rembUpperBps: reuses the bandwidth managed remb upper bound
                    1.0      // rembUpSlopeBpsPerS: reuses the bandwidth managed remb up slope
            );
            var drain = new StateThresholds(
                    0.08,    // plrLower: compiled default; drain recovers when loss eases
                    0.20,    // plrUpper: uaqc_drain_plr_upper_threshold
                    700.0,   // rttAbsoluteThresholdMs: uaqc_drain_rtt_threshold
                    3.0,     // rttRatioThreshold: uaqc_drain_rtt_ratio_threshold
                    150.0,   // rttUpSlopeMsPerS: uaqc_drain_rtt_trend_up_threshold
                    -5.0,    // rttDownSlopeMsPerS: compiled default
                    12_000,  // rembLowerBps: uaqc_drain_remb_lower_threshold
                    16_000,  // rembUpperBps: uaqc_drain_remb_upper_threshold
                    1.0      // rembUpSlopeBpsPerS: uaqc_drain_remb_trend_up_threshold
            );
            var ultraLowBandwidth = new StateThresholds(
                    0.10,    // plrLower: compiled default
                    0.30,    // plrUpper: uaqc_ultra_low_bandwidth_plr_upper_threshold
                    1000.0,  // rttAbsoluteThresholdMs: uaqc_ultra_low_bandwidth_rtt_threshold
                    4.0,     // rttRatioThreshold: uaqc_ultra_low_bandwidth_rtt_ratio_threshold
                    200.0,   // rttUpSlopeMsPerS: uaqc_ultra_low_bandwidth_rtt_trend_up_threshold
                    -5.0,    // rttDownSlopeMsPerS: compiled default
                    8_000,   // rembLowerBps: uaqc_ultra_low_bandwidth_remb_lower_threshold
                    12_000,  // rembUpperBps: uaqc_ultra_low_bandwidth_remb_upper_threshold
                    1.0      // rembUpSlopeBpsPerS: uaqc_ultra_low_bandwidth_remb_trend_up_threshold
            );
            return new Config(
                    highQuality,
                    bandwidthManaged,
                    lossy,
                    drain,
                    ultraLowBandwidth,
                    0.1,     // plrEmaAlpha: compiled default; server omits this key
                    0.1,     // rttEmaAlpha: compiled default; server omits this key
                    0.1,     // rembEmaAlpha: compiled default; server omits this key
                    25_000,  // highQualityTargetBps: uaqc high_quality target_bitrate
                    24_000,  // bandwidthManagedTargetBps: uaqc bandwidth_managed target_bitrate
                    24_000,  // lossyTargetBps: reuses the bandwidth managed target
                    15_000,  // drainTargetBps: uaqc drain target_bitrate
                    12_000,  // ultraLowBandwidthTargetBps: uaqc ultra_low_bandwidth target_bitrate
                    15_000,  // probingTargetBps: uaqc probing target_bitrate
                    32,      // slideWindowSize: uaqc_slide_window_size
                    0,       // transitionHesitationMs: uaqc_state_transition_hesitation_ms
                    22_000,  // probingExitRembBps: uaqc_probing_remb_threshold
                    50.0     // probingFecOverheadPct: compiled default; server omits uaqc_probing_compensation_pct
            );
        }
    }

    /**
     * The configuration of the quality machine.
     */
    private final Config config;

    /**
     * The round trip estimator providing the smoothed estimate and the slowly decaying floor.
     */
    private final RttEstimator rttEstimator;

    /**
     * The current quality state of the machine.
     */
    private UaqcState state;

    /**
     * The exponentially smoothed packet loss ratio, in {@code [0, 1]}.
     */
    private double plrEma;

    /**
     * The exponentially smoothed receiver estimated maximum bitrate, in bits per second.
     */
    private double rembEma;

    /**
     * The smoothed receiver estimate trend slope, in bits per second per second.
     */
    private double rembSlopeBpsPerS;

    /**
     * The current forward error correction overhead fraction, in {@code [0, 1]}, applied while probing.
     */
    private double fecOverheadFraction;

    /**
     * The time of the previous update, in milliseconds, for the round trip and receiver estimate slopes,
     * or {@code -1} before the first update.
     */
    private long lastUpdateMs;

    /**
     * The smoothed round trip estimate at the previous update, in milliseconds, for the slope, or
     * {@code 0} before the first update.
     */
    private double lastRttMs;

    /**
     * The time of the last state transition, in milliseconds, for the transition hesitation gate, or
     * {@code Long.MIN_VALUE} before any transition.
     */
    private long lastTransitionMs;

    /**
     * Constructs a control with the given configuration in the {@link UaqcState#PROBING} state.
     *
     * @param config the configuration; never {@code null}
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public UnifiedAudioQualityControl(Config config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.rttEstimator = new RttEstimator();
        this.state = UaqcState.PROBING;
        this.plrEma = 0.0;
        this.rembEma = 0.0;
        this.rembSlopeBpsPerS = 0.0;
        this.fecOverheadFraction = 0.0;
        this.lastUpdateMs = -1;
        this.lastRttMs = 0.0;
        this.lastTransitionMs = Long.MIN_VALUE;
    }

    /**
     * Folds the latest measurements in, advances the quality state, and returns it.
     *
     * <p>Updates the smoothed loss, round trip, and receiver estimate trend state, evaluates the three
     * signals against the active state's thresholds, aggregates them into a better or worse move, and
     * applies the per state transition rules subject to the transition hesitation gate. While
     * {@link UaqcState#PROBING} it also recomputes the forward error correction overhead from the loss and
     * exits probing when the receiver estimate clears the exit threshold.
     *
     * @param plr     the instantaneous packet loss ratio over the recent window, in {@code [0, 1]}
     * @param rttMs   the latest round trip time sample in milliseconds; a sample that is not positive is ignored
     * @param rembBps the latest receiver estimated maximum bitrate in bits per second
     * @param nowMs   the current time in milliseconds, from a monotonic source
     * @return the quality state after this round
     */
    public UaqcState update(double plr, double rttMs, long rembBps, long nowMs) {
        plrEma = plrEma == 0.0 ? plr : (1.0 - config.plrEmaAlpha()) * plrEma + config.plrEmaAlpha() * plr;
        rttEstimator.update(rttMs, config.rttEmaAlpha());
        rttEstimator.updateMin(rttMs, config.rttEmaAlpha());
        var elapsedS = lastUpdateMs >= 0 && nowMs > lastUpdateMs ? (nowMs - lastUpdateMs) / 1000.0 : 0.0;
        updateRembTrend(rembBps, elapsedS);
        var rttSlope = updateRttSlope(elapsedS);
        lastUpdateMs = nowMs;

        var thresholds = thresholdsFor(state);
        var plrSignal = evaluatePlr(thresholds);
        var rttSignal = evaluateRtt(thresholds, rttSlope);
        var rembSignal = evaluateRemb(thresholds);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "uaqc round: state={0} plr={1} rtt={2} remb={3}",
                    state, plrSignal, rttSignal, rembSignal);
        }

        applyTransition(plrSignal, rttSignal, rembSignal, rembBps, nowMs);

        if (state == UaqcState.PROBING) {
            updateProbingFec();
        } else {
            fecOverheadFraction = 0.0;
        }
        return state;
    }

    /**
     * Returns the current quality state.
     *
     * @return the current {@link UaqcState}
     */
    public UaqcState state() {
        return state;
    }

    /**
     * Returns the per state target bitrate of the current quality state.
     *
     * <p>Each state carries the target bitrate the audio path should aim for while in it: highest while
     * {@link UaqcState#HIGH_QUALITY}, lowest while {@link UaqcState#ULTRA_LOW_BANDWIDTH}.
     *
     * @return the current state's target bitrate, in bits per second
     */
    public long targetBitrateBps() {
        return switch (state) {
            case PROBING -> config.probingTargetBps();
            case HIGH_QUALITY -> config.highQualityTargetBps();
            case BANDWIDTH_MANAGED -> config.bandwidthManagedTargetBps();
            case LOSSY -> config.lossyTargetBps();
            case DRAIN -> config.drainTargetBps();
            case ULTRA_LOW_BANDWIDTH -> config.ultraLowBandwidthTargetBps();
        };
    }

    /**
     * Returns the forward error correction overhead fraction the audio path should add this round.
     *
     * <p>It is nonzero only while {@link UaqcState#PROBING}; the {@link AudioRateController} subtracts this
     * fraction of the target from the codec bitrate to fund the redundancy.
     *
     * @return the overhead fraction in {@code [0, 1]}
     */
    public double fecOverheadFraction() {
        return fecOverheadFraction;
    }

    /**
     * Returns the threshold block the given state evaluates its signals against.
     *
     * <p>{@link UaqcState#PROBING} has no signal thresholds of its own; it exits only on the probing exit
     * receiver estimate test, so it borrows the {@link UaqcState#BANDWIDTH_MANAGED} block for the smoothing
     * driven evaluation that runs each round.
     *
     * @param current the current state
     * @return the active threshold block
     */
    private StateThresholds thresholdsFor(UaqcState current) {
        return switch (current) {
            case PROBING, BANDWIDTH_MANAGED -> config.bandwidthManaged();
            case HIGH_QUALITY -> config.highQuality();
            case LOSSY -> config.lossy();
            case DRAIN -> config.drain();
            case ULTRA_LOW_BANDWIDTH -> config.ultraLowBandwidth();
        };
    }

    /**
     * Folds a receiver estimate sample into its smoothed value and trend slope.
     *
     * @param rembBps  the receiver estimated maximum bitrate sample, in bits per second
     * @param elapsedS the seconds since the previous update, or {@code 0} when none is available
     */
    private void updateRembTrend(long rembBps, double elapsedS) {
        if (rembEma == 0.0) {
            rembEma = rembBps;
            return;
        }
        var prev = rembEma;
        rembEma = (1.0 - config.rembEmaAlpha()) * rembEma + config.rembEmaAlpha() * rembBps;
        if (elapsedS > 0.0) {
            var rawSlope = (rembEma - prev) / elapsedS;
            rembSlopeBpsPerS = (1.0 - config.rembEmaAlpha()) * rembSlopeBpsPerS + config.rembEmaAlpha() * rawSlope;
        }
    }

    /**
     * Computes the round trip trend slope against the previous smoothed estimate, in ms per second.
     *
     * @param elapsedS the seconds since the previous update, or {@code 0} when none is available
     * @return the round trip slope, or {@code 0} when no elapsed time is available
     */
    private double updateRttSlope(double elapsedS) {
        var currentRtt = (double) rttEstimator.estimate();
        var slope = 0.0;
        if (elapsedS > 0.0) {
            slope = (currentRtt - lastRttMs) / elapsedS;
        }
        lastRttMs = currentRtt;
        return slope;
    }

    /**
     * Evaluates the packet loss ratio signal against the active state's thresholds.
     *
     * @param thresholds the active state's threshold block
     * @return {@link UaqcSignal#NON_CONGESTED} at or below the lower threshold,
     *         {@link UaqcSignal#CONGESTED} at or above the upper threshold, else {@link UaqcSignal#HOLD}
     */
    private UaqcSignal evaluatePlr(StateThresholds thresholds) {
        if (plrEma <= thresholds.plrLower()) {
            return UaqcSignal.NON_CONGESTED;
        }
        if (plrEma >= thresholds.plrUpper()) {
            return UaqcSignal.CONGESTED;
        }
        return UaqcSignal.HOLD;
    }

    /**
     * Evaluates the round trip time signal against the active state's absolute threshold, the current over
     * minimum ratio, and the trend slope.
     *
     * <p>A current round trip above the absolute threshold is congested outright. Otherwise the ratio of
     * the current estimate to the floor is compared with the ratio threshold and combined with the trend
     * slope: a ratio at or above the threshold while the slope is rising yields {@link UaqcSignal#CONGESTED},
     * while a ratio below the threshold with the slope falling past the down slope yields
     * {@link UaqcSignal#NON_CONGESTED}.
     *
     * @param thresholds the active state's threshold block
     * @param rttSlope   the round trip trend slope in ms per second
     * @return the round trip congestion verdict
     */
    private UaqcSignal evaluateRtt(StateThresholds thresholds, double rttSlope) {
        var current = (double) rttEstimator.estimate();
        if (current >= thresholds.rttAbsoluteThresholdMs()) {
            return UaqcSignal.CONGESTED;
        }
        var floor = (double) rttEstimator.minEstimate();
        if (floor <= 0.0) {
            return UaqcSignal.HOLD;
        }
        var ratio = current / floor;
        if (ratio >= thresholds.rttRatioThreshold() && rttSlope >= thresholds.rttUpSlopeMsPerS()) {
            return UaqcSignal.CONGESTED;
        }
        if (ratio < thresholds.rttRatioThreshold() && rttSlope <= thresholds.rttDownSlopeMsPerS()) {
            return UaqcSignal.NON_CONGESTED;
        }
        return UaqcSignal.HOLD;
    }

    /**
     * Evaluates the receiver estimate signal against the active state's level thresholds and trend slope.
     *
     * @param thresholds the active state's threshold block
     * @return {@link UaqcSignal#NON_CONGESTED} when the estimate is at or above the upper threshold and
     *         not falling, {@link UaqcSignal#CONGESTED} when at or below the lower threshold, else
     *         {@link UaqcSignal#HOLD}
     */
    private UaqcSignal evaluateRemb(StateThresholds thresholds) {
        if (rembEma >= thresholds.rembUpperBps() && rembSlopeBpsPerS >= thresholds.rembUpSlopeBpsPerS()) {
            return UaqcSignal.NON_CONGESTED;
        }
        if (rembEma <= thresholds.rembLowerBps()) {
            return UaqcSignal.CONGESTED;
        }
        return UaqcSignal.HOLD;
    }

    /**
     * Applies the aggregated congestion verdict to the quality state using the per state ladder, subject to
     * the transition hesitation gate.
     *
     * <p>From {@link UaqcState#PROBING} the only exit is to {@link UaqcState#BANDWIDTH_MANAGED} when the
     * receiver estimate clears the probing exit threshold. From the other states the aggregate move is
     * computed: any {@link UaqcSignal#NON_CONGESTED} signal can move toward a better state, any
     * {@link UaqcSignal#CONGESTED} signal moves toward a worse state, and all {@link UaqcSignal#HOLD} keeps
     * the state; the per state rules bound the legal targets. A transition in any state other than probing
     * is suppressed when less than the configured hesitation has elapsed since the last transition.
     *
     * @param plrSignal  the packet loss verdict
     * @param rttSignal  the round trip verdict
     * @param rembSignal the receiver estimate verdict
     * @param rembBps    the latest receiver estimate level for the probing exit test
     * @param nowMs      the current time, in milliseconds, for the hesitation gate
     */
    private void applyTransition(UaqcSignal plrSignal, UaqcSignal rttSignal, UaqcSignal rembSignal,
                                 long rembBps, long nowMs) {
        if (state == UaqcState.PROBING) {
            if (rembBps >= config.probingExitRembBps()) {
                transitionTo(UaqcState.BANDWIDTH_MANAGED, nowMs);
            }
            return;
        }

        if (config.transitionHesitationMs() > 0
                && lastTransitionMs != Long.MIN_VALUE
                && nowMs - lastTransitionMs < config.transitionHesitationMs()) {
            return;
        }

        var anyNonCongested = plrSignal == UaqcSignal.NON_CONGESTED
                || rttSignal == UaqcSignal.NON_CONGESTED
                || rembSignal == UaqcSignal.NON_CONGESTED;
        var anyCongested = plrSignal == UaqcSignal.CONGESTED
                || rttSignal == UaqcSignal.CONGESTED
                || rembSignal == UaqcSignal.CONGESTED;

        switch (state) {
            case HIGH_QUALITY -> {
                if (anyCongested) {
                    transitionTo(UaqcState.BANDWIDTH_MANAGED, nowMs);
                }
            }
            case BANDWIDTH_MANAGED -> {
                if (plrSignal == UaqcSignal.CONGESTED && rttSignal != UaqcSignal.CONGESTED) {
                    transitionTo(UaqcState.LOSSY, nowMs);
                } else if (anyCongested) {
                    transitionTo(UaqcState.DRAIN, nowMs);
                } else if (anyNonCongested) {
                    transitionTo(UaqcState.HIGH_QUALITY, nowMs);
                }
            }
            case LOSSY -> {
                if (rttSignal == UaqcSignal.CONGESTED || rembSignal == UaqcSignal.CONGESTED) {
                    transitionTo(UaqcState.DRAIN, nowMs);
                } else if (plrSignal == UaqcSignal.NON_CONGESTED) {
                    transitionTo(UaqcState.BANDWIDTH_MANAGED, nowMs);
                }
            }
            case DRAIN -> {
                if (anyCongested) {
                    transitionTo(UaqcState.ULTRA_LOW_BANDWIDTH, nowMs);
                } else if (anyNonCongested) {
                    transitionTo(UaqcState.BANDWIDTH_MANAGED, nowMs);
                }
            }
            case ULTRA_LOW_BANDWIDTH -> {
                if (anyNonCongested && !anyCongested) {
                    transitionTo(UaqcState.DRAIN, nowMs);
                }
            }
            case PROBING -> {
            }
        }
    }

    /**
     * Moves the machine to the given state and records the transition time for the hesitation gate.
     *
     * @param next  the state to move to
     * @param nowMs the current time, in milliseconds
     */
    private void transitionTo(UaqcState next, long nowMs) {
        if (next == state) {
            return;
        }
        var previous = state;
        state = next;
        lastTransitionMs = nowMs;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "uaqc state {0} -> {1}", previous, next);
        }
    }

    /**
     * Recomputes the probing forward error correction overhead from the smoothed loss.
     *
     * <p>While probing, the overhead is funded in proportion to the loss up to the configured probing
     * overhead percentage; a higher loss buys more redundancy.
     */
    private void updateProbingFec() {
        var fraction = plrEma * (config.probingFecOverheadPct() / 100.0);
        fecOverheadFraction = Math.clamp(fraction, 0.0, config.probingFecOverheadPct() / 100.0);
    }
}
