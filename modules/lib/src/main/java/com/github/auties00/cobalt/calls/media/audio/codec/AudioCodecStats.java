package com.github.auties00.cobalt.calls.media.audio.codec;

/**
 * An immutable snapshot of one {@link AudioCodec} instance's lifetime counters: the FEC and PLC
 * recovery tallies, the rolling encode and decode timing, and the observed and target bitrates.
 *
 * <p>An {@link AudioCodec} accumulates these counters as it encodes and decodes and exposes them
 * through this record, which the call telemetry layer drains into the WAM call event and which the
 * codec also logs at close. {@link #fecFrames()} and {@link #plcFrames()} count the decode side
 * recoveries: in band forward error correction reconstructions and packet loss concealment fills.
 * {@link #avgEncodeMicros()} and {@link #avgDecodeMicros()} are the rolling averages of the native
 * encode and decode wall time. {@link #avgTargetBitrate()} is the running average target bitrate the
 * encoder ran at, and {@link #observedBitrate()} is the bitrate derived from the running average
 * encoded frame size.
 *
 * @param totalEncodedFrames the cumulative count of frames passed to the encoder
 * @param totalDecodedFrames the cumulative count of frames passed to the decoder
 * @param fecFrames          the decode side count of frames reconstructed from in band forward error correction data
 * @param plcFrames          the decode side count of frames filled by packet loss concealment
 * @param avgEncodeMicros    the rolling average native encode wall time per frame, in microseconds
 * @param avgDecodeMicros    the rolling average native decode wall time per frame, in microseconds
 * @param avgTargetBitrate   the running average encoder target bitrate, in bits per second
 * @param observedBitrate    the bitrate derived from the running average encoded frame size, in bits
 *                           per second
 * @implNote This implementation collapses the native codec's per bandwidth frame counts (narrowband,
 * mediumband, wideband, super wideband, fullband) into the single aggregate frame totals, since the
 * telemetry consumer reads only the aggregate frame counts and recovery tallies. Encode and decode
 * timing is held in microseconds for resolution.
 */
public record AudioCodecStats(
        long totalEncodedFrames,
        long totalDecodedFrames,
        long fecFrames,
        long plcFrames,
        long avgEncodeMicros,
        long avgDecodeMicros,
        int avgTargetBitrate,
        int observedBitrate
) {
    /**
     * Returns a stats snapshot with every counter set to zero, the state of a codec that has
     * processed no frames.
     *
     * @return the empty stats snapshot
     */
    public static AudioCodecStats empty() {
        return new AudioCodecStats(0, 0, 0, 0, 0, 0, 0, 0);
    }
}
