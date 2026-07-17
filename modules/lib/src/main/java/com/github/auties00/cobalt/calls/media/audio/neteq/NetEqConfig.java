package com.github.auties00.cobalt.calls.media.audio.neteq;

/**
 * Holds the tuning knobs that govern the {@link LiveNetEq} adaptive jitter buffer.
 *
 * <p>The configuration is immutable and is installed once when a call's audio receive path opens its
 * jitter buffer. It collects four groups of knobs. The playout delay bounds and offsets
 * ({@link #minDelayMs()}, {@link #maxDelayMs()}, {@link #delayOffsetMs()}, {@link #targetDelayMs()},
 * {@link #initMinE2eDelayMs()}) clamp and bias the target buffer level the {@link DelayManager}
 * estimates. The histogram and estimator parameters ({@link #dmHistorySizeMs()},
 * {@link #dlHistorySizeMs()}, {@link #maxHistoryMs()}, {@link #underrunQuantile()},
 * {@link #underrunForgetFactor()}, {@link #reorderForgetFactor()}, {@link #reorderStartForgetWeight()})
 * drive the inter arrival time histogram and its forgetting. The peak detector and buffer management
 * flags ({@link #enablePeakDetector()}, {@link #smartBufferFlushEnabled()},
 * {@link #bufferFlushMaxLengthMs()}, {@link #maxPacketsInBuffer()}, {@link #use20msGetPeriod()},
 * {@link #numInitialPackets()}) shape the decision logic. The accelerate and preemptive expand decision
 * limits ({@link #audioJitbufBufferLowerLimitScalePercent()},
 * {@link #audioJitbufBufferLimitsWindowSizeMs()}, {@link #highThresholdOffsetMs()},
 * {@link #useMaxDelayInHighThreshold()}, {@link #allowTimeStretchAcceleration()},
 * {@link #allowTimeStretchForHighLatency()}, {@link #allowTimeStretchThresholdMs()}) set the low and high
 * buffer level thresholds the {@link DecisionLogic} compares the buffered span against, and gate whether the
 * buffer is time compressed or time stretched at all. The NACK and concealment behaviour
 * ({@link #enableCodecPlc()}, {@link #preexpandWithFilteredLevelPerc()}, {@link #skipNackWithFec()},
 * {@link #ladEnabledForNack()}, {@link #ladEnabledForFec()}, {@link #ladNackExtraInsertTimeMs()},
 * {@link #nackRttLimitMs()}, {@link #maxNackListSize()}, {@link #audioNackMaxSeqReq()},
 * {@link #enableSpeakerStatus()}) wires the {@link NackTracker} and the loss recovery path.
 *
 * <p>Obtain the production configuration from {@link #defaults()}, which carries the WhatsApp
 * {@code voip_settings} value for every key the server pushes and the upstream default for the keys the
 * server does not push.
 *
 * @param minDelayMs                     the lower bound on the target playout delay in milliseconds
 * @param maxDelayMs                     the upper bound on the target playout delay in milliseconds
 * @param delayOffsetMs                  a signed bias added to the estimated target playout delay
 * @param targetDelayMs                  an explicit target playout delay override, or {@code 0} for none
 * @param initMinE2eDelayMs              the initial minimum end to end delay seeded before estimation
 * @param dmHistorySizeMs               the {@link DelayManager} inter arrival histogram window
 * @param dlHistorySizeMs               the {@link DecisionLogic} buffer level history window
 * @param maxHistoryMs                   the overall cap on histogram history retained
 * @param underrunQuantile               the histogram quantile the target level is read at
 * @param underrunForgetFactor           the exponential forgetting factor of the underrun estimator
 * @param reorderForgetFactor            the forgetting factor applied to reordered arrival deviations
 * @param reorderStartForgetWeight       the initial weight the reorder optimization forgetting ramps from
 * @param enablePeakDetector             whether the inter arrival peak detector raises the delay floor
 * @param smartBufferFlushEnabled        whether gross over buffering is drained by a smart flush
 * @param bufferFlushMaxLengthMs         the buffer length above which a flush is forced
 * @param maxPacketsInBuffer             the {@link PacketBuffer} capacity in packets
 * @param use20msGetPeriod               whether the get period is 20 ms rather than 10 ms
 * @param numInitialPackets              the warm up packet count before normal decisions begin
 * @param audioJitbufBufferLowerLimitScalePercent the percentage of the target level the low decision limit
 *                                       sits at, below which the buffer is preemptively expanded
 * @param audioJitbufBufferLimitsWindowSizeMs the window in milliseconds added above the low limit to form
 *                                       the high decision limit, above which the buffer is accelerated
 * @param highThresholdOffsetMs          a flat millisecond offset added to the high decision limit
 * @param useMaxDelayInHighThreshold     whether the maximum delay bound also floors the high decision limit
 * @param allowTimeStretchAcceleration   whether accelerate (time compression) is permitted at all
 * @param allowTimeStretchForHighLatency whether time stretch is gated on the buffer exceeding the
 *                                       high latency threshold
 * @param allowTimeStretchThresholdMs    the buffered span in milliseconds above which time stretch is
 *                                       permitted when gated by {@link #allowTimeStretchForHighLatency()}
 * @param enableCodecPlc                 whether codec internal loss concealment is preferred over expand
 * @param preexpandWithFilteredLevelPerc the smoothed level percentage used before an expansion
 * @param skipNackWithFec                whether a packet a FEC copy will recover is excluded from NACK
 * @param ladEnabledForNack              whether lost audio detection extends NACK timing
 * @param ladEnabledForFec               whether lost audio detection extends FEC timing
 * @param ladNackExtraInsertTimeMs       the extra insert time lost audio detection grants a NACK
 * @param nackRttLimitMs                 the round trip time ceiling above which NACK is suppressed
 * @param maxNackListSize                the hard cap on the {@link NackTracker} list length
 * @param audioNackMaxSeqReq             the maximum sequence number span a single NACK request covers
 * @param enableSpeakerStatus            whether speaker status change events are emitted from the buffer
 * @implNote This implementation splits its defaults across three sources. Keys the WhatsApp server pushes
 * in {@code voip_settings} take the server value: {@code neteq_init_min_e2e_delay_ms=280},
 * {@code neteq_delay_offset_ms=-50}, {@code neteq_dm_history_size_ms=500},
 * {@code neteq_dl_history_size_ms=500}, {@code neteq_underrun_quantile=0.98},
 * {@code neteq_underrun_forget_factor=0.99}, {@code neteq_enable_peak_detector=false},
 * {@code neteq_smart_buffer_flush_enabled=true}, {@code buffer_flush_max_length_ms=1000},
 * {@code neteq_enable_codec_plc=true}, {@code neteq_max_delay=500}, {@code neteq_use_20ms_get_period=true},
 * {@code neteq_preexpand_with_filtered_level_perc=50}, {@code neteq_skip_nack_with_fec=false},
 * {@code neteq_lad_enabled_for_nack=true}, {@code neteq_lad_enabled_for_fec=true},
 * {@code neteq_lad_nack_extra_insert_time_ms=20}, {@code neteq_nack_rtt_limit_ms=500},
 * {@code neteq_enable_speaker_status=true}, {@code audio_nack_max_seq_req=10}, and the time stretch gates
 * {@code neteq_allow_time_stretch_acceleration=false} (accelerate is suppressed in production),
 * {@code neteq_allow_time_stretch_for_high_latency=true}, {@code neteq_allow_time_stretch_threshold_ms=40}.
 * The accelerate and preemptive expand decision limits are not pushed by the server and take the upstream
 * defaults: {@code audio_jitbuf_buffer_lower_limit_scale_percent=75} (the low limit is 75% of the target),
 * {@code audio_jitbuf_buffer_limits_window_size_ms=20} (the high limit is the target plus this one packet
 * window), {@code high_threshold_offset_ms=20} (a flat offset added to the high limit), and
 * {@code use_max_delay_in_high_threshold=false}. Likewise {@code maxPacketsInBuffer=200} and the reorder
 * optimization decay ({@code reorderForgetFactor=0.9993}, {@code reorderStartForgetWeight=2.0}) take the
 * upstream defaults; the reorder path is itself inert because {@code use_reorder_optimizer} is false. The
 * {@link #maxNackListSize()} cap is fixed at {@link #NACK_LIST_SIZE_LIMIT}. Four further fields are not
 * pushed by the server and keep their upstream default because they tune playout quality rather than
 * interoperability: {@link #minDelayMs()} ({@code 0}), {@link #targetDelayMs()} ({@code 0}),
 * {@link #maxHistoryMs()} ({@code 2000}), and {@link #numInitialPackets()} ({@code 5}).
 */
