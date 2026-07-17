package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomAdmitAck;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomAdmitStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomDenyAck;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomDenyStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomLeaveAck;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomLeaveStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomToggleAck;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomToggleStanza;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomUser;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.engine.control.event.CallLinkLobbySelfStateChanged;
import com.github.auties00.cobalt.calls.engine.control.event.WaitingRoomAdmitAcked;
import com.github.auties00.cobalt.calls.engine.control.event.WaitingRoomDenied;
import com.github.auties00.cobalt.calls.engine.control.event.WaitingRoomDenyAcked;
import com.github.auties00.cobalt.calls.engine.control.event.WaitingRoomStateChanged;
import com.github.auties00.cobalt.calls.engine.control.event.WaitingRoomToggleAcked;

/**
 * Drives the waiting room control: admitting and denying queued participants and toggling the lobby gate.
 *
 * <p>This controller owns the host side waiting room operations and surfaces the inbound waiting room
 * state to the host. {@link #admit(Jid)} releases a single queued participant and {@link #admitAll()}
 * releases every one; {@link #deny(Jid)} turns a participant away; {@link #setEnabled(boolean)} toggles the
 * call's waiting room gate. Each of those rides a request reply IQ and emits its ack event
 * ({@link WaitingRoomAdmitAcked}, {@link WaitingRoomDenyAcked}, {@link WaitingRoomToggleAcked}). The local
 * user's own lobby withdrawal is {@link #leave()} (and its link token form {@link #leave(String)}), which
 * rides a request reply IQ answered by a bare leave receipt and emits no host event.
 *
 * <p>Inbound state is delivered through {@link #onWaitingRoomUpdate(List)}, which emits a
 * {@link WaitingRoomStateChanged} carrying the current lobby occupants so the host can present an
 * admit or deny choice, and through {@link #onSelfLobbyState(WaitingRoomUserState)} and
 * {@link #onDenied()}, which surface the local user's own lobby progress as a
 * {@link CallLinkLobbySelfStateChanged} and a terminal {@link WaitingRoomDenied}. The controller is bound
 * to one call's identity, its IQ sender, and its event sink at construction; it owns no timers.
 *
 * @implNote This implementation maps each operation onto its waiting room IQ message type: admit sends
 * message type {@code 71} (an empty participant list admits every queued user), deny sends type {@code 73},
 * the gate toggle sends type {@code 69}, and the local user's lobby withdrawal sends type {@code 67},
 * answered by a type {@code 68} leave receipt. The host learns the lobby occupants from the inbound
 * {@code <waiting_room>} update; the {@link WaitingRoomUserState} constants model the per user lobby
 * progress ({@code outgoing}, {@code receipt}, {@code terminated}, {@code joined}).
 */
public final class WaitingRoomController {
    /**
     * The logger for {@link WaitingRoomController}.
     */
    private static final System.Logger LOGGER = Log.get(WaitingRoomController.class);

    /**
     * The call identity this controller stamps onto its waiting room actions.
     */
    private final CallControlContext context;

    /**
     * The request reply egress waiting room IQs are dispatched through.
     */
    private final CallLinkIqSender iqSender;

    /**
     * The event sink waiting room events are emitted into.
     */
    private final CallEventSink events;

