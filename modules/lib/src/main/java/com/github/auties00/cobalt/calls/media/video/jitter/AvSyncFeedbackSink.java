package com.github.auties00.cobalt.calls.media.video.jitter;

import com.github.auties00.cobalt.calls.media.audio.neteq.LiveNetEq;

/**
 * Receives audio and video synchronisation corrections from the video timing path and applies them to
 * the audio jitter buffer's playout delay.
 *
 * <p>This is the one way seam from the video receive path to the audio receive path. The
 * {@link VideoTimingController} owns the measurement and produces an {@link AvSyncFeedback}; the audio
 * jitter buffer owns the playout delay state and implements this sink to ingest the correction. The two
 * sides are wired together at call bring up so neither has a compile time dependency on the other's
 * concrete type: the video unit depends only on this interface, and the audio unit supplies the
 * implementation. The audio jitter buffer's ingestion entry point is
 * {@link LiveNetEq#ingestAvSyncFeedbackMillis(int)}, so the implementation of this sink rounds
 * {@link AvSyncFeedback#correctionMs()} to whole milliseconds and forwards it there; the sign convention
 * matches, a positive correction lengthening the audio delay to wait for slower video.
 *
 * <p>The sink is invoked on the video receive thread, once per synchronisation interval, with a
 * correction that is already bounded and weighted. A correction of zero is delivered as a heartbeat and
 * the implementation may treat it as a no operation. Because the call arrives from the video thread while
 * the audio buffer is pulled from the playback thread, an implementation that mutates audio buffer state
 * must guard that state the same way the audio buffer guards its insert versus pull concurrency.
 *
 * @implNote This implementation rounds the floating point correction to whole milliseconds before
 * forwarding it, matching the integer granularity of {@link LiveNetEq#ingestAvSyncFeedbackMillis(int)}.
 * The interface is owned by the video jitter buffer and implemented by the audio {@link LiveNetEq},
 * so neither side references the other's concrete type; a {@link #noop()} implementation disables
 * lip sync correction without changing interoperability.
 */
@FunctionalInterface
public interface AvSyncFeedbackSink {
    /**
     * Applies one synchronisation correction to the audio jitter buffer's playout delay.
     *
     * <p>Invoked once per synchronisation interval from the video receive thread. A {@code feedback}
     * whose {@link AvSyncFeedback#correctionMs()} is zero is a heartbeat that an implementation may
     * treat as a no operation.
     *
     * @implSpec An implementation that mutates shared audio buffer state must guard it against the
     * concurrent playback pull, since this method runs on the video thread.
     * @param feedback the correction to apply, carrying the measured relative delay and the bounded
     *                 adjustment; never {@code null}
     */
    void applyAvSyncFeedback(AvSyncFeedback feedback);

    /**
     * Returns a sink that ignores every correction, disabling lip sync adjustment.
     *
     * <p>Used when audio and video synchronisation ingestion is off or when no audio buffer is wired to
     * the video path, so the {@link VideoTimingController} can run without a {@code null} check.
     *
     * @return a sink whose {@link #applyAvSyncFeedback(AvSyncFeedback)} does nothing
     */
    static AvSyncFeedbackSink noop() {
        return feedback -> {
        };
    }
}
