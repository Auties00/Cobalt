package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import java.util.Objects;

/**
 * An immutable set of Opus encoder and decoder parameters the call media path opens a codec with and
 * reconfigures it with during an active call.
 *
 * <p>The parameters cover the codec geometry ({@link #sampleRate()}, {@link #channels()},
 * {@link #application()}), the bitrate triplet ({@link #defaultBitrate()}, {@link #minBitrate()},
 * {@link #maxBitrate()}) seeded from the {@link OpusDefaultAttr} table, the rate control mode
 * ({@link #variableBitrate()}, {@link #maxBandwidth()}, {@link #complexity()}), the loss recovery knobs
 * ({@link #inbandFec()}, {@link #packetLossPercent()}, {@link #discontinuousTransmission()}), the
 * fixed coding shape ({@link #forceChannels()}, {@link #signalVoice()}, {@link #lsbDepth()}), and the
 * packetization geometry ({@link #framesPerPacket()}, {@link #frameMillis()}). On open the codec
 * applies every control these fields select; on a {@linkplain OpusAudioCodec#modify(OpusCodecParams)
 * modify} round only the mutable subset (bitrate, packet loss percentage, variable bitrate, complexity,
 * maximum bandwidth) is reapplied.
 *
 * <p>Build instances through {@link #forSampleRate(int, int, OpusApplication)}, which seeds the
 * bitrate triplet and the bandwidth derived defaults from the matching {@link OpusDefaultAttr} row, then
 * derive variants with the {@code with*} copy methods. The {@linkplain #framesPerPacket() frames per
 * packet} count is bounded to {@code 1..6}, the WhatsApp aggregation range.
 *
 * @param sampleRate                the input and output sample rate in Hz; one of {@code 8000},
 *                                  {@code 12000}, {@code 16000}, {@code 24000}, {@code 48000}
 * @param channels                  the channel count, {@code 1} for mono or {@code 2} for stereo
 * @param application               the libopus application mode the encoder opens in
 * @param defaultBitrate            the target bitrate in bits per second, or {@code -1000}
 *                                  ({@link #BITRATE_AUTO}) to let libopus choose
 * @param minBitrate                the minimum target bitrate in bits per second
 * @param maxBitrate                the maximum target bitrate in bits per second the bandwidth cap
 *                                  clamps toward
 * @param variableBitrate           whether variable bitrate coding is enabled
 * @param maxBandwidth              the maximum audio bandwidth ceiling the encoder caps to
 * @param complexity                the encoder complexity level {@code 0..10}
 * @param inbandFec                 whether inband forward error correction (LBRR) is enabled
 * @param packetLossPercent         the expected packet loss percentage {@code 0..100} driving FEC
 *                                  redundancy
 * @param discontinuousTransmission whether discontinuous transmission (DTX) is enabled
 * @param forceChannels             the forced channel count {@code 1} or {@code 2}, or {@code -1000}
 *                                  ({@link #BITRATE_AUTO}) to leave libopus to decide
 * @param signalVoice               whether the signal type hint is voice ({@code true}) or music
 *                                  ({@code false})
 * @param lsbDepth                  the encoder least significant bit depth hint {@code 8..24}
 * @param framesPerPacket           the number of Opus frames aggregated into one RTP payload,
 *                                  {@code 1..6}
 * @param frameMillis               the duration of one Opus frame in milliseconds
 * @implNote This implementation seeds the bitrate triplet and the bandwidth ceiling from
 * {@link OpusDefaultAttr}. The defaults for {@link #complexity()}, {@link #framesPerPacket()}, and
 * {@link #packetLossPercent()} are the voice tuned constants applied when the runtime {@code voip_settings}
 * {@code encode} block omits an explicit value. The default frame duration is 20 ms, matching the NetEq
 * get period.
 */