    /**
     * Constructs a waiting room controller bound to a call, its IQ sender, and its event sink.
     *
     * @param context  the call identity to stamp onto waiting room actions; never {@code null}
     * @param iqSender the request reply egress to dispatch waiting room IQs through; never {@code null}
     * @param events   the event sink to emit waiting room events into; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public WaitingRoomController(CallControlContext context, CallLinkIqSender iqSender, CallEventSink events) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.iqSender = Objects.requireNonNull(iqSender, "iqSender cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
    }

    /**
     * Admits a single waiting room participant into the call.
     *
     * <p>Dispatches a waiting room admit IQ for the participant, parses the reply into a
     * {@link WaitingRoomAdmitAck}, and emits a {@link WaitingRoomAdmitAcked} with the admitted participants.
     *
     * @param userJid the device JID of the participant to admit; never {@code null}
     * @return the admit acknowledgement
     * @throws NullPointerException if {@code userJid} is {@code null}
     */
    public WaitingRoomAdmitAck admit(Jid userJid) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "admitting waiting room participant {0}", userJid);
        var request = WaitingRoomAdmitStanza.of(context.callId(), context.callCreator(), userJid);
        var ack = WaitingRoomAdmitAck.of(iqSender.sendForReply(request));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "admit acked, {0} users", ack.users().size());
        events.emit(new WaitingRoomAdmitAcked(ack.users()));
        return ack;
    }

    /**
     * Admits every waiting room participant into the call at once.
     *
     * <p>Dispatches the admit all waiting room IQ (an admit with no explicit participant list), parses the
     * reply into a {@link WaitingRoomAdmitAck}, and emits a {@link WaitingRoomAdmitAcked}.
     *
     * @return the admit acknowledgement
     */
    public WaitingRoomAdmitAck admitAll() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "admitting all waiting room participants for call {0}", context.callId());
        }
        var request = WaitingRoomAdmitStanza.all(context.callId(), context.callCreator());
        var ack = WaitingRoomAdmitAck.of(iqSender.sendForReply(request));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "admit all acked, {0} users", ack.users().size());
        events.emit(new WaitingRoomAdmitAcked(ack.users()));
        return ack;
    }

    /**
     * Denies a single waiting room participant admission to the call.
     *
     * <p>Dispatches a waiting room deny IQ for the participant, parses the reply into a
     * {@link WaitingRoomDenyAck}, and emits a {@link WaitingRoomDenyAcked} with the denied participants.
     *
     * @param userJid the device JID of the participant to deny; never {@code null}
     * @return the deny acknowledgement
     * @throws NullPointerException if {@code userJid} is {@code null}
     */
    public WaitingRoomDenyAck deny(Jid userJid) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "denying waiting room participant {0}", userJid);
        var request = WaitingRoomDenyStanza.of(context.callId(), context.callCreator(), userJid);
        var ack = WaitingRoomDenyAck.of(iqSender.sendForReply(request));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deny acked, {0} users", ack.users().size());
        events.emit(new WaitingRoomDenyAcked(ack.users()));
        return ack;
    }

    /**
     * Enables or disables the call's waiting room gate.
     *
     * <p>Dispatches a waiting room toggle IQ carrying the new gate setting, parses the reply into a
     * {@link WaitingRoomToggleAck}, and emits a {@link WaitingRoomToggleAcked} with the applied setting,
     * defaulting an absent echoed flag to the requested value.
     *
     * @param enabled {@code true} to enable the waiting room, {@code false} to disable it
     * @return the toggle acknowledgement
     */
    public WaitingRoomToggleAck setEnabled(boolean enabled) {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "setting waiting room enabled={0} for call {1}", enabled, context.callId());
        }
        var request = WaitingRoomToggleStanza.of(context.callId(), context.callCreator(), enabled);
        var ack = WaitingRoomToggleAck.of(iqSender.sendForReply(request));
        events.emit(new WaitingRoomToggleAcked(ack.enabled().orElse(enabled)));
        return ack;
    }

    /**
     * Withdraws the local user's pending join from the waiting room lobby.
     *
     * <p>Dispatches a {@code waiting_room_leave} IQ carrying the call's universal header and waits for the
     * relay's leave receipt, parsing it into a {@link WaitingRoomLeaveAck}. This is the local user's own
     * lobby withdrawal, sent while the local user is queued in a call link lobby and has not yet been
     * admitted or denied; it carries no call link token. Unlike the host side admit, deny, and toggle
     * operations, the leave emits no host event, because the relay answers it with a bare receipt rather
     * than a host facing state change.
     *
     * @return the leave acknowledgement
     */
    public WaitingRoomLeaveAck leave() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "leaving waiting room lobby for call {0}", context.callId());
        var request = WaitingRoomLeaveStanza.of(context.callId(), context.callCreator());
        return WaitingRoomLeaveAck.of(iqSender.sendForReply(request));
    }

    /**
     * Withdraws the local user's pending join from a call link lobby keyed by a link token.
     *
     * <p>Dispatches a {@code waiting_room_leave} IQ carrying the call's universal header and the call link
     * token the local user is leaving, and waits for the relay's leave receipt, parsing it into a
     * {@link WaitingRoomLeaveAck}. This is the link token form of {@link #leave()}, sent when the lobby the
     * local user is queued in is keyed by a call link. Like {@link #leave()} it emits no host event.
     *
     * @param linkToken the call link token of the lobby being left; never {@code null}
     * @return the leave acknowledgement
     * @throws NullPointerException if {@code linkToken} is {@code null}
     */
    public WaitingRoomLeaveAck leave(String linkToken) {
        Objects.requireNonNull(linkToken, "linkToken cannot be null");
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "leaving call link lobby {0} for call {1}",
                    new LogRedactable.Token(linkToken), context.callId());
        }
        var request = new WaitingRoomLeaveStanza(context.callId(), context.callCreator(), Optional.of(linkToken));
        return WaitingRoomLeaveAck.of(iqSender.sendForReply(request));
    }

    /**
     * Surfaces the current set of waiting room occupants from an inbound update.
     *
     * <p>Emits a {@link WaitingRoomStateChanged} carrying the supplied occupants so the host can present an
     * admit or deny choice; an empty list means no participant is currently waiting.
     *
     * @param waiting the participants currently waiting in the lobby; never {@code null}
     * @throws NullPointerException if {@code waiting} is {@code null}
     */
    public void onWaitingRoomUpdate(List<WaitingRoomUser> waiting) {
        Objects.requireNonNull(waiting, "waiting cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "waiting room update, {0} occupants", waiting.size());
        events.emit(new WaitingRoomStateChanged(waiting));
    }

    /**
     * Surfaces a change in the local user's own lobby state.
     *
     * <p>Emits a {@link CallLinkLobbySelfStateChanged} carrying the local user's new lobby state, used
     * while the local user is itself waiting in a call link lobby.
     *
     * @param state the local user's new lobby state; never {@code null}
     * @throws NullPointerException if {@code state} is {@code null}
     */
    public void onSelfLobbyState(WaitingRoomUserState state) {
        Objects.requireNonNull(state, "state cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "self lobby state -> {0}", state);
        events.emit(new CallLinkLobbySelfStateChanged(state));
    }

    /**
     * Surfaces that the local user's waiting room admission was denied.
     *
     * <p>Emits a terminal {@link WaitingRoomDenied} so the host can inform the user it will not be joining.
     */
    public void onDenied() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "waiting room admission denied for call {0}", context.callId());
        events.emit(new WaitingRoomDenied());
    }
}
