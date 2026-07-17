package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.engine.state.CallLinkState;
import com.github.auties00.cobalt.calls.signaling.link.LinkCreateAck;
import com.github.auties00.cobalt.calls.signaling.link.LinkCreateStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkEditAck;
import com.github.auties00.cobalt.calls.signaling.link.LinkEditStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkJoinAck;
import com.github.auties00.cobalt.calls.signaling.link.LinkJoinStanza;
import com.github.auties00.cobalt.calls.signaling.link.LinkQueryAck;
import com.github.auties00.cobalt.calls.signaling.link.LinkQueryStanza;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.call.CallLinkMedia;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import com.github.auties00.cobalt.calls.engine.control.event.CallLinkStateChanged;
import com.github.auties00.cobalt.calls.engine.control.event.LinkQueryAcked;

/**
 * Drives the call link control plane: previewing, editing, and joining a call through a link token.
 *
 * <p>This controller owns the call link query and join handshake and the link substate it advances
 * through. {@link #query(String, CallLinkMedia, CallLinkQueryAction)} resolves a link token to its
 * metadata, moving the substate from {@link CallLinkState#LINK_QUERY_SENT} to
 * {@link CallLinkState#LINK_QUERY_ACKED} and emitting the resolved link as a {@link LinkQueryAcked};
 * {@link #preview(String, CallLinkMedia)} and {@link #getInfo(String, CallLinkMedia)} are the
 * preview and edit conveniences over it. {@link #join(String, int)} requests admission, moving the
 * substate from {@link CallLinkState#LINK_JOIN_SENT} to {@link CallLinkState#LINK_JOIN_ACKED}.
 * {@link #edit(String, boolean)} changes a link's waiting room setting. Every substate change emits a
 * {@link CallLinkStateChanged}.
 *
 * <p>The link control plane rides request reply IQs addressed to the {@code call} service, so the
 * controller dispatches through the blocking {@link CallLinkIqSender} and parses the typed ack rather
 * than the signaling seam. The media negotiation payload a real join also attaches (audio
 * capabilities, the capability version, the end to end key, the video codec capability) is owned by the
 * offer and crypto units and is not built here. The link substate is held behind a lock; the controller
 * owns no timers.
 *
 * @implNote This implementation guards the mutable link substate with a {@link ReentrantLock}.
 */
public final class CallLinkController {
    /**
     * The logger for {@link CallLinkController}.
     */
    private static final System.Logger LOGGER = Log.get(CallLinkController.class);

    /**
     * The request reply egress link IQs are dispatched through.
     */
    private final CallLinkIqSender iqSender;

    /**
     * The event sink link events are emitted into.
     */
    private final CallEventSink events;

    /**
     * Guards the link substate.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * The current call link join substate.
     */
    private CallLinkState linkState = CallLinkState.NONE;

