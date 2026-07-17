package com.github.auties00.cobalt.calls.transport.congestion.ratecontrol;

import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusCodecParams;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusInbandFecPacker;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Turns the combined bandwidth estimate target into the audio encoder's bitrate, packet loss
 * percentage, and forward error correction settings, gated by the unified audio quality state.
 *
 * <p>Each {@link #apply(long, double, double, long, OpusCodecParams)} round takes the combiner's target
 * bitrate for the audio stream, deducts the {@linkplain #transportOverheadBps() transport overhead}, then
 * advances the {@link UnifiedAudioQualityControl} with the latest loss, round trip, and receiver estimate
 * measurements. The resulting quality state and its forward error correction overhead decide how much of
 * the budget funds redundancy: while {@link UaqcState#PROBING} a fraction of the target is reserved for
 * in band forward error correction and the codec bitrate is reduced accordingly; in the steady state the
 * full budget goes to the codec. The expected loss percentage the codec uses to size its in band
 * redundancy tracks the measured loss. The method returns an updated {@link OpusCodecParams} carrying the
 * new bitrate, loss percentage, and forward error correction flag, clamped to the codec's configured
 * bitrate floor and ceiling.
 *
 * <p>The controller holds the quality control across rounds and is otherwise stateless. It holds no
 * clock; the caller supplies a current time reading each round. Instances are not thread safe; the single
 * rate control thread that owns one drives all rounds.
 *
 * @implNote This implementation reserves part of the audio budget for in band forward error correction
 * only while the quality control is probing; in the steady state the whole budget funds the codec. The
 * transport overhead deducted from the target is always {@code 0} in this build (see
 * {@link #transportOverheadBps()}), so the audio target reaches the codec unmodified. The codec bitrate
 * floor and ceiling that clamp the result come from the supplied {@link OpusCodecParams}.
 */
public final class AudioRateController {
    /**
     * The logger for {@link AudioRateController}.
     */
    private static final System.Logger LOGGER = Log.get(AudioRateController.class);

    /**
     * The unified audio quality control whose state gates the forward error correction budget.
     */
    private final UnifiedAudioQualityControl qualityControl;

    /**
     * The in band forward error correction policy object the encoder loss percentage is routed through.
     *
     * <p>{@link OpusInbandFecPacker#encoderPacketLossPercent(int)} is a pure clamp policy applied before
     * the codec's packet loss setting, so a single shared instance carries no per call state.
     */
    private final OpusInbandFecPacker fecPacker = new OpusInbandFecPacker();

    /**
     * Constructs an audio rate controller over the given quality control.
     *
     * @param qualityControl the unified audio quality control; never {@code null}
     */
    public AudioRateController(UnifiedAudioQualityControl qualityControl) {
        this.qualityControl = qualityControl;
    }

    /**
     * Constructs an audio rate controller with a default configured quality control.
     */
    public AudioRateController() {
        this(new UnifiedAudioQualityControl(UnifiedAudioQualityControl.Config.defaults()));
    }

    /**
     * Computes the audio encoder settings for the combined target and the latest network measurements.
     *
     * <p>Deducts the transport overhead from the target, advances the quality control, splits off the
     * forward error correction budget while probing, sets the expected loss percentage from the measured
     * loss, and returns the supplied parameters with the new bitrate, loss percentage, and forward error
     * correction flag applied and clamped to the codec's bitrate window.
     *
     * @param combinedTargetBps the combiner's target bitrate for the audio stream, in bits per second
     * @param plr               the measured packet loss ratio over the recent window, in {@code [0, 1]}
     * @param rttMs             the latest round trip time sample in milliseconds
     * @param rembBps           the latest receiver estimated maximum bitrate, in bits per second
     * @param params            the current codec parameters to retarget; never {@code null}
     * @return the audio rate result carrying the updated parameters and the chosen quality state
     */
    public AudioRateResult apply(long combinedTargetBps, double plr, double rttMs, long rembBps,
                                 OpusCodecParams params) {
        var nowMs = System.nanoTime() / 1_000_000L;
        var state = qualityControl.update(plr, rttMs, rembBps, nowMs);

        var overheadBps = transportOverheadBps();
        var budgetBps = Math.max(params.minBitrate(), combinedTargetBps - overheadBps);

        var fecFraction = qualityControl.fecOverheadFraction();
        var codecBps = (long) (budgetBps * (1.0 - fecFraction));
        codecBps = Math.clamp(codecBps, params.minBitrate(), params.maxBitrate());

        var lossPercent = fecPacker.encoderPacketLossPercent((int) Math.round(plr * 100.0));
        var fecEnabled = fecFraction > 0.0 || lossPercent > 0;

        var updated = new OpusCodecParams(params.sampleRate(), params.channels(), params.application(),
                (int) codecBps, params.minBitrate(), params.maxBitrate(), params.variableBitrate(),
                params.maxBandwidth(), params.complexity(), fecEnabled, lossPercent,
                params.discontinuousTransmission(), params.forceChannels(), params.signalVoice(),
                params.lsbDepth(), params.framesPerPacket(), params.frameMillis());
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "audio rate applied: target={0}bps codec={1}bps state={2} fec={3} lossPercent={4}",
                    combinedTargetBps, codecBps, state, fecEnabled, lossPercent);
        }
        return new AudioRateResult(updated, state, fecFraction);
    }

    /**
     * Returns the current quality state of the unified audio quality control.
     *
     * @return the current {@link UaqcState}
     */
    public UaqcState state() {
        return qualityControl.state();
    }

    /**
     * Returns the transport overhead bitrate the audio target is reduced by before it reaches the codec,
     * in bits per second.
     *
     * @implNote This implementation returns {@code 0}. The deduction is gated on a strictly positive
     * overhead value ({@code 0 < value}, then {@code target -= value}), and that value is never set non
     * zero in this build, so the gate is always false and the audio target reaches the codec unmodified.
     *
     * @return {@code 0}, the bits per second overhead deducted from the audio target
     */
    private long transportOverheadBps() {
        return 0;
    }

    /**
     * The outcome of one audio rate control round: the retargeted codec parameters, the quality state,
     * and the forward error correction overhead applied.
     *
     * <p>The {@link #params()} are ready to hand to a codec reconfigure; the {@link #state()} and
     * {@link #fecOverheadFraction()} are surfaced for telemetry and for the redundancy packer.
     *
     * @param params              the updated codec parameters with the new bitrate, loss percentage, and
     *                            forward error correction flag; never {@code null}
     * @param state               the unified audio quality state after this round
     * @param fecOverheadFraction the forward error correction overhead fraction reserved this round, in
     *                            {@code [0, 1]}
     */
    public record AudioRateResult(OpusCodecParams params, UaqcState state, double fecOverheadFraction) {
    }
}