public record OpusCodecParams(
        int sampleRate,
        int channels,
        OpusApplication application,
        int defaultBitrate,
        int minBitrate,
        int maxBitrate,
        boolean variableBitrate,
        OpusBandwidth maxBandwidth,
        int complexity,
        boolean inbandFec,
        int packetLossPercent,
        boolean discontinuousTransmission,
        int forceChannels,
        boolean signalVoice,
        int lsbDepth,
        int framesPerPacket,
        int frameMillis
) {
    /**
     * Sentinel matching libopus {@code OPUS_AUTO} for the bitrate and forced channel controls.
     *
     * <p>A {@link #defaultBitrate()} of this value tells libopus to choose its own target; a
     * {@link #forceChannels()} of this value leaves the channel forcing decision to libopus.
     */
    public static final int BITRATE_AUTO = -1000;

    /**
     * The lowest frames per packet count the aggregator accepts.
     */
    public static final int MIN_FRAMES_PER_PACKET = 1;

    /**
     * The highest frames per packet count the aggregator accepts.
     */
    public static final int MAX_FRAMES_PER_PACKET = 6;

    /**
     * The default encoder complexity applied when the runtime {@code voip_settings} value is absent.
     */
    public static final int DEFAULT_COMPLEXITY = 9;

    /**
     * The default frame duration in milliseconds, matching the NetEq 20 ms get period.
     */
    public static final int DEFAULT_FRAME_MILLIS = 20;

    /**
     * The default least significant bit depth hint for 16 bit PCM input.
     */
    public static final int DEFAULT_LSB_DEPTH = 16;

    /**
     * Validates the geometry and clamps the frames per packet count into {@code 1..6}.
     *
     * @throws NullPointerException     if {@code application} or {@code maxBandwidth} is {@code null}
     * @throws IllegalArgumentException if {@code channels} is not {@code 1} or {@code 2}, if
     *                                  {@code complexity} is outside {@code 0..10}, if
     *                                  {@code packetLossPercent} is outside {@code 0..100}, or if
     *                                  {@code frameMillis} is not positive
     */
    public OpusCodecParams {
        Objects.requireNonNull(application, "application cannot be null");
        Objects.requireNonNull(maxBandwidth, "maxBandwidth cannot be null");
        if (channels < 1 || channels > 2) {
            throw new IllegalArgumentException("channels must be 1 or 2, got " + channels);
        }
        if (complexity < 0 || complexity > 10) {
            throw new IllegalArgumentException("complexity must be in 0..10, got " + complexity);
        }
        if (packetLossPercent < 0 || packetLossPercent > 100) {
            throw new IllegalArgumentException("packetLossPercent must be in 0..100, got " + packetLossPercent);
        }
        if (frameMillis <= 0) {
            throw new IllegalArgumentException("frameMillis must be positive, got " + frameMillis);
        }
        framesPerPacket = Math.clamp(framesPerPacket, MIN_FRAMES_PER_PACKET, MAX_FRAMES_PER_PACKET);
    }

    /**
     * Returns the per channel sample count of one Opus frame for the configured frame duration.
     *
     * <p>Computed as {@code sampleRate * frameMillis / 1000}; for example 320 samples for a 20 ms
     * frame at 16 kHz. This is the {@code frame_size} argument passed to {@code opus_encode} and
     * {@code opus_decode}.
     *
     * @return the per channel frame size in samples
     */
    public int frameSize() {
        return sampleRate * frameMillis / 1000;
    }

    /**
     * Returns a copy with the given target bitrate.
     *
     * @param bitrate the target bitrate in bits per second, or {@link #BITRATE_AUTO}
     * @return a copy carrying the new bitrate
     */
    public OpusCodecParams withBitrate(int bitrate) {
        return new OpusCodecParams(sampleRate, channels, application, bitrate, minBitrate, maxBitrate,
                variableBitrate, maxBandwidth, complexity, inbandFec, packetLossPercent,
                discontinuousTransmission, forceChannels, signalVoice, lsbDepth, framesPerPacket, frameMillis);
    }

    /**
     * Returns a copy with the given expected packet loss percentage.
     *
     * @param percent the expected loss percentage {@code 0..100}
     * @return a copy carrying the new loss percentage
     */
    public OpusCodecParams withPacketLossPercent(int percent) {
        return new OpusCodecParams(sampleRate, channels, application, defaultBitrate, minBitrate, maxBitrate,
                variableBitrate, maxBandwidth, complexity, inbandFec, percent,
                discontinuousTransmission, forceChannels, signalVoice, lsbDepth, framesPerPacket, frameMillis);
    }

    /**
     * Returns a copy with the given encoder complexity level.
     *
     * @param complexity the complexity level {@code 0..10}
     * @return a copy carrying the new complexity
     */
    public OpusCodecParams withComplexity(int complexity) {
        return new OpusCodecParams(sampleRate, channels, application, defaultBitrate, minBitrate, maxBitrate,
                variableBitrate, maxBandwidth, complexity, inbandFec, packetLossPercent,
                discontinuousTransmission, forceChannels, signalVoice, lsbDepth, framesPerPacket, frameMillis);
    }

    /**
     * Returns a copy with the given maximum audio bandwidth ceiling.
     *
     * @param maxBandwidth the maximum bandwidth ceiling
     * @return a copy carrying the new bandwidth ceiling
     */
    public OpusCodecParams withMaxBandwidth(OpusBandwidth maxBandwidth) {
        return new OpusCodecParams(sampleRate, channels, application, defaultBitrate, minBitrate, maxBitrate,
                variableBitrate, maxBandwidth, complexity, inbandFec, packetLossPercent,
                discontinuousTransmission, forceChannels, signalVoice, lsbDepth, framesPerPacket, frameMillis);
    }

    /**
     * Returns a copy with inband forward error correction toggled.
     *
     * @param inbandFec whether to enable inband FEC
     * @return a copy carrying the new FEC setting
     */
    public OpusCodecParams withInbandFec(boolean inbandFec) {
        return new OpusCodecParams(sampleRate, channels, application, defaultBitrate, minBitrate, maxBitrate,
                variableBitrate, maxBandwidth, complexity, inbandFec, packetLossPercent,
                discontinuousTransmission, forceChannels, signalVoice, lsbDepth, framesPerPacket, frameMillis);
    }

    /**
     * Returns a copy with the given frames per packet aggregation count.
     *
     * @param framesPerPacket the frames per packet count, clamped into {@code 1..6}
     * @return a copy carrying the new frames per packet count
     */
    public OpusCodecParams withFramesPerPacket(int framesPerPacket) {
        return new OpusCodecParams(sampleRate, channels, application, defaultBitrate, minBitrate, maxBitrate,
                variableBitrate, maxBandwidth, complexity, inbandFec, packetLossPercent,
                discontinuousTransmission, forceChannels, signalVoice, lsbDepth, framesPerPacket, frameMillis);
    }

    /**
     * Builds a parameter set for the given sample rate, channels, and application mode, seeded from the
     * matching {@link OpusDefaultAttr} table row.
     *
     * <p>The bitrate triplet, the maximum bandwidth ceiling (mapped from the row's bandwidth enum
     * column), and the frame derived defaults come from the row; the rate control and loss recovery
     * knobs take the WhatsApp voice defaults (variable bitrate on, complexity
     * {@link #DEFAULT_COMPLEXITY}, inband FEC on, DTX on, voice signal, 20 ms frames, single frame
     * packets).
     *
     * @param sampleRate  the input sample rate in Hz; must match an {@link OpusDefaultAttr} row
     * @param channels    the channel count, {@code 1} or {@code 2}
     * @param application the libopus application mode
     * @return the seeded parameter set
     * @throws NullPointerException     if {@code application} is {@code null}
     * @throws IllegalArgumentException if {@code sampleRate} matches no table row or {@code channels}
     *                                  is invalid
     */
    public static OpusCodecParams forSampleRate(int sampleRate, int channels, OpusApplication application) {
        Objects.requireNonNull(application, "application cannot be null");
        var attr = OpusDefaultAttr.ofSampleRate(sampleRate);
        var maxBandwidth = bandwidthForEnum(attr.bandwidthEnum());
        return new OpusCodecParams(
                sampleRate,
                channels,
                application,
                attr.defaultBitrate(),
                attr.minBitrate(),
                attr.maxBitrate(),
                true,
                maxBandwidth,
                DEFAULT_COMPLEXITY,
                true,
                0,
                true,
                BITRATE_AUTO,
                true,
                DEFAULT_LSB_DEPTH,
                MIN_FRAMES_PER_PACKET,
                DEFAULT_FRAME_MILLIS
        );
    }

    /**
     * Maps a {@link OpusDefaultAttr} bandwidth enum column value onto the matching {@link OpusBandwidth}
     * ceiling.
     *
     * @implNote This implementation reads the {@link OpusDefaultAttr} bandwidth enum column ({@code 0}
     * narrowband, {@code 2} wideband, {@code 3} super wideband, {@code 4} fullband) and returns the
     * corresponding {@link OpusBandwidth}; the column never carries {@code 1} (mediumband) across the
     * table rows.
     *
     * @param bandwidthEnum the bandwidth enum column value
     * @return the matching bandwidth ceiling
     * @throws IllegalArgumentException if {@code bandwidthEnum} is not {@code 0}, {@code 2}, {@code 3},
     *                                  or {@code 4}
     */
    private static OpusBandwidth bandwidthForEnum(int bandwidthEnum) {
        return switch (bandwidthEnum) {
            case 0 -> OpusBandwidth.NARROWBAND;
            case 2 -> OpusBandwidth.WIDEBAND;
            case 3 -> OpusBandwidth.SUPER_WIDEBAND;
            case 4 -> OpusBandwidth.FULLBAND;
            default -> throw new IllegalArgumentException("Unknown bandwidth-enum column: " + bandwidthEnum);
        };
    }
}
