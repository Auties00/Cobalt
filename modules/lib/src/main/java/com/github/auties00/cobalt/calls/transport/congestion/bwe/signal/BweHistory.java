package com.github.auties00.cobalt.calls.transport.congestion.bwe.signal;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Holds a per call ring of recent bandwidth estimate samples used to seed a new call's estimate and to
 * decide whether the selective forwarding unit fast probe mode should run.
 *
 * <p>Across a call the transport records the bandwidth estimate it observes; this ring keeps the most
 * recent {@value #CAPACITY} samples, in kilobits per second, overwriting the oldest once full. Two
 * consumers read it: a newly created transport seeds the peer initial bandwidth from
 * {@link #seedEstimateKbps()} so a reconnect does not restart from the conservative floor, and the
 * relay setup calls {@link #shouldActivateSfuFastProbeMode()} to decide whether to probe the link
 * aggressively, which it does when the recent history is empty or weak enough that the slow ramp would
 * be too cautious.
 *
 * <p>The ring is not thread safe; the single call transport thread records and reads it.
 *
 * @implNote This implementation models the estimate history as a bounded ring of the most recent
 *           {@value #CAPACITY} samples, whose mean seeds a fresh estimate and whose recent level gates
 *           fast probe mode. {@link #FAST_PROBE_THRESHOLD_KBPS} and {@link #INSTANT_RAMP_UP_RATIO} are
 *           the compiled voip parameter defaults for the history based bandwidth estimator.
 */
public final class BweHistory {
    /**
     * The logger for {@link BweHistory}.
     */
    private static final System.Logger LOGGER = Log.get(BweHistory.class);

    /**
     * The number of recent samples the ring retains.
     */
    // TODO: the native design (wa_calling_history.cc) is not this per-call ring but a persistent key-value
    //  record store (wa_storage) of about ten record types (detailed_transport, uaqc, video, group_call,
    //  calling_audio, audio_device_restart, dial, offer_peek, call_state_v1), each keyed by
    //  call_self_ip_str_hashed + call_peer_ip_str_hashed and matched on self-IP + peer-IP + p2p status,
    //  trimmed oldest-first (by a 64-bit timestamp) on save to a max record count. That cap is a runtime
    //  config field with no compiled-in default and no *_max_records voip param to recover, so this depth of
    //  16 is a Cobalt stand-in, not a value read from WhatsApp. A faithful port is a whole persistent
    //  QoE-history subsystem gated by the history_based_bwe_enabled voip setting (off for group calls) and
    //  wired to roughly twenty history/ramp voip params (history_based_bwe_*, fr_hbwe_*, rbe_instant_ramp_*,
    //  instant_ramp_up_min_bps/target_bps), so it also depends on the compressed voip_settings decode.
    //  Deliberately unbuilt for a library: the payoff (faster bitrate convergence in the first seconds of a
    //  repeat call over the same network path) does not justify a persistent per-endpoint store, and the
    //  deciding constants (this cap and the two gate thresholds below) are not statically recoverable.
    public static final int CAPACITY = 16;

    /**
     * The bandwidth level, in kilobits per second, at or below which fast probe mode is worthwhile.
     *
     * <p>When the most recent sample is missing or no greater than this level the slow additive ramp
     * would take too long to reach a usable rate, so the selective forwarding unit is asked to fast
     * probe instead.
     *
     * @implNote This implementation uses {@code 500}, the compiled default of the voip parameter
     * {@code history_based_bwe_instant_ramp_up_threshold} ({@code 500000} bits per second) expressed in
     * kilobits per second. In the engine this threshold gates the history based instant ramp up, which
     * fires when the history based estimate scaled by {@link #INSTANT_RAMP_UP_RATIO} exceeds it; this
     * ring uses it as the fast probe floor.
     */
    public static final int FAST_PROBE_THRESHOLD_KBPS = 500;

    /**
     * The ratio the history based estimate is scaled by when it seeds a fresh call's bandwidth.
     *
     * @implNote This implementation uses {@code 1.0}, the compiled default of the voip parameter
     * {@code history_based_bwe_instant_ramp_up_ratio} (the {@code 1.0f} bit pattern). A ratio of
     * {@code 1.0} leaves the history based estimate unscaled when it seeds a new call.
     */
    public static final double INSTANT_RAMP_UP_RATIO = 1.0;

    /**
     * Holds the recent samples in a circular buffer, in kilobits per second.
     *
     * <p>Indices in {@code [0, count)} after wrap are addressed modulo {@value #CAPACITY} from
     * {@code head}; before wrap the live samples are {@code [0, count)} directly.
     */
    private final int[] samples;

    /**
     * Holds the index at which the next sample is written, modulo {@value #CAPACITY}.
     */
    private int head;

    /**
     * Holds the number of live samples, saturating at {@value #CAPACITY}.
     */
    private int count;

    /**
     * Constructs an empty history with no recorded samples.
     */
    public BweHistory() {
        this.samples = new int[CAPACITY];
        this.head = 0;
        this.count = 0;
    }

    /**
     * Records one bandwidth estimate sample, overwriting the oldest when the ring is full.
     *
     * <p>A sample that is not strictly positive is ignored, treated as a missing measurement rather than
     * a data point.
     *
     * @param estimateKbps the observed bandwidth estimate in kilobits per second; ignored when not
     *                     strictly positive
     */
    public void record(int estimateKbps) {
        if (estimateKbps <= 0) {
            return;
        }
        samples[head] = estimateKbps;
        head = (head + 1) % CAPACITY;
        if (count < CAPACITY) {
            count++;
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "bwe history sample recorded: {0}kbps count={1}", estimateKbps, count);
        }
    }

    /**
     * Returns the bandwidth estimate, in kilobits per second, to seed a fresh transport with.
     *
     * <p>This is the arithmetic mean of the retained samples scaled by {@link #INSTANT_RAMP_UP_RATIO},
     * which smooths a single outlier while still reflecting the call's recent capacity. It is {@code 0}
     * when no sample has been recorded, in which case the transport falls back to its own conservative
     * initial estimate.
     *
     * @implNote This implementation scales the history mean by {@link #INSTANT_RAMP_UP_RATIO}, the
     * compiled {@code history_based_bwe_instant_ramp_up_ratio} default ({@code 1.0f}), matching the
     * engine's history based instant ramp up, which sets the new sender bandwidth to the history based
     * estimate times that ratio. At the compiled default of {@code 1.0} the mean is seeded unscaled.
     *
     * @return the ratio scaled mean of the retained samples, or {@code 0} when the history is empty
     */
    public int seedEstimateKbps() {
        if (count == 0) {
            return 0;
        }
        var total = 0L;
        for (var index = 0; index < count; index++) {
            total += samples[index];
        }
        var seed = (int) (total / count * INSTANT_RAMP_UP_RATIO);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "bwe history seed estimate: {0}kbps from {1} samples", seed, count);
        }
        return seed;
    }

    /**
     * Returns the most recently recorded sample, in kilobits per second.
     *
     * @return the last recorded sample, or {@code 0} when the history is empty
     */
    public int mostRecentKbps() {
        if (count == 0) {
            return 0;
        }
        var lastIndex = (head - 1 + CAPACITY) % CAPACITY;
        return samples[lastIndex];
    }

    /**
     * Returns whether the selective forwarding unit fast probe mode should run for this call.
     *
     * <p>Fast probing is activated when the history is empty (a first call, with no capacity to seed
     * from) or when the most recent sample is at or below {@value #FAST_PROBE_THRESHOLD_KBPS}, where the
     * slow additive ramp would be too cautious to reach a usable rate promptly.
     *
     * @return {@code true} when the selective forwarding unit should fast probe rather than slowly ramp
     *         the estimate
     */
    // TODO: the native fast-probe gate (call_bwe_history.cc maybe_process_history_based_bwe) does not test a
    //  single kbps ring against one threshold; it walks up to 32 per-record structs drawn from the
    //  persistent history store (see CAPACITY) and compares each record's gating value against two float
    //  thresholds carried on the call instance. The history/ramp param family is known
    //  (history_based_bwe_instant_ramp_up_{threshold,ratio}, instant_ramp_up_min_bps/target_bps, fr_hbwe_*),
    //  but which two the walk uses and their per-call values are initialised at setup and only observable
    //  from a live memory dump of a connected call, so this empty-or-weak history gate stands in for that
    //  walk as the closest reproducible behaviour and is not byte faithful.
    public boolean shouldActivateSfuFastProbeMode() {
        if (count == 0) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "sfu fast probe mode activated: no history");
            }
            return true;
        }
        var recent = mostRecentKbps();
        var activate = recent <= FAST_PROBE_THRESHOLD_KBPS;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "sfu fast probe decision: activate={0} recent={1}kbps threshold={2}kbps",
                    activate, recent, FAST_PROBE_THRESHOLD_KBPS);
        }
        return activate;
    }

    /**
     * Returns the number of live samples in the history.
     *
     * @return the sample count, in {@code [0, }{@value #CAPACITY}{@code ]}
     */
    public int size() {
        return count;
    }

    /**
     * Removes all recorded samples, returning the history to its empty state.
     */
    public void clear() {
        head = 0;
        count = 0;
    }
}
