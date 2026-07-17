package com.github.auties00.cobalt.calls.media.audio.codec.mlow.bwe;

import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowRangeDecoder;

/**
 * Decodes one internal frame's high band parameters for the MLow speech codec.
 *
 * <p>On the profiles above 16 kHz each internal frame carries, after its low band parameters, two high band
 * vector quantization indices: a high band gain index and a high band LSF index. This decoder reads them from
 * the same range coded stream the low band decode reads, immediately after that frame's low band parameters.
 * The gain index is read first against the gain entropy cumulative mass function (CMF) selected by frame
 * length, voicing, and rate. The LSF index is read second against the LSF CMF, which is the conditional CMF row
 * selected by the previous frame's LSF index when the frame is conditionally coded and low rate, and the
 * unconditional CMF otherwise.
 *
 * <p>The decoder is stateful across the frames of a packet and across packets in one continuous decode session:
 * it threads the previous high band LSF index used to select the conditional CMF row. Construct one instance
 * per logical stream and feed it every frame in order; {@link #reset()} returns it to the freshly constructed
 * state.
 *
 * @implNote This implementation reads the conditional LSF CMF row in place. The chosen row is windowed out of
 * the concatenated conditional block by {@code selCond(voiced)[prevHbLpcIx] * cmfLen}, and the offset aware
 * {@link MlowEntropyWrapper#decodeUpdate(MlowRangeDecoder, int[], int, int)} decodes that row from its window
 * (measuring every span relative to the first windowed entry) without copying it out to a temporary array.
 */
public final class MlowHbParamDecoder {
    /**
     * The previous frame's high band LSF index.
     *
     * <p>Read to select the conditional LSF CMF row and updated after each frame's LSF decode, threading the
     * conditional coding state across frames and packets.
     */
    private int prevHbLpcIx;

    /**
     * Constructs a high band parameter decoder with cleared conditional coding state.
     */
    public MlowHbParamDecoder() {
        this.prevHbLpcIx = 0;
    }

    /**
     * Returns this decoder to its freshly constructed state.
     *
     * <p>Clears the previous high band LSF index so the next frame's conditional CMF row selection starts from
     * the reset value. Call this between independent decode sessions; do not call it between the packets of one
     * continuous stream, which must thread state.
     */
    public void reset() {
        this.prevHbLpcIx = 0;
    }

    /**
     * Decodes one internal frame's high band parameters from the shared range coded stream.
     *
     * <p>Reads the gain index against the gain CMF selected by frame length, voicing, and rate, then the LSF
     * index against the conditional or unconditional LSF CMF, advancing {@code decoder} past both symbols and
     * updating the threaded previous high band LSF index.
     *
     * @param decoder         the range decoder positioned at the frame's first high band symbol, immediately
     *                        after the frame's low band parameters
     * @param frameLength16   the low band frame length in samples; selects the gain frame length class, where
     *                        {@code 320} is the 20 ms case of four high band subframes
     * @param voiced          {@code true} when the frame is voiced
     * @param condCoding      the conditional coding flag in effect for the frame
     * @param lowRate         {@code true} for the low rate mode
     * @return the decoded high band parameters of the frame
     */
    public MlowBandwidthExtension.HbFrameParams decode(MlowRangeDecoder decoder, int frameLength16, boolean voiced,
                                                       boolean condCoding, boolean lowRate) {
        var v = voiced ? 1 : 0;
        var lr = lowRate ? 1 : 0;
        var framelen20 = frameLength16 == 320 ? 1 : 0;

        var gainCmf = MlowHbTables.gainCmf(framelen20, v, lr);
        var gainQi = MlowEntropyWrapper.decodeUpdate(decoder, gainCmf);

        int lsfIdx;
        if (condCoding && lowRate) {
            var cmfLen = MlowHbTables.lsfSize(v, lr) + 1;
            var row = MlowHbTables.selCond(v)[prevHbLpcIx];
            var condBlock = MlowHbTables.lsfCmfCond(v);
            lsfIdx = MlowEntropyWrapper.decodeUpdate(decoder, condBlock, row * cmfLen, cmfLen);
        } else {
            lsfIdx = MlowEntropyWrapper.decodeUpdate(decoder, MlowHbTables.lsfCmf(v, lr));
        }
        prevHbLpcIx = lsfIdx;

        return new MlowBandwidthExtension.HbFrameParams(gainQi, lsfIdx);
    }
}
