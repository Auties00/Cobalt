package com.github.auties00.cobalt.calls.engine.event;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.WhatsAppListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.CallInteraction;
import com.github.auties00.cobalt.wire.linked.call.CallScreenShareState;
import com.github.auties00.cobalt.listener.linked.LinkedCallEndedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallInteractionListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallLinkAdmittedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallLinkDeniedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallLinkLobbyJoinRequestListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallMuteChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallParticipantsChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallPeerStateChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallPeerVideoPermissionChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallPreacceptListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallScreenShareChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallTranscriptListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallVideoStateChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedCallVideoUpgradeRequestListener;

import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Publishes the voice and video call engine's events to the host, gating the engine's internal ids and
 * fanning the survivors out to registered listeners.
 *
 * <p>Every call engine unit (the lifecycle controller, the state machine, the controllers active during a
 * call) constructs a typed {@link CallEvent} and hands it here rather than touching the listener registry
 * directly. The bus owns two responsibilities: it applies the gate that suppresses the engine's internal
 * and diagnostic event ids, and it fans the surviving host facing events out to the registered listeners.
 * Concentrating both behind this seam keeps the producers decoupled from the listener bus and gives the
 * engine exactly one place where an event becomes observable.
 *
 * <p>{@link #shouldEmit(CallEventType)} is the gate: it admits only the host facing event ids and drops
 * every internal lifecycle and diagnostic id. {@link #emit(CallEvent)} runs that gate and then, for each
 * interested registered listener, maps the typed {@link CallEvent} onto the matching
 * {@code LinkedCall*Listener} callback and dispatches it on its own virtual thread, so a slow or throwing
 * listener can neither stall the call engine nor block the fan out to the other listeners.
 *
 * <p>The bus reads the registered listeners from the client's store on every emit, so listeners added or
 * removed during a call take effect immediately, and it iterates the unmodifiable snapshot the store
 * returns rather than holding any registry of its own. It maps each typed event to zero or more public
 * callbacks: a state change is internal and surfaces no callback (the lifecycle controller decides the
 * user facing consequence and emits the dedicated end or peer state event), an admin forced mute surfaces
 * both as a mute change and as a peer mute interaction, and a {@link CallEvent.Generic} event passes the
 * gate yet maps to no public callback because its typed payload is open. Every fan out is fire and forget;
 * the bus never blocks on a listener and never propagates a listener exception back to the engine.
 *
 * @implNote This implementation models the gate as an allow set ({@link #HOST_FACING}) rather than a deny
 * mask: it enumerates the host facing ids explicitly and suppresses everything else by default, so a
 * wrongly classified id is a missing notification rather than a leak of an engine internal id to an
 * application listener. Each host facing event is dispatched on its own virtual thread through
 * {@link Thread#startVirtualThread(Runnable)}.
 */
public final class LiveCallEventBus {
    /**
     * The logger for {@link LiveCallEventBus}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCallEventBus.class);

    /**
     * The event types the gate admits to listeners.
     *
     * <p>This is the union of the host facing event ids and the signaling driven lifecycle ids the typed
     * {@link CallEvent} records surface (the incoming offer, the preaccept, and the terminate end). Every
     * {@link CallEventType} not in this set is treated as an internal or diagnostic id and suppressed by
     * {@link #shouldEmit(CallEventType)}.
     *
     * <p>The set is a superset of every {@link CallEventType} a {@link CallEvent} record reports through
     * {@link CallEvent#type()}, so {@link #shouldEmit(CallEventType)} admits every event this bus can be
     * handed.
     */
    private static final Set<CallEventType> HOST_FACING = Collections.unmodifiableSet(EnumSet.of(
            CallEventType.CALL_OFFER_RECEIVED,
            CallEventType.CALL_PREACCEPT_RECEIVED,
            CallEventType.CALL_TERMINATE_RECEIVED,
            CallEventType.CALL_STATE_CHANGED,
            CallEventType.GROUP_INFO_CHANGED,
            CallEventType.CALL_WAITING_STATE_CHANGED,
            CallEventType.MUTE_STATE_CHANGED,
            CallEventType.CALL_FATAL,
            CallEventType.UPDATE_JOINABLE_CALL_LOG,
            CallEventType.PLAY_CALL_TONE,
            CallEventType.MUTE_BY_ANOTHER_PARTICIPANT,
            CallEventType.CALL_LINK_STATE_CHANGED,
            CallEventType.MUTE_REQUEST_FAILED,
            CallEventType.CALL_GRID_RANKING_CHANGED,
            CallEventType.VIDEO_RENDERING_STATE_CHANGED,
            CallEventType.USER_REMOVED,
            CallEventType.SCREEN_SHARE,
            CallEventType.LID_CALLER_DISPLAY_INFO,
            CallEventType.UPDATE_1ON1_CALL_LOG,
            CallEventType.CALL_LINK_LOBBY_SELF_STATE_CHANGED,
            CallEventType.REACTION_STATE_CHANGED,
            CallEventType.VIDEO_STATE_CHANGED,
            CallEventType.PEER_VIDEO_PERMISSION_CHANGED,
            CallEventType.RAISE_HAND_STATE_CHANGED,
            CallEventType.TRANSCRIPT_RECEIVED,
            CallEventType.WAITING_ROOM_DENIED,
            CallEventType.WAITING_ROOM_STATE_CHANGED,
            CallEventType.BOT_PRESENCE_CHANGED,
            CallEventType.LINK_QUERY_ACKED,
            CallEventType.WAITING_ROOM_ADMIT_ACKED,
            CallEventType.WAITING_ROOM_DENY_ACKED,
            CallEventType.CALL_ADD_EXTENSION_RECEIVED,
            CallEventType.CALL_ADD_EXTENSION_SUCCESS,
            CallEventType.CALL_ADD_EXTENSION_FAILURE));

    /**
     * The client whose listener registry receives the fanned out events and whose handle the callbacks
     * receive.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Constructs a bus bound to the client it dispatches through.
     *
     * @param whatsapp the client whose store supplies the listeners and whose handle the public
     *                 callbacks receive
     * @throws NullPointerException if {@code whatsapp} is {@code null}
     */
    public LiveCallEventBus(LinkedWhatsAppClient whatsapp) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
    }

    /**
     * Returns whether an event of the given type is fanned out to listeners rather than suppressed.
     *
     * <p>Returns {@code true} only for the host facing event ids and {@code false} for the engine's
     * internal lifecycle and diagnostic ids. The decision depends solely on the {@link CallEventType}, so
     * it is stable for a given type and may be consulted by a producer to skip building a payload it knows
     * will be dropped.
     *
     * @implNote This implementation tests the type against {@link #HOST_FACING}; a {@link CallEvent.Generic}
     * event therefore passes when its carried type is host facing even though no typed callback exists
     * for it.
     * @param type the event type to test
     * @return {@code true} if an event of this type is emitted to listeners, {@code false} if suppressed
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public boolean shouldEmit(CallEventType type) {
        Objects.requireNonNull(type, "type cannot be null");
        return HOST_FACING.contains(type);
    }

    /**
     * Publishes a call event, gating it and fanning the survivors out to the registered listeners.
     *
     * <p>First consults {@link #shouldEmit(CallEventType)} for the event's {@link CallEvent#type()} and
     * returns without side effect when the gate rejects it. For a gated in event it maps the typed
     * {@link CallEvent} onto the matching host callbacks and invokes each interested listener on its own
     * virtual thread, so that a slow or throwing listener can neither stall the call engine nor abort the
     * fan out to the other listeners.
     *
     * @implNote This implementation gates on {@link CallEvent#type()}, then switches over the sealed
     * {@link CallEvent} hierarchy to route each family to its public callbacks. The
     * {@link CallEvent.StateChanged}, {@link CallEvent.CallLinkStateChanged}, and
     * {@link CallEvent.Generic} families have no public callback and so reach the gate but dispatch
     * nothing. The exhaustive {@code switch} needs no default branch.
     * @param event the event to publish
     * @throws NullPointerException if {@code event} is {@code null}
     */
    public void emit(CallEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        if (!shouldEmit(event.type())) {
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "call event {0} for call {1} suppressed by gate",
                        event.type(), event.callId());
            }
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call event {0} for call {1} emitted", event.type(), event.callId());
        switch (event) {
            case CallEvent.IncomingOffer e -> dispatch(LinkedCallListener.class,
                    listener -> listener.onCall(whatsapp, e.incoming()));
            case CallEvent.Preaccept e -> dispatch(LinkedCallPreacceptListener.class,
                    listener -> listener.onCallPreaccept(whatsapp, e.callId(), e.peerJid()));
            case CallEvent.PeerStateChanged e -> dispatch(LinkedCallPeerStateChangedListener.class,
                    listener -> listener.onCallPeerStateChanged(whatsapp, e.callId(), e.peerJid(), e.state()));
            case CallEvent.MuteChanged e -> emitMuteChanged(e);
            case CallEvent.VideoStateChanged e -> dispatch(LinkedCallVideoStateChangedListener.class,
                    listener -> listener.onCallVideoStateChanged(
                            whatsapp, e.callId(), e.participantJid(), e.enabled()));
            case CallEvent.VideoUpgradeRequest e -> dispatch(LinkedCallVideoUpgradeRequestListener.class,
                    listener -> listener.onCallVideoUpgradeRequest(whatsapp, e.callId(), e.peerJid()));
            case CallEvent.Reaction e -> {
                var interaction = e.toInteraction();
                dispatch(LinkedCallInteractionListener.class,
                        listener -> listener.onCallInteraction(
                                whatsapp, e.callId(), e.participantJid(), interaction));
            }
            case CallEvent.RaiseHand e -> {
                var interaction = e.toInteraction();
                dispatch(LinkedCallInteractionListener.class,
                        listener -> listener.onCallInteraction(
                                whatsapp, e.callId(), e.participantJid(), interaction));
            }
            case CallEvent.WaitingRoomJoinRequest e -> dispatch(LinkedCallLinkLobbyJoinRequestListener.class,
                    listener -> listener.onCallLinkLobbyJoinRequest(whatsapp, e.link(), e.peer()));
            case CallEvent.WaitingRoomAdmitted e -> dispatch(LinkedCallLinkAdmittedListener.class,
                    listener -> listener.onCallLinkAdmitted(whatsapp, e.link()));
            case CallEvent.WaitingRoomDenied e -> dispatch(LinkedCallLinkDeniedListener.class,
                    listener -> listener.onCallLinkDenied(whatsapp, e.link()));
            case CallEvent.ParticipantsChanged e -> dispatch(LinkedCallParticipantsChangedListener.class,
                    listener -> listener.onCallParticipantsChanged(
                            whatsapp, e.callId(), e.groupJid(), e.participants(), e.added()));
            case CallEvent.Ended e -> dispatch(LinkedCallEndedListener.class,
                    listener -> listener.onCallEnded(
                            whatsapp, e.callId(), e.from().orElse(null), e.reason()));
            case CallEvent.StateChanged ignored -> {
                // A lifecycle internal transition; the lifecycle controller emits the dedicated
                // user facing event the transition implies, so no public callback fires here.
            }
            case CallEvent.CallLinkStateChanged ignored -> {
                // A bare link substate advance carries no JoinableCallLink, so the admitted and denied
                // callbacks are emitted from the WaitingRoomAdmitted and WaitingRoomDenied events that do
                // carry it; the lifecycle controller consumes the substate advance itself.
            }
            case CallEvent.ScreenShareChanged e -> {
                var state = switch (e.state()) {
                    case STARTED -> CallScreenShareState.STARTED;
                    case STOPPED -> CallScreenShareState.STOPPED;
                    case FAILED -> CallScreenShareState.FAILED;
                };
                dispatch(LinkedCallScreenShareChangedListener.class,
                        listener -> listener.onCallScreenShareChanged(
                                whatsapp, e.callId(), e.participantJid(), state));
            }
            case CallEvent.PeerVideoPermissionChanged e -> dispatch(
                    LinkedCallPeerVideoPermissionChangedListener.class,
                    listener -> listener.onCallPeerVideoPermissionChanged(
                            whatsapp, e.callId(), e.peerJid(), e.permitted()));
            case CallEvent.Transcript e -> dispatch(LinkedCallTranscriptListener.class,
                    listener -> listener.onCallTranscript(
                            whatsapp, e.callId(), e.speakerJid(), e.text(), e.language(), e.sequence()));
            case CallEvent.Generic ignored -> {
                // Untyped host-facing ids reach the bus as a Generic event and are intentionally not
                // surfaced to a public callback: only the typed CallEvent families map to a listener. There
                // is deliberately no raw onCallRawEvent egress, so a host-facing id that carries no typed
                // CallEvent record and dedicated listener is dropped here rather than delivered untyped.
            }
        }
    }

    /**
     * Fans a mute change out to the mute changed callback and, when admin forced, to the interaction
     * callback as a peer mute request.
     *
     * <p>A self initiated change reaches only {@link LinkedCallMuteChangedListener}. An admin forced mute
     * reaches that callback too, because the participant's mic state did change, and additionally surfaces
     * as a {@link CallInteraction.PeerMuteRequest} on
     * {@link LinkedCallInteractionListener}, the facade through which a forced mute is exposed.
     *
     * @param event the mute change to fan out
     */
    private void emitMuteChanged(CallEvent.MuteChanged event) {
        dispatch(LinkedCallMuteChangedListener.class,
                listener -> listener.onCallMuteChanged(
                        whatsapp, event.callId(), event.participantJid(), event.muted()));
        if (event.byAnotherParticipant()) {
            var interaction = new CallInteraction.PeerMuteRequest(
                    event.participantJid().toString(), Optional.empty());
            dispatch(LinkedCallInteractionListener.class,
                    listener -> listener.onCallInteraction(
                            whatsapp, event.callId(), event.participantJid(), interaction));
        }
    }

    /**
     * Dispatches an action to every registered listener of a given event interface type, each on its own
     * virtual thread.
     *
     * <p>The current listener snapshot is read from the client's store, so a listener added or removed
     * during a call is reflected on the next emit. Each matching listener's callback is run through
     * {@link Thread#startVirtualThread(Runnable)} and wrapped so a listener exception is logged and
     * swallowed rather than propagated, isolating one faulty listener from the engine and from the other
     * listeners.
     *
     * @param listenerType the event interface type whose registered listeners receive the action
     * @param action       the callback invocation to run for each matching listener
     * @param <T>          the event interface type
     */
    private <T extends WhatsAppListener> void dispatch(Class<T> listenerType, Consumer<T> action) {
        for (var listener : whatsapp.store().listeners()) {
            if (listenerType.isInstance(listener)) {
                var typed = listenerType.cast(listener);
                Thread.startVirtualThread(() -> {
                    try {
                        action.accept(typed);
                    } catch (RuntimeException exception) {
                        if (Log.WARNING) {
                            LOGGER.log(Level.WARNING, "call event listener threw "
                                    + exception.getClass().getSimpleName(), exception);
                        }
                    }
                });
            }
        }
    }
}
