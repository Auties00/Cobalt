package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.LiveTranscriptionInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.Objects;
import com.github.auties00.cobalt.calls.engine.control.event.TranscriptReceived;

/**
 * Surfaces inbound live transcription fragments as transcript events.
 *
 * <p>Live transcription travels over the call's application data side channel, one
 * {@link LiveTranscriptionInfo} per caption fragment. This controller maps each inbound fragment to a
 * {@link TranscriptReceived} event carrying the speaking participant, the caption text, the source
 * language, and the sequence id the host uses to correlate and order the fragments of an utterance. It is
 * a thin bridge from the application data layer to the event bus; it owns no state and no timers, and it
 * is bound to its event sink at construction.
 *
 * <p>The {@link LiveTranscriptionInfo} body carries only the transcript id, caption, and language, never
 * the speaker; the speaking participant is known only from the application data demultiplexer's sender
 * attribution. {@link #onTranscript(Jid, LiveTranscriptionInfo)} is therefore the sink the demultiplexer
 * binds, pairing each inbound fragment with the device {@link Jid} it arrived from rather than surfacing a
 * fragment stream with no sender.
 */
public final class TranscriptionController {
    /**
     * The logger for {@link TranscriptionController}.
     */
    private static final System.Logger LOGGER = Log.get(TranscriptionController.class);

    /**
     * The event sink transcript events are emitted into.
     */
    private final CallEventSink events;

    /**
     * Constructs a live transcription controller bound to its event sink.
     *
     * @param events the event sink to emit transcript events into; never {@code null}
     * @throws NullPointerException if {@code events} is {@code null}
     */
    public TranscriptionController(CallEventSink events) {
        this.events = Objects.requireNonNull(events, "events cannot be null");
    }

    /**
     * Surfaces one inbound live transcription fragment as a transcript event.
     *
     * <p>Emits a {@link TranscriptReceived} carrying the speaking participant, the fragment's caption text,
     * its source language, and its sequence id (the transcript id used to correlate updates). A fragment
     * whose caption is absent is dropped, since there is no text to surface; an absent transcript id
     * surfaces as sequence {@code 0}.
     *
     * @param sender   the device {@link Jid} of the speaking participant, supplied by the application data
     *                 layer's sender attribution; never {@code null}
     * @param fragment the inbound transcription fragment; never {@code null}
     * @return {@code true} when a transcript event was emitted, {@code false} when the fragment carried no
     *         caption and was dropped
     * @throws NullPointerException if {@code sender} or {@code fragment} is {@code null}
     */
    public boolean onTranscript(Jid sender, LiveTranscriptionInfo fragment) {
        Objects.requireNonNull(sender, "sender cannot be null");
        Objects.requireNonNull(fragment, "fragment cannot be null");
        var caption = fragment.caption().orElse(null);
        if (caption == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping transcript fragment from {0}, no caption", sender);
            return false;
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "transcript fragment from {0}, id={1}", sender,
                    fragment.transcriptId().orElse(0));
        }
        events.emit(new TranscriptReceived(sender, caption, fragment.language(),
                fragment.transcriptId().orElse(0)));
        return true;
    }
}
