package com.github.auties00.cobalt.calls.stream;

import java.nio.file.Path;
import java.util.Objects;
import com.github.auties00.cobalt.calls.stream.video.DiscardVideoInput;
import com.github.auties00.cobalt.calls.stream.video.Y4mVideoInput;

/**
 * Defines the remote inbound video sink of a call: the application supplied destination for the
 * {@link VideoFrame}s the call engine decodes from the peer.
 *
 * <p>This is the read side of a call's video. The engine decodes each video frame received from the
 * peer and {@linkplain #offerVideo(VideoFrame) delivers} it to this sink; the embedder renders it to
 * whatever surface it chooses. The contract has two faces. The face the engine drives is
 * {@link #offerVideo(VideoFrame)} and {@link #shutdown()}, by which the engine fills the sink and signals
 * the end of the stream. The face the application drives is {@link #readVideo()}, by which the embedder
 * pulls received frames to render them; a file backed sink instead writes each frame to disk inside
 * {@link #offerVideo(VideoFrame)} and is not read from.
 *
 * <p>Frames carry planar 4:2:0 pixels as described by {@link VideoFrame}, and the resolution may
 * change frame to frame as the codec follows the peer's bandwidth adaptation. An implementation
 * decides its own buffering policy between the engine fill and the consumer; a buffered sink typically
 * prefers freshness, dropping the oldest buffered frame rather than stalling the decoder when the
 * renderer falls behind. The application never ends the sink itself; the engine invokes
 * {@link #shutdown()} when the call ends.
 *
 * @apiNote An embedder implements this interface to render received video, or obtains a bundled
 * implementation from one of the factories on this type: {@link #discard()} to drop the received video and
 * {@link #toY4m(Path)} to record it to a raw Y4M file. The {@link #offerVideo(VideoFrame)} and
 * {@link #shutdown()} methods belong to the engine; application code drives a custom sink through
 * {@link #readVideo()} and never calls the pair the engine drives directly.
 */
public interface VideoInput extends AudioInput {
    /**
     * Returns a sink that discards the received video.
     *
     * <p>Every {@link #offerVideo(VideoFrame)} is dropped and {@link #readVideo()} yields nothing, so this is the
     * sink to install when a call needs no inbound video, and is the default video sink. {@link #readVideo()}
     * blocks until the call ends and then returns {@code null}, so a consumer that drains it in a loop
     * terminates cleanly.
     *
     * @return a discarding sink
     */
    static VideoInput discard() {
        return new DiscardVideoInput();
    }

    /**
     * Returns a sink that records the received video to a raw YUV4MPEG2 (Y4M) file.
     *
     * <p>Each {@link #offerVideo(VideoFrame)} appends the frame uncompressed, so no encoder is involved and a
     * player such as {@code ffplay} reads the file back directly; the file is finalized when the call ends.
     * The recording is fixed to the first frame's resolution and carries no per frame timing, so a later
     * frame of a different resolution is dropped and the frame rate is nominal. The application does not
     * read a sink bound to a file.
     *
     * @param path the Y4M file to write
     * @return a sink bound to the file
     * @throws NullPointerException  if {@code path} is {@code null}
     * @throws IllegalStateException if the file cannot be created
     */
    static VideoInput toY4m(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        return new Y4mVideoInput(path);
    }

    /**
     * Delivers one decoded remote frame for the application to consume.
     *
     * <p>Invoked by the engine for each frame it decodes from the peer. A buffered sink enqueues the
     * frame for {@link #readVideo()}, dropping the oldest buffered frame if the renderer is behind. After
     * {@link #shutdown()} has run an implementation discards the frame. The frame is never
     * {@code null}.
     *
     * <p>The frame's {@linkplain VideoFrame#pixels() pixel buffer} may be borrowed from a pool the engine
     * reuses across frames, so it is valid only for the duration of this call. A sink that renders or writes
     * the frame synchronously before returning needs no copy; a sink that buffers it for a later
     * {@link #readVideo()} or hands it to another thread copies the pixels out first (or copies the whole frame),
     * since the engine may refill the buffer on a later decode.
     *
     * @param frame the decoded frame; never {@code null}
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    void offerVideo(VideoFrame frame);

    /**
     * Returns the next frame of received remote video, blocking until one is available, or
     * {@code null} once the call has ended.
     *
     * <p>Returns frames previously delivered through {@link #offerVideo(VideoFrame)}. The method blocks
     * while no frame is ready and returns {@code null} exactly once the sink has been
     * {@linkplain #shutdown() ended} and drained.
     *
     * <p>The returned frame's {@linkplain VideoFrame#pixels() pixel buffer} is borrowed from a pool the
     * engine reuses across frames: it is valid only until the next call to this method on the same
     * input, after which the engine may refill and re offer it. A consumer that needs the pixels beyond
     * the next read copies them out; it must never retain the returned array past the next read nor
     * mutate it.
     *
     * @return the next frame, or {@code null} once the stream has ended
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    VideoFrame readVideo() throws InterruptedException;

    /**
     * Ends the sink, unblocking a pending {@link #readVideo()}.
     *
     * <p>Invoked by the engine when the call ends. After it runs, {@link #readVideo()} returns {@code null}
     * once drained. Implementations make this idempotent, since the engine may signal teardown more
     * than once during a racing shutdown.
     */
    void shutdown();
}