public record NetEqConfig(
        int minDelayMs,
        int maxDelayMs,
        int delayOffsetMs,
        int targetDelayMs,
        int initMinE2eDelayMs,
        int dmHistorySizeMs,
        int dlHistorySizeMs,
        int maxHistoryMs,
        double underrunQuantile,
        double underrunForgetFactor,
        double reorderForgetFactor,
        double reorderStartForgetWeight,
        boolean enablePeakDetector,
        boolean smartBufferFlushEnabled,
        int bufferFlushMaxLengthMs,
        int maxPacketsInBuffer,
        boolean use20msGetPeriod,
        int numInitialPackets,
        int audioJitbufBufferLowerLimitScalePercent,
        int audioJitbufBufferLimitsWindowSizeMs,
        int highThresholdOffsetMs,
        boolean useMaxDelayInHighThreshold,
        boolean allowTimeStretchAcceleration,
        boolean allowTimeStretchForHighLatency,
        int allowTimeStretchThresholdMs,
        boolean enableCodecPlc,
        int preexpandWithFilteredLevelPerc,
        boolean skipNackWithFec,
        boolean ladEnabledForNack,
        boolean ladEnabledForFec,
        int ladNackExtraInsertTimeMs,
        int nackRttLimitMs,
        int maxNackListSize,
        int audioNackMaxSeqReq,
        boolean enableSpeakerStatus
) {
    /**
     * The hard cap on the {@link NackTracker} list length.
     *
     * <p>A NACK list size is valid only when it is greater than zero and no larger than this value; the
     * compact constructor rejects any {@link #maxNackListSize()} outside that range.
     */
    public static final int NACK_LIST_SIZE_LIMIT = 500;

    /**
     * The duration of one packet in milliseconds at the 20 ms get period, the unit the histogram counts
     * inter arrival deviations in.
     *
     * <p>The production configuration sets {@link #use20msGetPeriod()}, so the buffer level and the target
     * level are denominated in 20 ms packet units.
     */
    public static final int PACKET_MILLIS = 20;

    /**
     * Validates the bounds, quantile, and cap fields, rejecting any inconsistent tuning.
     *
     * @throws IllegalArgumentException if {@code minDelayMs} is negative, if {@code maxDelayMs} is below
     *                                  {@code minDelayMs}, if {@code underrunQuantile} is outside the open
     *                                  interval {@code (0, 1)}, if {@code maxPacketsInBuffer} is not
     *                                  positive, if {@code audioJitbufBufferLowerLimitScalePercent} is not
     *                                  positive, or if {@code maxNackListSize} is outside
     *                                  {@code 1..}{@link #NACK_LIST_SIZE_LIMIT}
     */
    public NetEqConfig {
        if (minDelayMs < 0) {
            throw new IllegalArgumentException("minDelayMs must be non-negative, got " + minDelayMs);
        }
        if (maxDelayMs < minDelayMs) {
            throw new IllegalArgumentException("maxDelayMs " + maxDelayMs + " is below minDelayMs " + minDelayMs);
        }
        if (underrunQuantile <= 0.0 || underrunQuantile >= 1.0) {
            throw new IllegalArgumentException("underrunQuantile must be in (0, 1), got " + underrunQuantile);
        }
        if (maxPacketsInBuffer <= 0) {
            throw new IllegalArgumentException("maxPacketsInBuffer must be positive, got " + maxPacketsInBuffer);
        }
        if (audioJitbufBufferLowerLimitScalePercent <= 0) {
            throw new IllegalArgumentException("audioJitbufBufferLowerLimitScalePercent must be positive, got "
                    + audioJitbufBufferLowerLimitScalePercent);
        }
        if (maxNackListSize < 1 || maxNackListSize > NACK_LIST_SIZE_LIMIT) {
            throw new IllegalArgumentException("maxNackListSize must be in 1.." + NACK_LIST_SIZE_LIMIT
                    + ", got " + maxNackListSize);
        }
    }

    /**
     * Returns the production configuration used by a call's audio receive path.
     *
     * <p>Fields the WhatsApp server pushes in {@code voip_settings} take the server value, the remaining
     * fields take their upstream default because the server does not push them (see the class
     * {@link NetEqConfig} implementation note for the full split). The result enables the codec packet loss
     * concealment path, disables the peak detector, runs the 20 ms get period, suppresses accelerate, and
     * caps the NACK list at {@link #NACK_LIST_SIZE_LIMIT}.
     *
     * @return the default jitter buffer configuration
     */
    public static NetEqConfig defaults() {
        return new NetEqConfig(
                // neteq_min_delay: not pushed by the server; upstream default is no forced floor.
                0,
                500,   // neteq_max_delay
                -50,   // neteq_delay_offset_ms
                // target_delay_ms: not pushed by the server; upstream default estimates without an override.
                0,
                280,   // neteq_init_min_e2e_delay_ms
                500,   // neteq_dm_history_size_ms
                500,   // neteq_dl_history_size_ms
                // max_history_ms: not pushed by the server; upstream histogram cap default.
                2000,
                0.98,  // neteq_underrun_quantile
                0.99,  // neteq_underrun_forget_factor
                // reorder_forget_factor: upstream default; the reorder path is inert because
                //  use_reorder_optimizer is false.
                0.9993,
                // reorder_start_forget_weight: upstream default; inert while use_reorder_optimizer is false.
                2.0,
                false, // neteq_enable_peak_detector
                true,  // neteq_smart_buffer_flush_enabled
                1000,  // buffer_flush_max_length_ms
                // max_packets_in_buffer: not pushed by the server; upstream default.
                200,
                true,  // neteq_use_20ms_get_period
                // num_initial_packets: not pushed by the server; upstream default of 5 warm up packets.
                5,
                // audio_jitbuf_buffer_lower_limit_scale_percent: not pushed by the server; upstream default.
                75,
                // audio_jitbuf_buffer_limits_window_size_ms: not pushed by the server; upstream default.
                20,
                // high_threshold_offset_ms: not pushed by the server; upstream default.
                20,
                // use_max_delay_in_high_threshold: not pushed by the server; upstream default.
                false,
                false, // neteq_allow_time_stretch_acceleration
                true,  // neteq_allow_time_stretch_for_high_latency
                40,    // neteq_allow_time_stretch_threshold_ms
                true,  // neteq_enable_codec_plc
                50,    // neteq_preexpand_with_filtered_level_perc
                false, // neteq_skip_nack_with_fec
                true,  // neteq_lad_enabled_for_nack
                true,  // neteq_lad_enabled_for_fec
                20,    // neteq_lad_nack_extra_insert_time_ms
                500,   // neteq_nack_rtt_limit_ms
                NACK_LIST_SIZE_LIMIT,
                10,    // audio_nack_max_seq_req
                true   // neteq_enable_speaker_status
        );
    }

    /**
     * Returns the get period in milliseconds, either 20 ms or 10 ms.
     *
     * <p>The value is derived from {@link #use20msGetPeriod()}; one playback pull renders one frame of this
     * duration.
     *
     * @return the get period, {@code 20} when {@link #use20msGetPeriod()} is set, otherwise {@code 10}
     */
    public int getPeriodMs() {
        return use20msGetPeriod ? 20 : 10;
    }
}
