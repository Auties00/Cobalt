package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import com.github.auties00.cobalt.calls.media.audio.codec.AudioCodec;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Drives the Opus in band forward error correction scheme by mapping observed receive loss onto the
 * encoder's expected loss control and deciding, for each lost frame, whether the decoder conceals the gap
 * or reconstructs it from the in band LBRR copy carried in the following packet.
 *
 * <p>Opus in band FEC embeds a low bitrate (LBRR) copy of the previous frame inside each packet when the
 * encoder runs with FEC enabled and a non zero expected packet loss; the more loss the encoder expects,
 * the more bitrate it spends on that redundancy. This packer is the policy half of that scheme. On the send
 * side {@link #encoderPacketLossPercent(int)} turns a measured loss percentage into the expected loss value
 * the encoder is reconfigured with. On the receive side {@link #shouldDecodeFec(boolean)} decides whether a
 * gap pulls the next packet's LBRR (when that packet has already arrived) or falls back to packet loss
 * concealment. The packer holds no native state; it computes the control values and recovery decisions the
 * codec and receiver act on.
 *
 * <p>Instances are not thread safe; the send path and receive path each drive their own packer on their own
 * thread.
 */
public final class OpusInbandFecPacker {
    /**
     * The lowest expected packet loss percentage libopus accepts.
     */
    private static final int MIN_LOSS_PERCENT = 0;

    /**
     * The highest expected packet loss percentage libopus accepts.
     */
    private static final int MAX_LOSS_PERCENT = 100;

    /**
     * The logger for {@link OpusInbandFecPacker}.
     */
    private static final System.Logger LOGGER = Log.get(OpusInbandFecPacker.class);

    /**
     * Constructs an in band FEC packer.
     *
     * <p>The packer carries no state beyond its policy; one instance serves a stream for its lifetime.
     */
    public OpusInbandFecPacker() {
    }

    /**
     * Maps a measured receive loss percentage onto the encoder's expected packet loss control value.
     *
     * <p>The returned value is what the encoder is reconfigured with through its expected loss control,
     * driving how much LBRR redundancy each packet carries. The measured loss is clamped into the libopus
     * {@code 0..100} range.
     *
     * @implNote This implementation clamps the measured loss into the {@code 0..100} range libopus accepts,
     * bounded by {@link #MIN_LOSS_PERCENT} and {@link #MAX_LOSS_PERCENT}.
     * @param measuredLossPercent the measured receive loss percentage
     * @return the expected packet loss value to apply to the encoder, in {@code 0..100}
     */
    public int encoderPacketLossPercent(int measuredLossPercent) {
        // TODO: WhatsApp derives the encoder's expected loss from a rate controller that combines several
        //  link signals with field trial constants the server does not push; pass the measured loss through
        //  directly until those signals and constants are recovered.
        var clamped = Math.clamp(measuredLossPercent, MIN_LOSS_PERCENT, MAX_LOSS_PERCENT);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "calls opus fec encoder loss percent: measured={0} applied={1}",
                    measuredLossPercent, clamped);
        }
        return clamped;
    }

    /**
     * Decides whether a lost frame is reconstructed from the next packet's in band FEC.
     *
     * <p>In band FEC reconstruction requires the packet that follows the lost frame, since that packet
     * carries the LBRR copy. When that packet has arrived the decoder pulls the FEC; otherwise the gap is
     * concealed. The receiver pairs this decision with {@link AudioCodec#recover(byte[], int)}, passing the
     * next packet to reconstruct or {@code null} to conceal.
     *
     * @param nextPacketAvailable whether the packet following the lost frame has been received
     * @return {@code true} to decode the next packet's FEC, {@code false} to conceal the gap
     */
    public boolean shouldDecodeFec(boolean nextPacketAvailable) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "calls opus fec decode decision: nextPacketAvailable={0}", nextPacketAvailable);
        return nextPacketAvailable;
    }
}
