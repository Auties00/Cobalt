package com.github.auties00.cobalt.calls.engine.context;

import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Buffers an inbound group or call link offer and the signaling that arrives after it, holding the call
 * in a pending state until the local user joins or declines it.
 *
 * <p>When an offer arrives that the user must explicitly join (a group call, a waiting room or lobby
 * call, or a call link join) rather than answer immediately, the voip engine does not allocate a full
 * call context: it allocates a lightweight pending context that remembers the original offer and queues
 * every later signaling message for that call so nothing is lost in the interval before the user acts.
 * This class is that pending context. It retains the decoded {@link OfferStanza}, appends each subsequent
 * {@link CallMessage} in arrival order through {@link #buffer(CallMessage)}, and tracks the pending
 * lifecycle through {@link State}. When the user joins, the buffered messages are drained in arrival
 * order through {@link #drain()} and replayed against the freshly allocated call context; when the user
 * declines, {@link #reject()} moves the pending call to its terminal state so the queued messages are
 * discarded.
 *
 * <p>The pending call projects the original offer onto the user facing {@link IncomingCall} through
 * {@link #toIncomingCall(Instant, boolean)} so the host can surface the waiting room or lobby entry to
 * the user without seeing the raw stanza. The projection reads the offer's call identifier, caller, group
 * membership, and video flag.
 *
 * <p>The class is thread safe: a single lock guards the buffered message list and the {@link State}, so
 * the socket reader appending a message and the join path draining the buffer never race. The retained
 * {@link OfferStanza} and {@link CallMessage} records are immutable, so draining the buffer to the caller
 * leaks no mutable state.
 *
 * @implNote This implementation retains the offer and the queued {@link CallMessage} records directly
 * rather than cloning them into a buffer pool, because the records are immutable; the {@link State} enum
 * models the pending lifecycle, with {@link State#REJECTED} as its terminal value.
 */
public final class PendingCall {
    /**
     * The logger for {@link PendingCall}.
     */
    private static final System.Logger LOGGER = Log.get(PendingCall.class);

    /**
     * Enumerates the lifecycle states of a pending call.
     *
     * <p>A pending call is created {@link #PENDING}; it leaves that state when the user joins (the engine
     * promotes it to a full call context and replays the buffer) or declines it ({@link #REJECTED}). The
     * states are deliberately coarse because the pending context exists only to hold the offer and its
     * queued messages until the user acts.
     */
    public enum State {
        /**
         * The pending call is buffering signaling and awaiting the user's decision to join or decline.
         */
        PENDING,

        /**
         * The pending call has been declined by the user; its buffered messages are discarded and it
         * emits no further signaling.
         */
        REJECTED
    }

    /**
     * The decoded offer that opened this pending call.
     */
    private final OfferStanza offer;

    /**
     * Guards the buffered message list and the {@link #state}.
     */
    private final Object lock;

    /**
     * Holds the signaling messages buffered after the offer, in arrival order, drained on join.
     */
    private final List<CallMessage> buffered;

    /**
     * The pending call's current lifecycle state.
     */
    private State state;

    /**
     * Constructs a pending call from its opening offer in the {@link State#PENDING} state.
     *
     * @param offer the decoded offer that opened the pending call; must not be {@code null}
     * @throws NullPointerException if {@code offer} is {@code null}
     */
    public PendingCall(OfferStanza offer) {
        this.offer = Objects.requireNonNull(offer, "offer cannot be null");
        this.lock = new Object();
        this.buffered = new ArrayList<>();
        this.state = State.PENDING;
    }

    /**
     * Returns the call identifier of this pending call.
     *
     * @return the offer's call identifier, never {@code null}
     */
    public String callId() {
        return offer.callId().orElseThrow();
    }

    /**
     * Returns the decoded offer that opened this pending call.
     *
     * @return the opening offer, never {@code null}
     */
    public OfferStanza offer() {
        return offer;
    }

    /**
     * Returns the pending call's current lifecycle state.
     *
     * @return the current state, never {@code null}
     */
    public State state() {
        synchronized (lock) {
            return state;
        }
    }

    /**
     * Buffers a signaling message that arrived after the offer for replay once the user joins.
     *
     * <p>Appends the message to the arrival order queue when the call is still {@link State#PENDING};
     * a message that arrives after the call has been {@link State#REJECTED} is dropped because the pending
     * context no longer replays anything. The buffered {@link CallMessage} is immutable, so no defensive
     * copy is made.
     *
     * @param message the signaling message to buffer; must not be {@code null}
     * @return {@code true} if the message was buffered, {@code false} if the pending call was already
     *         rejected and the message was dropped
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public boolean buffer(CallMessage message) {
        Objects.requireNonNull(message, "message cannot be null");
        synchronized (lock) {
            if (state != State.PENDING) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping buffered message for rejected pending call {0}", callId());
                return false;
            }
            buffered.add(message);
            if (Log.TRACE) LOGGER.log(Level.TRACE, "buffered message for pending call {0}, {1} queued", callId(), buffered.size());
            return true;
        }
    }

    /**
     * Removes and returns every buffered message in arrival order for replay against the call context.
     *
     * <p>The caller replays the returned messages through the normal dispatch path once the call context
     * exists; the pending call's queue is emptied so a subsequent drain returns an empty list. Draining
     * does not change the {@link State}; the join path drives the promotion to a full call context
     * separately. The returned list is a fresh copy the caller owns.
     *
     * @return the buffered messages in arrival order, or an empty list when none are buffered
     */
    public List<CallMessage> drain() {
        synchronized (lock) {
            var drained = List.copyOf(buffered);
            buffered.clear();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "drained {0} buffered message(s) for pending call {1}", drained.size(), callId());
            return drained;
        }
    }

    /**
     * Returns the number of messages currently buffered.
     *
     * @return the count of buffered messages
     */
    public int bufferedCount() {
        synchronized (lock) {
            return buffered.size();
        }
    }

    /**
     * Moves the pending call to its declined terminal state and discards its buffered messages.
     *
     * <p>Sets the state to {@link State#REJECTED} and clears the buffered message queue so nothing is
     * replayed; idempotent, so declining an already declined pending call has no effect. The caller emits
     * the decline signaling separately. Returns whether this call performed the transition so the caller
     * can avoid emitting a duplicate decline.
     *
     * @return {@code true} if this call moved the pending call from {@link State#PENDING} to
     *         {@link State#REJECTED}, {@code false} if it was already rejected
     */
    public boolean reject() {
        synchronized (lock) {
            if (state == State.REJECTED) {
                return false;
            }
            state = State.REJECTED;
            buffered.clear();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "rejected pending call {0}", callId());
            return true;
        }
    }

    /**
     * Projects the opening offer onto the user facing incoming call model the host surfaces.
     *
     * <p>Maps the offer's call identifier, caller, group membership, group JID, and video flag onto an
     * {@link IncomingCall}. The chat the offer arrived in is the group JID for a group call and the
     * caller's phone number JID otherwise; when the offer carries no caller phone number JID the call
     * creator's JID is used as the peer and chat. The supplied timestamp is the moment the offer was
     * received, and the {@code offlineOffer} flag reflects whether the offer arrived from the offline
     * delivery path.
     *
     * @param receivedAt   the moment the offer was received; must not be {@code null}
     * @param offlineOffer whether the offer arrived from the offline delivery path
     * @return the projected incoming call model, never {@code null}
     * @throws NullPointerException if {@code receivedAt} is {@code null}
     */
    public IncomingCall toIncomingCall(Instant receivedAt, boolean offlineOffer) {
        Objects.requireNonNull(receivedAt, "receivedAt cannot be null");
        var peer = offer.callerPnValue().orElse(offer.callCreator().orElseThrow());
        var groupJid = offer.groupJidValue().orElse(null);
        var chatJid = resolveChatJid(peer, groupJid);
        return new IncomingCall(offer.callId().orElseThrow(), peer, chatJid, receivedAt, offer.isVideo(),
                offer.isGroup(), groupJid, offlineOffer);
    }

    /**
     * Resolves the chat JID an offer arrived in.
     *
     * <p>Returns the group JID for a group call so the host attributes the offer to the group chat, and
     * the peer JID for a one to one call.
     *
     * @param peer     the caller JID
     * @param groupJid the group JID, or {@code null} for a one to one call
     * @return the chat JID the offer is attributed to
     */
    private Jid resolveChatJid(Jid peer, Jid groupJid) {
        return groupJid != null ? groupJid : peer;
    }
}
