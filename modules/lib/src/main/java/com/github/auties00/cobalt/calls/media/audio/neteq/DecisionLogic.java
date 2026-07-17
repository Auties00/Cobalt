package com.github.auties00.cobalt.calls.media.audio.neteq;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Chooses the {@link NetEqOperation} for each playback pull from the buffer level against the target, the
 * availability of the next packet, and the previous operation.
 *
 * <p>On every get period {@link #decide(Input)} maps the current state onto exactly one operation. When the
 * next packet continues the sequence, the choice depends on how the buffered span sits against two limits
 * derived from the {@link DelayManager} target: a low limit at
 * {@link NetEqConfig#audioJitbufBufferLowerLimitScalePercent()} of the target, and a high limit one
 * {@link NetEqConfig#audioJitbufBufferLimitsWindowSizeMs()} packet window above the target plus the flat
 * {@link NetEqConfig#highThresholdOffsetMs()} offset. A span at or above the high limit compresses the audio
 * in time ({@link NetEqOperation#ACCELERATE}, or {@link NetEqOperation#FAST_ACCELERATE} when grossly
 * overfull); a span below the low limit stretches it in time ({@link NetEqOperation#PREEMPTIVE_EXPAND}); a
 * span between the limits decodes normally ({@link NetEqOperation#NORMAL}); and the first decode after a
 * concealment run blends the new packet in ({@link NetEqOperation#MERGE}). Time stretch is itself gated:
 * accelerate is taken only when {@link NetEqConfig#allowTimeStretchAcceleration()} is set, and both
 * accelerate and preemptive expand are suppressed unless the buffered span exceeds
 * {@link NetEqConfig#allowTimeStretchThresholdMs()} when {@link NetEqConfig#allowTimeStretchForHighLatency()}
 * is set. When the next packet is missing the choice is concealment: {@link NetEqOperation#CODEC_PLC} if the
 * codec exposes loss concealment and the configuration prefers it, otherwise {@link NetEqOperation#EXPAND};
 * if the buffer is entirely empty and a comfort noise gap is in effect the choice is
 * {@link NetEqOperation#RFC3389_CNG}.
 *
 * <p>The logic holds no state of its own beyond a warmup counter: it forces {@link NetEqOperation#NORMAL}
 * for the first {@link NetEqConfig#numInitialPackets()} decodes so the buffer fills before adaptive time
 * stretching begins. Instances are not thread safe; the pull path drives one decision logic from a single
 * thread.
 *
 * @implNote This implementation forces {@link NetEqOperation#NORMAL} for the first
 * {@link NetEqConfig#numInitialPackets()} pulls so the buffer fills before adaptive time stretching, then
 * compares the buffered span against two limits each get period to select normal decode, accelerate, or
 * preemptive expand. The low limit is {@link NetEqConfig#audioJitbufBufferLowerLimitScalePercent()} (75) of
 * the target; the high limit is the larger of the target and the low limit plus one
 * {@link NetEqConfig#audioJitbufBufferLimitsWindowSizeMs()} (20) packet window, offset by
 * {@link NetEqConfig#highThresholdOffsetMs()} (20). Time stretch is gated below
 * {@link NetEqConfig#allowTimeStretchThresholdMs()} (40) when {@link NetEqConfig#allowTimeStretchForHighLatency()}
 * is set.
 */
public final class DecisionLogic {
    /**
     * The multiple of the high decision limit at or above which the aggressive fast accelerate is chosen.
     *
     * <p>A buffered span at least this multiple of the high limit is grossly overfull and warrants
     * compressing more than one period per frame. Applied only when accelerate itself is permitted.
     */
    private static final int FAST_ACCELERATE_HIGH_LIMIT_MULTIPLE = 2;

    /**
     * The denominator the lower limit scale percentage is taken over.
     */
    private static final int PERCENT = 100;

    /**
     * The logger for {@link DecisionLogic}.
     */
    private static final System.Logger LOGGER = Log.get(DecisionLogic.class);

    /**
     * The configuration carrying the warmup count, the decision limits, and the codec PLC preference.
     */
    private final NetEqConfig config;

    /**
     * The number of normal decodes performed so far, capped at the warmup count.
     *
     * <p>While below {@link NetEqConfig#numInitialPackets()} the logic forces {@link NetEqOperation#NORMAL}
     * so the buffer fills before adaptive time stretching begins.
     */
    private int decodesPerformed;

    /**
     * Constructs decision logic governed by the given configuration.
     *
     * @param config the configuration carrying the warmup count and codec PLC preference; never
     *               {@code null}
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public DecisionLogic(NetEqConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.decodesPerformed = 0;
    }

    /**
     * The state the decision depends on for one get period.
     *
     * <p>{@link #bufferSpanMillis()} is the buffered playout duration; {@link #targetLevelMillis()} is the
     * {@link DelayManager} target; {@link #nextPacketAvailable()} reports whether any packet is buffered;
     * {@link #nextPacketContiguous()} reports whether the next buffered packet continues the sequence
     * without a gap; {@link #comfortNoiseActive()} reports whether a discontinuous transmission
     * comfort noise gap is in effect; {@link #codecHasPlc()} reports whether the active codec exposes
     * loss concealment; and {@link #lastOperation()} is the operation chosen on the previous pull, which
     * distinguishes a fresh decode from the merge that follows a concealment run.
     *
     * @param bufferSpanMillis     the buffered playout duration in milliseconds
     * @param targetLevelMillis    the target playout level in milliseconds
     * @param nextPacketAvailable  whether any packet is buffered
     * @param nextPacketContiguous whether the next buffered packet continues the sequence without a gap
     * @param comfortNoiseActive   whether a comfort noise gap is in effect
     * @param codecHasPlc          whether the codec exposes its own packet loss concealment
     * @param lastOperation        the operation chosen on the previous pull; never {@code null}
     */
    public record Input(
            int bufferSpanMillis,
            int targetLevelMillis,
            boolean nextPacketAvailable,
            boolean nextPacketContiguous,
            boolean comfortNoiseActive,
            boolean codecHasPlc,
            NetEqOperation lastOperation
    ) {
        /**
         * Validates the input, rejecting a {@code null} previous operation.
         *
         * @throws NullPointerException if {@code lastOperation} is {@code null}
         */
        public Input {
            Objects.requireNonNull(lastOperation, "lastOperation cannot be null");
        }
    }

    /**
     * Returns the operation to render for the given state.
     *
     * <p>Concealment is chosen when no contiguous packet is available: comfort noise for an empty buffer in
     * a discontinuous transmission gap, codec concealment or built in expansion otherwise. With a
     * contiguous packet available, the warmup forces a normal decode, a concealment run is closed with a
     * merge, and the buffered span against the low and high decision limits selects normal decode, accelerate
     * (or fast accelerate), or preemptive expand, subject to the time stretch gates.
     *
     * @param input the state for this get period; never {@code null}
     * @return the chosen operation; never {@code null} and never {@link NetEqOperation#UNDEFINED}
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public NetEqOperation decide(Input input) {
        Objects.requireNonNull(input, "input cannot be null");
        if (!input.nextPacketAvailable() || !input.nextPacketContiguous()) {
            if (!input.nextPacketAvailable() && input.comfortNoiseActive()) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "calls neteq decision: {0}", NetEqOperation.RFC3389_CNG);
                return NetEqOperation.RFC3389_CNG;
            }
            if (input.codecHasPlc() && config.enableCodecPlc()) {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "calls neteq decision: {0}", NetEqOperation.CODEC_PLC);
                return NetEqOperation.CODEC_PLC;
            }
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls neteq decision: {0}", NetEqOperation.EXPAND);
            return NetEqOperation.EXPAND;
        }
        if (decodesPerformed < config.numInitialPackets()) {
            decodesPerformed++;
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "calls neteq decision: {0} (warmup {1}/{2})",
                        NetEqOperation.NORMAL, decodesPerformed, config.numInitialPackets());
            }
            return NetEqOperation.NORMAL;
        }
        if (isConcealment(input.lastOperation())) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls neteq decision: {0}", NetEqOperation.MERGE);
            return NetEqOperation.MERGE;
        }
        var target = Math.max(input.targetLevelMillis(), 1);
        var span = input.bufferSpanMillis();
        var lowLimit = target * config.audioJitbufBufferLowerLimitScalePercent() / PERCENT;
        var highLimit = Math.max(target, lowLimit + config.audioJitbufBufferLimitsWindowSizeMs())
                + config.highThresholdOffsetMs();
        if (config.useMaxDelayInHighThreshold()) {
            highLimit = Math.max(highLimit, config.maxDelayMs());
        }
        // TODO: apply the stable playout deceleration offset on top of the accelerate path
        // TODO: model the group decision logic variant used for bot and AI calls
        if (!timeStretchAllowed(span)) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls neteq decision: {0} (time stretch gated)", NetEqOperation.NORMAL);
            return NetEqOperation.NORMAL;
        }
        if (span >= highLimit) {
            if (!config.allowTimeStretchAcceleration()) {
                if (Log.TRACE) {
                    LOGGER.log(Level.TRACE, "calls neteq decision: {0} (accelerate disallowed)", NetEqOperation.NORMAL);
                }
                return NetEqOperation.NORMAL;
            }
            // TODO: recover the exact fast accelerate span multiple; this uses twice the high limit
            var operation = span >= highLimit * FAST_ACCELERATE_HIGH_LIMIT_MULTIPLE
                    ? NetEqOperation.FAST_ACCELERATE
                    : NetEqOperation.ACCELERATE;
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "calls neteq decision: {0} (span={1}ms highLimit={2}ms)", operation, span, highLimit);
            }
            return operation;
        }
        if (span < lowLimit) {
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "calls neteq decision: {0} (span={1}ms lowLimit={2}ms)",
                        NetEqOperation.PREEMPTIVE_EXPAND, span, lowLimit);
            }
            return NetEqOperation.PREEMPTIVE_EXPAND;
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "calls neteq decision: {0}", NetEqOperation.NORMAL);
        return NetEqOperation.NORMAL;
    }

    /**
     * Returns whether time stretch is permitted for the given buffered span under the high latency gate.
     *
     * <p>When {@link NetEqConfig#allowTimeStretchForHighLatency()} is set, time stretch is permitted only
     * once the buffered span exceeds {@link NetEqConfig#allowTimeStretchThresholdMs()}, so a nearly empty
     * buffer is never compressed or stretched. When the gate is clear, time stretch is always permitted.
     *
     * @param bufferSpanMillis the buffered playout duration in milliseconds
     * @return {@code true} if accelerate and preemptive expand may be chosen for this span
     */
    private boolean timeStretchAllowed(int bufferSpanMillis) {
        if (!config.allowTimeStretchForHighLatency()) {
            return true;
        }
        return bufferSpanMillis > config.allowTimeStretchThresholdMs();
    }

    /**
     * Resets the warmup counter so the next decodes refill the buffer before adaptive time stretching.
     *
     * <p>Used when the stream is reconfigured or the buffer is flushed.
     */
    public void reset() {
        decodesPerformed = 0;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls neteq decision logic reset");
    }

    /**
     * Returns whether the given operation is a concealment operation that a merge must follow.
     *
     * @param operation the operation to classify
     * @return {@code true} if the operation concealed a gap
     */
    private static boolean isConcealment(NetEqOperation operation) {
        return operation == NetEqOperation.EXPAND
                || operation == NetEqOperation.CODEC_PLC
                || operation == NetEqOperation.RFC3389_CNG
                || operation == NetEqOperation.CODEC_INTERNAL_CNG;
    }
}
