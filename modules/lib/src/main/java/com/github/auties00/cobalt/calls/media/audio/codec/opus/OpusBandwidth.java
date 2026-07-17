package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates the five libopus audio bandwidth ceilings the call encoder can cap itself to, each a
 * standard {@code OPUS_BANDWIDTH_*} constant passed to the maximum bandwidth control.
 *
 * <p>The encoder's maximum bandwidth bounds how much of the audio spectrum libopus encodes, which in
 * turn bounds the effective bitrate: capping to narrowband forces an 8 kHz audio bandwidth even when
 * the encoder runs at a 48 kHz sample rate. The five constants are contiguous libopus codes starting
 * at {@code OPUS_BANDWIDTH_NARROWBAND}; the call engine selects one by a level index {@code 0..4} that
 * maps onto narrowband through fullband, reading the current ceiling with the get maximum bandwidth
 * control and applying a new one with the set maximum bandwidth control.
 *
 * @implNote This implementation derives each code as {@code OPUS_BANDWIDTH_NARROWBAND + level}, where
 * {@code OPUS_BANDWIDTH_NARROWBAND == 0x44d == 1101} and {@code level} runs {@code 0..4} for
 * narrowband, mediumband, wideband, super wideband, and fullband, giving codes {@code 1101..1105}.
 * The level to bandwidth mapping follows the libopus constant ordering, not the per sample rate
 * bandwidth index resolved by {@link OpusDefaultAttr}.
 */
public enum OpusBandwidth {
    /**
     * Narrowband: 4 kHz audio bandwidth, libopus code {@code 1101}.
     */
    NARROWBAND(0, 1101),

    /**
     * Mediumband: 6 kHz audio bandwidth, libopus code {@code 1102}.
     */
    MEDIUMBAND(1, 1102),

    /**
     * Wideband: 8 kHz audio bandwidth, libopus code {@code 1103}.
     */
    WIDEBAND(2, 1103),

    /**
     * Super wideband: 12 kHz audio bandwidth, libopus code {@code 1104}.
     */
    SUPER_WIDEBAND(3, 1104),

    /**
     * Fullband: 20 kHz audio bandwidth, libopus code {@code 1105}.
     */
    FULLBAND(4, 1105);

    /**
     * Resolves a call engine level index to its bandwidth, backing {@link #ofLevel(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #level}, so a level index resolves
     * to its bandwidth in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, OpusBandwidth> BY_LEVEL;

    /**
     * Resolves a libopus {@code OPUS_BANDWIDTH_*} code to its bandwidth, backing {@link #ofNative(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #code}, so a native code resolves
     * to its bandwidth in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, OpusBandwidth> BY_NATIVE;

    static {
        var byLevel = new HashMap<Integer, OpusBandwidth>();
        var byNative = new HashMap<Integer, OpusBandwidth>();
        for (var bandwidth : values()) {
            if (byLevel.put(bandwidth.level, bandwidth) != null) {
                throw new AssertionError("Conflict");
            }
            if (byNative.put(bandwidth.code, bandwidth) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_LEVEL = Map.copyOf(byLevel);
        BY_NATIVE = Map.copyOf(byNative);
    }

    /**
     * The libopus {@code OPUS_BANDWIDTH_NARROWBAND} base constant the level is added to.
     *
     * <p>Equal to {@code 0x44d}; the five bandwidth codes are this value plus the level {@code 0..4}.
     */
    public static final int OPUS_BANDWIDTH_NARROWBAND = 1101;

    /**
     * Holds the call engine level index {@code 0..4} for this bandwidth.
     */
    private final int level;

    /**
     * Holds the libopus {@code OPUS_BANDWIDTH_*} integer code for this bandwidth.
     */
    private final int code;

    /**
     * Constructs a bandwidth constant bound to its level index and native libopus code.
     *
     * @param level the call engine level index, {@code 0..4}
     * @param code  the libopus {@code OPUS_BANDWIDTH_*} integer code
     */
    OpusBandwidth(int level, int code) {
        this.level = level;
        this.code = code;
    }

    /**
     * Returns the call engine level index {@code 0..4} for this bandwidth.
     *
     * @return the level index
     */
    public int level() {
        return level;
    }

    /**
     * Returns the libopus {@code OPUS_BANDWIDTH_*} integer code applied through the maximum bandwidth
     * control.
     *
     * @return the native bandwidth code, one of {@code 1101..1105}
     */
    public int toNative() {
        return code;
    }

    /**
     * Returns the bandwidth for the given call engine level index.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_LEVEL} map rather than
     * scanning {@link #values()}.
     * @param level the level index, {@code 0..4}
     * @return the matching bandwidth
     * @throws IllegalArgumentException if {@code level} is outside {@code 0..4}
     */
    public static OpusBandwidth ofLevel(int level) {
        var bandwidth = BY_LEVEL.get(level);
        if (bandwidth == null) {
            throw new IllegalArgumentException("Unknown Opus bandwidth level: " + level);
        }
        return bandwidth;
    }

    /**
     * Returns the bandwidth for the given libopus {@code OPUS_BANDWIDTH_*} code.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_NATIVE} map rather than
     * scanning {@link #values()}.
     * @param code the native bandwidth code, one of {@code 1101..1105}
     * @return the matching bandwidth
     * @throws IllegalArgumentException if {@code code} is outside {@code 1101..1105}
     */
    public static OpusBandwidth ofNative(int code) {
        var bandwidth = BY_NATIVE.get(code);
        if (bandwidth == null) {
            throw new IllegalArgumentException("Unknown Opus bandwidth code: " + code);
        }
        return bandwidth;
    }
}