    /**
     * Constructs a call link controller bound to its IQ sender and event sink.
     *
     * @param iqSender the request reply egress to dispatch link IQs through; never {@code null}
     * @param events   the event sink to emit link events into; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public CallLinkController(CallLinkIqSender iqSender, CallEventSink events) {
        this.iqSender = Objects.requireNonNull(iqSender, "iqSender cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
    }

    /**
     * Resolves a call link token to its metadata under the given query action.
     *
     * <p>Moves the substate to {@link CallLinkState#LINK_QUERY_SENT}, dispatches a
     * {@code link_query} request carrying the token, media, and action verb, parses the reply into a
     * {@link LinkQueryAck}, moves the substate to {@link CallLinkState#LINK_QUERY_ACKED}, and emits a
     * {@link LinkQueryAcked} with the resolved link. Each substate change emits a
     * {@link CallLinkStateChanged}.
     *
     * @param token  the call link token to resolve; never {@code null}
     * @param media  the media kind the caller intends to use; never {@code null}
     * @param action the query action selecting a preview versus an edit lookup; never {@code null}
     * @return the resolved call link metadata
     * @throws NullPointerException if any argument is {@code null}
     */
    public LinkQueryAck query(String token, CallLinkMedia media, CallLinkQueryAction action) {
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(media, "media cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "querying call link {0}, media {1}, action {2}", new LogRedactable.Token(token), media, action);
        setLinkState(CallLinkState.LINK_QUERY_SENT);
        var request = new LinkQueryStanza(token, media, Optional.of(action.wireValue()),
                Optional.empty(), Optional.empty());
        var reply = iqSender.sendForReply(request);
        var ack = LinkQueryAck.of(reply);
        setLinkState(CallLinkState.LINK_QUERY_ACKED);
        events.emit(new LinkQueryAcked(ack.link()));
        return ack;
    }

    /**
     * Mints a fresh shareable call link token bound to a media kind and waiting room setting.
     *
     * <p>Dispatches a {@code link_create} request carrying the media kind and the requested waiting room
     * gate, parses the reply into a {@link LinkCreateAck}, and returns it; the ack surfaces the minted token
     * and composes to a {@link com.github.auties00.cobalt.wire.linked.call.CallLink} through
     * {@link LinkCreateAck#toCallLink()}. Unlike {@link #query(String, CallLinkMedia, CallLinkQueryAction)}
     * and {@link #join(String, int)}, creating a link mints a standalone token rather than advancing a call
     * through the query and join handshake, so it does not touch the {@link CallLinkState} join
     * substate and emits no {@link CallLinkStateChanged}; the minted link is delivered to the caller through
     * the return value.
     *
     * @param media              the media kind the link is created with; never {@code null}
     * @param waitingRoomEnabled {@code true} to request the link's waiting room gate at creation time
     * @return the create acknowledgement carrying the minted token
     * @throws NullPointerException if {@code media} is {@code null}
     */
    public LinkCreateAck create(CallLinkMedia media, boolean waitingRoomEnabled) {
        Objects.requireNonNull(media, "media cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "creating call link, media {0}, waiting room {1}", media, waitingRoomEnabled);
        var request = LinkCreateStanza.of(media, waitingRoomEnabled);
        var reply = iqSender.sendForReply(request);
        var ack = LinkCreateAck.of(reply);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created call link {0}", new LogRedactable.Token(ack.token()));
        return ack;
    }

    /**
     * Resolves a call link token for a passive preview before joining.
     *
     * <p>The {@link CallLinkQueryAction#PREVIEW} convenience over {@link #query(String, CallLinkMedia,
     * CallLinkQueryAction)}.
     *
     * @param token the call link token to resolve; never {@code null}
     * @param media the media kind the caller intends to use; never {@code null}
     * @return the resolved call link metadata
     * @throws NullPointerException if either argument is {@code null}
     */
    public LinkQueryAck preview(String token, CallLinkMedia media) {
        return query(token, media, CallLinkQueryAction.PREVIEW);
    }

    /**
     * Resolves a call link token for an edit lookup by the link owner.
     *
     * <p>The {@link CallLinkQueryAction#LINK_EDIT} convenience over {@link #query(String, CallLinkMedia,
     * CallLinkQueryAction)}; this is the lookup the owner issues before editing the link.
     *
     * @param token the call link token to resolve; never {@code null}
     * @param media the media kind the caller intends to use; never {@code null}
     * @return the resolved call link metadata
     * @throws NullPointerException if either argument is {@code null}
     */
    public LinkQueryAck getInfo(String token, CallLinkMedia media) {
        return query(token, media, CallLinkQueryAction.LINK_EDIT);
    }

    /**
     * Requests admission into the call a resolved call link token points at.
     *
     * <p>Moves the substate to {@link CallLinkState#LINK_JOIN_SENT}, dispatches a {@code link_join}
     * request carrying the token and join state leg, parses the reply into a {@link LinkJoinAck}, and moves
     * the substate to {@link CallLinkState#LINK_JOIN_ACKED}. Each substate change emits a
     * {@link CallLinkStateChanged}. The media negotiation payload a join also requires is supplied by the
     * offer and crypto units, not here.
     *
     * @param token     the call link token to join; never {@code null}
     * @param joinState the two step join handshake leg ({@code 1} or {@code 2}), or a negative value to
     *                  omit it
     * @return the join acknowledgement, carrying the call creator and any waiting room participants
     * @throws NullPointerException if {@code token} is {@code null}
     */
    public LinkJoinAck join(String token, int joinState) {
        Objects.requireNonNull(token, "token cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "joining call link {0}, join state {1}", new LogRedactable.Token(token), joinState);
        setLinkState(CallLinkState.LINK_JOIN_SENT);
        var request = new LinkJoinStanza(token, joinState);
        var reply = iqSender.sendForReply(request);
        var ack = LinkJoinAck.of(reply);
        setLinkState(CallLinkState.LINK_JOIN_ACKED);
        return ack;
    }

    /**
     * Edits a call link's waiting room setting.
     *
     * <p>Dispatches a {@code link_edit} request toggling the link's waiting room gate and parses the reply
     * into a {@link LinkEditAck}. The edit does not itself change the join substate.
     *
     * @param token               the call link token to edit; never {@code null}
     * @param waitingRoomEnabled  {@code true} to enable the link's waiting room, {@code false} to disable
     *                            it
     * @return the edit acknowledgement, echoing the applied waiting room setting
     * @throws NullPointerException if {@code token} is {@code null}
     */
    public LinkEditAck edit(String token, boolean waitingRoomEnabled) {
        Objects.requireNonNull(token, "token cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "editing call link {0}, waiting room {1}", new LogRedactable.Token(token), waitingRoomEnabled);
        var request = LinkEditStanza.ofWaitingRoom(token, waitingRoomEnabled);
        var reply = iqSender.sendForReply(request);
        return LinkEditAck.of(reply);
    }

    /**
     * Returns the current call link join substate.
     *
     * @return the current link substate; never {@code null}
     */
    public CallLinkState linkState() {
        lock.lock();
        try {
            return linkState;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets the link substate and emits a state change event when it differs.
     *
     * <p>Records the new substate under the lock; when it differs from the current one, emits a
     * {@link CallLinkStateChanged}.
     *
     * @param next the new link substate
     */
    private void setLinkState(CallLinkState next) {
        var changed = false;
        CallLinkState previous = null;
        lock.lock();
        try {
            if (linkState != next) {
                previous = linkState;
                linkState = next;
                changed = true;
            }
        } finally {
            lock.unlock();
        }
        if (changed) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call link state {0} -> {1}", previous, next);
            events.emit(new CallLinkStateChanged(next));
        }
    }
}
