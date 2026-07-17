package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ControlCallEvent} delivering one fragment of in call live transcription.
 *
 * <p>Live transcription emits caption fragments over the call's application data channel. The engine
 * surfaces each fragment as this event, carrying the speaking {@link #participant() participant} device
 * JID, the transcribed {@link #text() text}, the ISO {@link #language() language} code of the source
 * speech, and the {@link #sequence() sequence id} the host uses to correlate and order the fragments of a
 * single utterance. The language is optional because a source fragment may omit it.
 *
 * @param participant the device JID of the speaking participant; never {@code null}
 * @param text        the transcribed caption text; never {@code null}
 * @param language    the ISO language code of the source speech, or empty when absent; never {@code null}
 * @param sequence    the sequence id correlating and ordering the fragments of one utterance
 */
public record TranscriptReceived(Jid participant, String text, Optional<String> language, long sequence)
        implements ControlCallEvent {
    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code participant}, {@code text}, or {@code language} is
     *                              {@code null}
     */
    public TranscriptReceived {
        Objects.requireNonNull(participant, "participant cannot be null");
        Objects.requireNonNull(text, "text cannot be null");
        Objects.requireNonNull(language, "language cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#TRANSCRIPT_RECEIVED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.TRANSCRIPT_RECEIVED;
    }
}
