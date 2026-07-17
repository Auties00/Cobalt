package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

/**
 * Decoded table of contents (TOC) byte of an MLow packet.
 *
 * <p>Every MLow packet begins with a single TOC byte that announces the packet's coding mode, internal
 * sample rate, frame length, rate mode, forward error correction (FEC) presence, and channel count. This
 * record carries the decoded fields plus the derived per frame loop counts the low band parameter decoder
 * needs, so a caller never re derives them from the raw byte.
 *
 * <p>The bit layout, most significant bit first, is:
 * {@snippet :
 * // +--1--+--2--+--3--+--4--+--5--+--6--+--7--+--8--+
 * // |  SID/VoA  | Fs  |  FrameSz  | Rate| FEC | Ste |
 * // +-----+-----+-----+-----+-----+-----+-----+-----+
 * }
 * where bit 7 is {@code SID}, bit 6 is {@code VAD} (voice activity), bit 5 is the sample rate flag
 * ({@code 0} for 16 kHz, {@code 1} for the 32/48 kHz mode), bits 4 and 3 are the frame size selector
 * ({@code 00} for 10 ms, {@code 01} for 20 ms, {@code 10} for 60 ms, {@code 11} for 120 ms), bit 2 is the
 * low rate flag, bit 1 is the raw FEC/hangover bit, and bit 0 is the stereo flag.
 *
 * <p>Two derived fields follow the coding conventions of the format: {@link #fec()} is set only when
 * {@code VAD} is also set ({@code FEC = VAD && rawBit1}), and {@link #codedAsActiveVoice()} is the
 * disjunction {@code VAD || rawBit1}, so an inactive packet flagged as hangover is still coded as if it may
 * contain voiced energy. The frame loop counts {@link #numFrames()}, {@link #numSubframes()}, and
 * {@link #frameLength16()} derive from the packet length, rate mode, and internal sample rate.
 *
 * <p>Scope is the SMPL 16 kHz, 60 ms, mono low band decode path. The 32/48 kHz high band modes are
 * representable here (the sample rate flag is decoded faithfully) but the parameter decoder that consumes
 * this record handles only the 16 kHz low band; a packet whose {@link #sampleRateHz()} exceeds 16 kHz is
 * an out of scope input.
 *
 * @param sid                 {@code true} for a silence insertion descriptor (SID) frame
 * @param vad                 {@code true} when the packet was sent with detected voice activity
 * @param sampleRateHz        the internal sample rate in hertz, either {@code 16000} or {@code 32000}
 * @param packetLenMs         the packet length in milliseconds, one of {@code 10}, {@code 20},
 *                            {@code 60}, or {@code 120}
 * @param lowRate             {@code true} for the low rate mode of SMPL, {@code false} for high rate
 * @param fec                 {@code true} when the packet carries an inband FEC frame
 *                            ({@code VAD && rawBit1})
 * @param codedAsActiveVoice  {@code true} when the frames are coded as if they may contain voiced energy
 *                            ({@code VAD || rawBit1})
 * @param stereo              {@code true} when the packet carries stereo information
 */
public record MlowTocByte(
        boolean sid,
        boolean vad,
        int sampleRateHz,
        int packetLenMs,
        boolean lowRate,
        boolean fec,
        boolean codedAsActiveVoice,
        boolean stereo) {
    /**
     * Sample clock rate of the CELP core in kilohertz.
     *
     * <p>The 16 kHz low band path runs at this rate; the frame and subframe sample counts are products of
     * this constant.
     */
    private static final int CELP_FS_KHZ = 16;

    /**
     * Decodes a TOC byte into its fields and derived loop counts.
     *
     * <p>Unpacks the eight bit fields of {@code tocByte}, including the {@code FEC = VAD && rawBit1} and
     * {@code codedAsActiveVoice = VAD || rawBit1} derivations, and the sample rate decode
     * {@code (((byte >> 5) & 1) + 1) * 16000}. The low byte is read unsigned, so a caller may pass a sign
     * extended {@code int} or a raw {@code byte} value.
     *
     * @param tocByte the raw TOC byte, read from the low eight bits
     * @return the decoded TOC with its derived per frame loop counts
     * @throws IllegalArgumentException if the frame size selector is not one of the four defined values,
     *                                  which the two bit field cannot produce but is guarded defensively
     */
    public static MlowTocByte decode(int tocByte) {
        var b = tocByte & 0xFF;
        var sid = ((b >> 7) & 1) != 0;
        var vad = ((b >> 6) & 1) != 0;
        var sampleRateHz = (((b >> 5) & 1) + 1) * 16000;
        var packetLenMs = switch ((b >> 3) & 3) {
            case 0 -> 10;
            case 1 -> 20;
            case 2 -> 60;
            case 3 -> 120;
            default -> throw new IllegalArgumentException("invalid frame-size selector in TOC byte " + b);
        };
        var lowRate = ((b >> 2) & 1) != 0;
        var rawBit1 = (b >> 1) & 1;
        var fec = vad && rawBit1 != 0;
        var codedAsActiveVoice = vad || rawBit1 != 0;
        var stereo = (b & 1) != 0;
        return new MlowTocByte(sid, vad, sampleRateHz, packetLenMs, lowRate, fec, codedAsActiveVoice, stereo);
    }

    /**
     * Returns the low band frame length in samples at 16 kHz.
     *
     * <p>Computed as {@code (1 + (packetLenMs > 10)) * (10 * CELP_FS_KHZ)}, so a 10 ms packet has a
     * 160 sample frame and every longer packet has a 320 sample frame. This is the per internal frame
     * length, not the whole packet length; {@link #numFrames()} internal frames make up the packet.
     *
     * @return the per frame low band sample count, {@code 160} or {@code 320}
     */
    public int frameLength16() {
        return (1 + (packetLenMs > 10 ? 1 : 0)) * (10 * CELP_FS_KHZ);
    }

    /**
     * Returns the number of internal 20 ms frames in the packet.
     *
     * <p>Computed as {@code (packetLenMs + 10) / 20}: a 10 or 20 ms packet is one frame, a 60 ms packet is
     * three frames, and a 120 ms packet is six frames. The parameter decoder runs once per internal frame,
     * threading conditional coding state from one frame to the next.
     *
     * @return the internal frame count
     */
    public int numFrames() {
        return (packetLenMs + 10) / 20;
    }

    /**
     * Returns the number of subframes per internal frame.
     *
     * <p>Computed as {@code 1 << (1 - lowRate + (packetLenMs > 10))}: a high rate 60 ms packet has four
     * subframes per frame, a low rate 60 ms packet has two, and a 10 ms packet has half as many. This is
     * the subframe count the pulse split, the per subframe gains, and the residual energy shape decode all
     * key off.
     *
     * @return the subframe count per internal frame; 1, 2, or 4
     */
    public int numSubframes() {
        return 1 << (1 - (lowRate ? 1 : 0) + (packetLenMs > 10 ? 1 : 0));
    }
}
