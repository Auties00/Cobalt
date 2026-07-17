package com.github.auties00.cobalt.calls.media.audio.codec;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.calls.media.audio.codec.mlow.MLowAudioCodec;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusAudioCodec;
import com.github.auties00.cobalt.calls.media.audio.codec.opus.OpusCodecParams;

/**
 * Encodes PCM audio into compressed frames, decodes compressed frames back to PCM, recovers lost
 * frames, and reconfigures the codec mid call.
 *
 * <p>An instance owns one encoder and one decoder for a single audio stream and is single writer:
 * the encode path, the decode path, and reconfiguration must be driven from one thread, since the
 * codec holds mutable native state and reusable scratch buffers. {@link #encode(short[], int)}
 * classifies each frame as voice active or discontinuous; {@link #decode(byte[], int, boolean)}
 * optionally pulls an in band forward error correction copy from the supplied packet;
 * {@link #recover(byte[], int)} fills a lost frame through packet loss concealment or the next
 * packet's forward error correction; {@link #modify(OpusCodecParams)} re applies the mutable rate
 * control settings; and {@link #stats()} snapshots the lifetime counters.
 *
 * <p>The hierarchy is sealed for exhaustive matching: {@link OpusAudioCodec} is the production codec
 * wrapping libopus, and {@link MLowAudioCodec} is the low bitrate MLow codec the engine can select
 * through the {@code hybrid_codec} setting. The 1:1 and group media paths default to Opus.
 */
public sealed interface AudioCodec extends AutoCloseable
        permits OpusAudioCodec, MLowAudioCodec {
    /**
     * Encodes one PCM frame into a compressed codec frame, classified for voice activity and DTX.
     *
     * @implSpec Implementations must require {@code pcm.length >= frameSize * channels} (signed 16 bit
     * samples) and must classify the result: an empty output (discontinuous transmission produced
     * nothing) yields a zero length {@linkplain EncodedAudioFrame#payload() payload}, a below threshold
     * output yields a {@linkplain EncodedAudioFrame#discontinuous() discontinuous} frame, and a speech
     * length output is inspected for the voice activity and in band forward error correction flags. The
     * frame size is the per channel sample count, not the byte count.
     *
     * @param pcm       the input PCM samples, signed 16 bit native byte order, interleaved if stereo
     * @param frameSize the per channel sample count of one frame
     * @return the encoded frame with its classification flags
     * @throws NullPointerException       if {@code pcm} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.Opus if encoding fails
     */
    EncodedAudioFrame encode(short[] pcm, int frameSize);

    /**
     * Decodes one compressed codec frame into PCM, optionally reconstructing it from the packet's
     * in band FEC.
     *
     * @implSpec Implementations must validate the output buffer fits {@code frameSize * channels}
     * samples. When {@code decodeFec} is {@code true}, the supplied packet is the next packet in
     * sequence and the decoder must extract the LBRR copy of the previous (lost) frame from it; when
     * {@code false}, the packet is decoded normally. The returned array length is the decoded sample
     * count times the channel count.
     *
     * @param payload   the compressed codec packet bytes
     * @param frameSize the per channel sample count to decode
     * @param decodeFec whether to reconstruct the previous frame from this packet's in band forward
     *                  error correction
     * @return the decoded PCM samples, signed 16 bit native byte order
     * @throws NullPointerException       if {@code payload} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.Opus if decoding fails
     */
    short[] decode(byte[] payload, int frameSize, boolean decodeFec);

    /**
     * Recovers a single lost frame, either by packet loss concealment or from a following packet's
     * in band forward error correction.
     *
     * @implSpec When {@code nextPayload} is {@code null}, implementations must run packet loss
     * concealment, synthesizing a plausible continuation frame from decoder state; when
     * {@code nextPayload} is non null, implementations must reconstruct the lost frame from that
     * packet's LBRR in band forward error correction. The returned array length is {@code frameSize}
     * times the channel count.
     *
     * @param nextPayload the next packet carrying the lost frame's forward error correction, or
     *                    {@code null} to conceal
     * @param frameSize   the per channel sample count to recover
     * @return the recovered PCM samples, signed 16 bit native byte order
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.Opus if recovery fails
     */
    short[] recover(byte[] nextPayload, int frameSize);

    /**
     * Reconfigures the encoder mid call from the mutable subset of the given parameters.
     *
     * @implSpec Implementations must re apply only the controls a live encoder accepts without a
     * reopen: the target bitrate (clamped by the cap derived from the maximum bandwidth), the expected
     * packet loss percentage, the variable bitrate flag, the encoder complexity, and the maximum
     * bandwidth. The codec geometry ({@linkplain OpusCodecParams#sampleRate() sample rate},
     * {@linkplain OpusCodecParams#channels() channels}, {@linkplain OpusCodecParams#application()
     * application}) must not change; a geometry change requires tearing the codec down and reopening.
     *
     * @param params the parameter set whose mutable fields the encoder adopts
     * @throws NullPointerException       if {@code params} is {@code null}
     * @throws IllegalStateException      if the codec is closed
     * @throws WhatsAppCallException.Opus if a control call fails
     */
    void modify(OpusCodecParams params);

    /**
     * Returns a snapshot of this codec's lifetime counters.
     *
     * @return the current stats snapshot
     */
    AudioCodecStats stats();

    /**
     * Returns whether the encoder's last encoded frame was a discontinuous transmission frame.
     *
     * @implSpec Implementations report the classification of the most recent
     * {@linkplain #encode(short[], int) encode} call; before the first encode the result is
     * {@code false}.
     *
     * @return {@code true} if the last encoded frame was a discontinuous transmission or comfort noise
     * frame
     */
    boolean lastFrameWasDiscontinuous();

    /**
     * Releases the native codec state and any owned resources.
     *
     * @implSpec Implementations must be idempotent and must emit the lifetime stats before releasing
     * native state. After closing, every other method throws {@link IllegalStateException}.
     */
    @Override
    void close();
}
