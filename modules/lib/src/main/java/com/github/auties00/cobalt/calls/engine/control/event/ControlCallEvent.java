package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.control.CallControlContext;
import com.github.auties00.cobalt.calls.engine.control.CallEventSink;

/**
 * Represents the typed, decoded payload of one in call control event an in call controller emits.
 *
 * <p>The in call controllers (mute, video, raise hand, reactions, screen share, waiting room, call link,
 * tone, transcription, grid ranking) each react to a local control action or an inbound control report by
 * publishing a typed event describing the call visible change. This sealed interface is the common
 * supertype of those events: each permitted record holds the fields its event family carries as immutable
 * Java components and reports the {@linkplain #type() event type} the engine selected. A controller hands
 * the event to its {@link CallEventSink}, which gates it and fans it out to the registered listeners.
 *
 * <p>The hierarchy is keyed by control event family and is distinct from the host facing
 * {@link com.github.auties00.cobalt.calls.engine.event.CallEvent} bus surface: where {@code CallEvent}
 * carries the owning call identifier and projects onto a public listener callback, a
 * {@code ControlCallEvent} is the finer grained control surface payload a controller produces against a
 * single {@link CallControlContext}, so it carries only the participant and state the control operation
 * changed. The permitted variants are exhaustive, so a {@code switch} over a {@code ControlCallEvent} needs
 * no default branch, and every record validates its non-{@code null} components at construction.
 *
 * <p>Each permitted record corresponds to one {@link CallEventType} that the in call controllers raise
 * through the engine's generic event dispatcher. Each record carries only the decoded fields the
 * corresponding controller produces and the listener surface needs.
 *
 * @see CallEventSink
 * @see CallEventType
 */
public sealed interface ControlCallEvent
        permits CallGridRankingChanged,
        CallLinkLobbySelfStateChanged,
        CallLinkStateChanged,
        LinkQueryAcked,
        MuteByAnotherParticipant,
        MuteRequestFailed,
        MuteStateChanged,
        PeerVideoPermissionChanged,
        PlayCallTone,
        RaiseHandStateChanged,
        ReactionStateChanged,
        ScreenShareEvent,
        TranscriptReceived,
        VideoStateChanged,
        WaitingRoomAdmitAcked,
        WaitingRoomDenied,
        WaitingRoomDenyAcked,
        WaitingRoomStateChanged,
        WaitingRoomToggleAcked {
    /**
     * Returns the event type that selected this control event.
     *
     * <p>The value is the dispatch discriminator the engine branched on, decoded into a
     * {@link CallEventType}; it is the discriminator the {@link CallEventSink} consults for its should emit
     * gate and for listener routing.
     *
     * @return the event type, never {@code null}
     */
    CallEventType type();
}
