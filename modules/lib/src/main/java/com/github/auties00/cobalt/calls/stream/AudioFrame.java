package com.github.auties00.cobalt.calls.stream;

import java.util.Objects;

/**
 * Holds one frame of mono PCM audio in the format the WhatsApp call media stack operates on.
 *
 * <p>The payload is signed 16 bit PCM sampled at 16 kHz, one channel. The sample rate and channel
 * count are not carried on each frame: the public call API is fixed to 16 kHz mono to match the
 * WhatsApp audio codec configuration, so a frame describes only its samples and the instant at which
 * they are presented. A frame's duration is implied by its sample count, since at 16 kHz one
 * millisecond is sixteen samples; the engine drains frames at a fixed cadence (a 10 ms or 20 ms
 * block), so a source that produces a different chunk size is rechunked by the engine rather than
 * rejected here.
 *
 * <p>A producer that captures at a different rate, such as the 48 kHz an operating system microphone
 * commonly delivers, downsamples to 16 kHz mono before publishing a frame to an {@link AudioOutput};
 * likewise a sink draining an {@link AudioInput} that renders at another rate resamples on its side.
 * The sample buffer is referenced as supplied and never copied. A frame the call engine delivers to a
 * consumer through {@link AudioInput#readAudio()} borrows a pooled buffer owned by the producing input: the
 * buffer is valid only until the consumer reads the next frame from that same input, at which point
 * the producer may refill and re offer it, so the consumer must neither retain a reference to it past
 * that point nor mutate it. A device backed {@link AudioOutput} may likewise lend a buffer it reuses
 * across {@link AudioOutput#takeAudio()} calls, subject to the same rule against retention.
 *
 * @apiNote The call API does not negotiate alternate audio formats; a source or sink operating at a
 * native rate other than 16 kHz mono is responsible for resampling, and a mismatched rate is
 * reproduced at the wrong pitch rather than corrected.
 * @implNote This implementation fixes the public audio format at 16 kHz mono 16 bit PCM; the per frame
 * duration is left implicit in the sample count rather than carried alongside the samples.
 * @param pcm the signed 16 bit PCM samples, one channel; never {@code null}
 * @param ptsMicros the presentation timestamp in microseconds, which does not decrease within a
 *                  single call
 */
public record AudioFrame(short[] pcm, long ptsMicros) {
    /**
     * Validates the frame, rejecting a {@code null} sample buffer.
     *
     * <p>The buffer reference is retained as supplied; the array is neither copied nor defensively
     * cloned. When the engine produces the frame from its pooled playback path the buffer is borrowed
     * and reused for a later frame, so a consumer must neither retain nor mutate it past the next
     * {@link AudioInput#readAudio()}. No constraint is placed on the sample count: an empty buffer is a
     * legal zero length frame, and any positive length is accepted because the engine rechunks to its
     * fixed block size.
     *
     * @throws NullPointerException if {@code pcm} is {@code null}
     */
    public AudioFrame {
        Objects.requireNonNull(pcm, "pcm cannot be null");
    }
}
