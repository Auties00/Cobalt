package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates the four per sample rate default Opus configurations the call engine seeds an encoder
 * from, the typed form of the native {@code opus_default_attr} table.
 *
 * <p>When a stream negotiates a sample rate, the engine looks up the row whose {@link #sampleRate()}
 * matches and copies its default, minimum, and maximum bitrate, its bandwidth enum column, and its
 * frame time ceiling into the encoder parameters. The minimum bitrate floor is uniformly 6000 bps
 * across all rows; the default and maximum scale with the sample rate. The bandwidth enum column is a
 * per row index distinct from the {@link OpusBandwidth} libopus codes: it is the value the native
 * table stores ({@code 0/2/3/4}), which the engine maps onto a maximum bandwidth ceiling.
 *
 * <p>The constants are declared ascending by sample rate ({@link #NB}, {@link #WB}, {@link #SWB},
 * {@link #FB}). Each row also carries a fixed frame time ceiling of {@link #FRAME_MILLIS_CEILING},
 * surfaced through {@link #frameMillisCeiling()}.
 *
 * @implNote This implementation reproduces the native {@code opus_default_attr} table with the
 * name pointer column omitted, as it carries no runtime value beyond logging.
 */
public enum OpusDefaultAttr {
    /**
     * Narrowband at 8 kHz: default 12000 bps, range {@code 6000..16000}, bandwidth enum column
     * {@code 0}.
     */
    NB(8000, 12000, 6000, 16000, 0),

    /**
     * Wideband at 16 kHz: default 25000 bps, range {@code 6000..32000}, bandwidth enum column
     * {@code 2}.
     */
    WB(16000, 25000, 6000, 32000, 2),

    /**
     * Super wideband at 24 kHz: default 32000 bps, range {@code 6000..32000}, bandwidth enum column
     * {@code 3}.
     */
    SWB(24000, 32000, 6000, 32000, 3),

    /**
     * Fullband at 48 kHz: default 40000 bps, range {@code 6000..40000}, bandwidth enum column
     * {@code 4}.
     */
    FB(48000, 40000, 6000, 40000, 4);

    /**
     * Resolves an input sample rate to its default attribute row, backing {@link #ofSampleRate(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #sampleRate}, so a sample rate
     * resolves to its row in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, OpusDefaultAttr> BY_SAMPLE_RATE;

    static {
        var bySampleRate = new HashMap<Integer, OpusDefaultAttr>();
        for (var attr : values()) {
            if (bySampleRate.put(attr.sampleRate, attr) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_SAMPLE_RATE = Map.copyOf(bySampleRate);
    }

    /**
     * The minimum bitrate floor, in bits per second, shared by every row of the table.
     *
     * <p>Equal to 6000 bps; the native table stores this value in the minimum bitrate column of all
     * four rows.
     */
    public static final int MIN_BITRATE_FLOOR = 6000;

    /**
     * The frame time ceiling column value, shared by every row of the native table.
     *
     * <p>Equal to {@code 120}; surfaced as {@link #frameMillisCeiling()} on each row.
     */
    public static final int FRAME_MILLIS_CEILING = 120;

    /**
     * Holds the input sample rate, in Hz, this row is keyed on.
     */
    private final int sampleRate;

    /**
     * Holds the default target bitrate, in bits per second, for this row.
     */
    private final int defaultBitrate;

    /**
     * Holds the minimum target bitrate, in bits per second, for this row.
     */
    private final int minBitrate;

    /**
     * Holds the maximum target bitrate, in bits per second, for this row.
     */
    private final int maxBitrate;

    /**
     * Holds the native bandwidth enum column value for this row.
     */
    private final int bandwidthEnum;

    /**
     * Constructs a row bound to its sample rate, bitrate triplet, and bandwidth enum column.
     *
     * @param sampleRate     the input sample rate in Hz
     * @param defaultBitrate the default target bitrate in bits per second
     * @param minBitrate     the minimum target bitrate in bits per second
     * @param maxBitrate     the maximum target bitrate in bits per second
     * @param bandwidthEnum  the native bandwidth enum column value
     */
    OpusDefaultAttr(int sampleRate, int defaultBitrate, int minBitrate, int maxBitrate, int bandwidthEnum) {
        this.sampleRate = sampleRate;
        this.defaultBitrate = defaultBitrate;
        this.minBitrate = minBitrate;
        this.maxBitrate = maxBitrate;
        this.bandwidthEnum = bandwidthEnum;
    }

    /**
     * Returns the input sample rate, in Hz, this row is keyed on.
     *
     * @return the sample rate in Hz
     */
    public int sampleRate() {
        return sampleRate;
    }

    /**
     * Returns the default target bitrate, in bits per second, for this row.
     *
     * @return the default bitrate in bits per second
     */
    public int defaultBitrate() {
        return defaultBitrate;
    }

    /**
     * Returns the minimum target bitrate, in bits per second, for this row.
     *
     * <p>Equal to {@link #MIN_BITRATE_FLOOR} for every row.
     *
     * @return the minimum bitrate in bits per second
     */
    public int minBitrate() {
        return minBitrate;
    }

    /**
     * Returns the maximum target bitrate, in bits per second, for this row.
     *
     * @return the maximum bitrate in bits per second
     */
    public int maxBitrate() {
        return maxBitrate;
    }

    /**
     * Returns the native bandwidth enum column value for this row.
     *
     * <p>This is the per row index the native table stores ({@code 0/2/3/4}), not the
     * {@link OpusBandwidth} libopus code; it is the value the engine maps onto a maximum bandwidth
     * ceiling.
     *
     * @return the bandwidth enum column value
     */
    public int bandwidthEnum() {
        return bandwidthEnum;
    }

    /**
     * Returns the frame time ceiling column value for this row.
     *
     * @return {@link #FRAME_MILLIS_CEILING}
     */
    public int frameMillisCeiling() {
        return FRAME_MILLIS_CEILING;
    }

    /**
     * Returns the default attribute row keyed on the given sample rate.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_SAMPLE_RATE} map rather than
     * scanning {@link #values()}.
     * @param sampleRate the input sample rate in Hz; one of {@code 8000}, {@code 16000},
     *                   {@code 24000}, {@code 48000}
     * @return the matching row
     * @throws IllegalArgumentException if no row is keyed on {@code sampleRate}
     */
    public static OpusDefaultAttr ofSampleRate(int sampleRate) {
        var attr = BY_SAMPLE_RATE.get(sampleRate);
        if (attr == null) {
            throw new IllegalArgumentException("No Opus default-attr row for sample rate: " + sampleRate);
        }
        return attr;
    }
}
