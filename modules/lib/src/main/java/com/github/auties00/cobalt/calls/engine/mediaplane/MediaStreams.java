package com.github.auties00.cobalt.calls.engine.mediaplane;

import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoInput;
import com.github.auties00.cobalt.calls.stream.VideoOutput;

/**
 * Carries the four application supplied media streams of one call from the service layer into the media
 * plane, grouped by their role in the media plane rather than by their public parameter name.
 *
 * <p>This is an internal engine value carried only between the call service and the media plane; embedders
 * never construct it. The public call API takes four streams: a capture audio source, a playback audio sink,
 * an optional capture video source, and an optional playback video sink. This record is the engine side
 * bundle the lifecycle controller threads into {@link MediaPlane#bringUp} so the brought up
 * {@link MediaPlane.Session} can drive the encode path from the capture sources and deliver decoded media to
 * the playback sinks. The fields are named for the direction of media flow:
 * <ul>
 *   <li>{@link #audioCapture()} and {@link #videoCapture()} are the sources the engine pulls local media
 *       from and encodes ({@link AudioOutput#takeAudio()} / {@link VideoOutput#takeVideo()}); they correspond to the
 *       public {@code audioOut} / {@code videoOut} parameters.</li>
 *   <li>{@link #audioPlayback()} and {@link #videoPlayback()} are the sinks the engine delivers decoded
 *       remote media to ({@link AudioInput#offerAudio(com.github.auties00.cobalt.calls.stream.AudioFrame)} /
 *       {@link VideoInput#offerVideo(com.github.auties00.cobalt.calls.stream.VideoFrame)}); they correspond to
 *       the public {@code audioIn} / {@code videoIn} parameters.</li>
 * </ul>
 *
 * <p>A stream backed by a device (a microphone capture source, a speaker playback sink) carries the platform
 * device behind the same interface, so the media plane needs no separate device path when a stream is
 * supplied: pulling the capture source captures from its device, and offering to the playback sink renders
 * to its device. A {@code null} field marks an absent stream; {@link #none()} supplies an all {@code null}
 * bundle for a bring up that has no application streams (the assembler's media plane probe), in which case
 * the media plane falls back to opening a platform capture and playback device directly. A call carrying
 * audio only leaves {@link #videoCapture()} and {@link #videoPlayback()} {@code null}.
 *
 * @param audioCapture  the capture source for local audio the engine encodes, or {@code null} to fall back
 *                      to a platform capture device
 * @param audioPlayback the playback sink for remote audio the engine renders decoded audio to, or
 *                      {@code null} to fall back to a platform playback device
 * @param videoCapture  the capture source for local video the engine encodes, or {@code null} on a call
 *                      carrying audio only or when no video source was supplied
 * @param videoPlayback the playback sink for remote video the engine renders decoded video to, or
 *                      {@code null} on a call carrying audio only or when no video sink was supplied
 */
public record MediaStreams(AudioOutput audioCapture, AudioInput audioPlayback,
                                 VideoOutput videoCapture, VideoInput videoPlayback) {
    /**
     * The shared empty bundle returned by {@link #none()}.
     *
     * <p>All four fields are {@code null}, so the media plane opens a platform capture and playback device
     * directly rather than driving an application stream.
     */
    private static final MediaStreams NONE = new MediaStreams(null, null, null, null);

    /**
     * Returns the bundle whose four fields are all {@code null} for a bring up with no application streams.
     *
     * <p>Used by a media plane bring up that carries no application capture or playback streams, such as the
     * engine assembler's probe; the media plane then opens a platform capture and playback device directly.
     *
     * @return the shared empty media streams bundle
     */
    public static MediaStreams none() {
        return NONE;
    }
}
