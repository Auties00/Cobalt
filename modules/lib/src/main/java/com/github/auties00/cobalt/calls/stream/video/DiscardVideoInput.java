package com.github.auties00.cobalt.calls.stream.video;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoInput;

/**
 * Discards the remote video, and the remote audio, of a call.
 *
 * <p>This is the {@link VideoInput} returned by {@link VideoInput#discard()}: every
 * {@link #offerVideo(VideoFrame)} is dropped and nothing is buffered, so it is the sink to install when a
 * call needs no inbound video, and it is the default video sink. It is never read from; {@link #readVideo()}
 * blocks until the call ends and then returns {@code null}, so a consumer that happens to drain it in a loop
 * terminates cleanly once the call ends.
 *
 * <p>Because a {@link VideoInput} is also an {@link AudioInput}, this sink drops the inbound audio the same
 * way: {@link #offerAudio(AudioFrame)} is dropped and {@link #readAudio()} blocks until the call ends and
 * returns {@code null}. The one {@link #shutdown()} ends both sides, sharing a single latch, so a consumer
 * draining either side terminates cleanly.
 */
public final class DiscardVideoInput implements VideoInput {
    /**
     * Released by {@link #shutdown()} so a pending {@link #readVideo()} or {@link #readAudio()} returns.
     */
    private final CountDownLatch done = new CountDownLatch(1);

    /**
     * {@inheritDoc}
     *
     * <p>Drops the frame.
     *
     * @param frame {@inheritDoc}
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    @Override
    public void offerVideo(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Blocks until {@link #shutdown()} and then returns {@code null}, since nothing is ever buffered.
     *
     * @return {@code null} once the call has ended
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    @Override
    public VideoFrame readVideo() throws InterruptedException {
        done.await();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Drops the frame.
     *
     * @param frame {@inheritDoc}
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    @Override
    public void offerAudio(AudioFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Blocks until {@link #shutdown()} and then returns {@code null}, since nothing is ever buffered.
     *
     * @return {@code null} once the call has ended
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    @Override
    public AudioFrame readAudio() throws InterruptedException {
        done.await();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wakes a pending {@link #readVideo()} or {@link #readAudio()}. Idempotent.
     */
    @Override
    public void shutdown() {
        done.countDown();
    }
}
