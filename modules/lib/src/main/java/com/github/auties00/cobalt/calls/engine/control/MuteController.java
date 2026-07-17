package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.incall.MuteV2Stanza;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.util.ScheduledTask;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import com.github.auties00.cobalt.calls.engine.control.event.MuteByAnotherParticipant;
import com.github.auties00.cobalt.calls.engine.control.event.MuteRequestFailed;
import com.github.auties00.cobalt.calls.engine.control.event.MuteStateChanged;

/**
 * Drives the in call mute control: the local user's own mute toggle and the group admin request that
 * another participant mute itself.
 *
 * <p>This controller owns two mute operations and their timing rules. {@link #setMuted(boolean)} reports
 * the local user's own microphone state by sending a {@code mute_v2} self state action and emitting a
 * {@link MuteStateChanged}; a recent unmute starts a thirty second lockout during which a request from
 * another participant to mute the local user is ignored, so a peer cannot immediately mute again a user who
 * just chose to unmute. {@link #requestPeerMute(Jid)} sends a {@code mute_v2} peer mute request to a
 * target participant once and emits a {@link MuteRequestFailed} when the
 * {@linkplain #PEER_MUTE_REQUEST_DEADLINE five second request deadline} elapses without the target
 * being observed muted or the operation being cancelled. Inbound peer mute requests addressed to the
 * local user are delivered through {@link #onPeerMuteRequest(Jid)}, which emits a
 * {@link MuteByAnotherParticipant} unless the unmute lockout is active.
 *
 * <p>The controller is bound to one call's identity and its signaling and event seams at construction. Its
 * timers run on virtual thread {@link ScheduledTask} recurrences; the lockout deadline and the live peer
 * mute retry are guarded by a single lock so a timer callback never races an operation call. It is closed
 * with {@link #close()}, which cancels the live retry.
 *
 * <p>Only the {@code mute_v2} action (wire type {@code 26}) is emitted; the legacy {@code <mute>} element
 * (wire type {@code 12}) is never sent.
 *
 * @implNote This implementation replaces the native call timer heap with a virtual thread
 * {@link ScheduledTask} recurrence and the native info mutex with a {@link ReentrantLock}.
 */
public final class MuteController implements AutoCloseable {
    /**
     * The logger for {@link MuteController}.
     */
    private static final System.Logger LOGGER = Log.get(MuteController.class);

    /**
     * The window after a local unmute during which an inbound peer mute request is ignored.
     *
     * <p>Set to thirty seconds so a peer cannot immediately mute again a user who just chose to unmute.
     */
    private static final Duration UNMUTE_LOCKOUT = Duration.ofSeconds(30);

    /**
     * The cadence at which the peer mute retry timer ticks.
     *
     * <p>Set to five seconds so the first scheduled tick coincides with the
     * {@linkplain #PEER_MUTE_REQUEST_DEADLINE request deadline}.
     */
    private static final Duration PEER_MUTE_RETRY_INTERVAL = Duration.ofSeconds(5);

    /**
     * The deadline after which an unconfirmed peer mute request is given up and declared failed.
     *
     * <p>A single five second deadline is armed when the peer mute request is sent. A per call watchdog
     * compares that deadline against the current clock every second and, once it has elapsed, marks the
     * request timed out and emits a {@link MuteRequestFailed}. The request is therefore sent once and
     * given up after five seconds with no observation of the target being muted; the
     * {@linkplain #PEER_MUTE_RETRY_INTERVAL five second tick cadence} coincides with this deadline, so the
     * first scheduled tick reaches the deadline before any resend occurs, yielding a single send followed
     * by a five second timeout.
     */
    private static final Duration PEER_MUTE_REQUEST_DEADLINE = Duration.ofSeconds(5);

    /**
     * The call identity this controller stamps onto its mute actions.
     */
    private final CallControlContext context;

    /**
     * The signaling egress mute actions are sent through.
     */
    private final CallSignalingSender sender;

    /**
     * The event sink mute events are emitted into.
     */
    private final CallEventSink events;

    /**
     * Guards the lockout deadline and the live peer mute retry state.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * The {@link System#nanoTime()} reading until which an inbound peer mute request is ignored, or
     * {@code 0} when no lockout is active.
     */
    private long unmuteLockoutUntilNanos;

    /**
     * The live peer mute retry, or {@code null} when no peer mute request is outstanding.
     */
    private PeerMuteRetry peerMuteRetry;

    /**
     * Whether the local user is currently muted, as last reported by {@link #setMuted(boolean)}.
     */
    private boolean muted;

    /**
     * Whether the controller has been closed.
     */
    private volatile boolean closed;

    /**
     * Constructs a mute controller bound to a call and its signaling and event seams.
     *
     * @param context the call identity to stamp onto mute actions; never {@code null}
     * @param sender  the signaling egress to send mute actions through; never {@code null}
     * @param events  the event sink to emit mute events into; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public MuteController(CallControlContext context, CallSignalingSender sender, CallEventSink events) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
    }

    /**
     * Reports the local user's own mute state, sending the self state action and emitting the change.
     *
     * <p>Sends a {@code mute_v2} self state action carrying the new mute flag, broadcast in a group call,
     * and emits a {@link MuteStateChanged} for the local user. Unmuting opens the
     * {@linkplain #UNMUTE_LOCKOUT thirty second} lockout during which an inbound peer mute request is
     * ignored; muting clears any active lockout. A call on a closed controller is ignored.
     *
     * @param muted {@code true} to report the local user muted, {@code false} to report unmuted
     */
    public void setMuted(boolean muted) {
        if (closed) {
            return;
        }
        lock.lock();
        try {
            this.muted = muted;
            if (muted) {
                unmuteLockoutUntilNanos = 0;
            } else {
                unmuteLockoutUntilNanos = System.nanoTime() + UNMUTE_LOCKOUT.toNanos();
            }
        } finally {
            lock.unlock();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "self mute state changed to {0}", muted);
        sender.send(MuteV2Stanza.ofSelfState(context.callId(), context.callCreator(), muted, context.group()));
        events.emit(new MuteStateChanged(context.selfJid(), muted, true));
    }

    /**
     * Returns whether the local user is currently muted as last reported.
     *
     * @return {@code true} when the local user is muted
     */
    public boolean isMuted() {
        lock.lock();
        try {
            return muted;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Requests that a target participant mute itself, declaring failure when the deadline elapses.
     *
     * <p>Sends a {@code mute_v2} peer mute request to the target once and arms the
     * {@linkplain #PEER_MUTE_REQUEST_DEADLINE five second deadline}; when it elapses from the send without
     * the target being observed muted (through {@link #onPeerMuted(Jid)}) or the request being cancelled,
     * the request is given up and a {@link MuteRequestFailed} is emitted. A new request to a different
     * target replaces any outstanding request. A call on a closed controller is ignored. This is a
     * group admin operation; it is the caller's responsibility to invoke it only when the local user may
     * mute others.
     *
     * @param target the device JID of the participant asked to mute; never {@code null}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public void requestPeerMute(Jid target) {
        Objects.requireNonNull(target, "target cannot be null");
        if (closed) {
            return;
        }
        lock.lock();
        try {
            cancelPeerMuteRetry();
            peerMuteRetry = new PeerMuteRetry(target);
            peerMuteRetry.handle = ScheduledTask.schedule(PEER_MUTE_RETRY_INTERVAL, () -> retryPeerMute(target));
        } finally {
            lock.unlock();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "requesting peer mute for {0}", target);
        sender.send(MuteV2Stanza.ofPeerRequest(context.callId(), context.callCreator(), context.group()));
    }

    /**
     * Cancels an outstanding peer mute request to the given target without emitting a failure.
     *
     * <p>Used when the request is satisfied or abandoned by the caller; a request to a different target,
     * or no outstanding request, leaves the controller unchanged. A call on a closed controller is
     * ignored.
     *
     * @param target the device JID whose pending request to cancel; never {@code null}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public void cancelPeerMuteRequest(Jid target) {
        Objects.requireNonNull(target, "target cannot be null");
        lock.lock();
        try {
            if (peerMuteRetry != null && peerMuteRetry.target.equals(target)) {
                cancelPeerMuteRetry();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "peer mute request cancelled for {0}", target);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Notifies the controller that a participant was observed muted, satisfying a pending request.
     *
     * <p>When the muted participant is the target of an outstanding peer mute request, the request is
     * cancelled successfully without emitting a failure. Other participants are ignored.
     *
     * @param participant the device JID observed muted; never {@code null}
     * @throws NullPointerException if {@code participant} is {@code null}
     */
    public void onPeerMuted(Jid participant) {
        Objects.requireNonNull(participant, "participant cannot be null");
        lock.lock();
        try {
            if (peerMuteRetry != null && peerMuteRetry.target.equals(participant)) {
                cancelPeerMuteRetry();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "peer mute request satisfied for {0}", participant);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Handles an inbound request from another participant that the local user mute itself.
     *
     * <p>Emits a {@link MuteByAnotherParticipant} so the host can act, unless a recent unmute lockout is
     * active, in which case the request is suppressed so a peer cannot immediately mute again a user who
     * just unmuted. Returns whether the request was surfaced. A call on a closed controller is ignored and
     * returns {@code false}.
     *
     * @param requester the device JID that asked the local user to mute; never {@code null}
     * @return {@code true} when the request was surfaced, {@code false} when suppressed by the lockout or
     *         the controller is closed
     * @throws NullPointerException if {@code requester} is {@code null}
     */
    public boolean onPeerMuteRequest(Jid requester) {
        Objects.requireNonNull(requester, "requester cannot be null");
        if (closed) {
            return false;
        }
        lock.lock();
        try {
            if (unmuteLockoutUntilNanos != 0 && System.nanoTime() < unmuteLockoutUntilNanos) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "peer mute request from {0} suppressed by unmute lockout", requester);
                }
                return false;
            }
        } finally {
            lock.unlock();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "peer mute request from {0} surfaced", requester);
        events.emit(new MuteByAnotherParticipant(requester));
        return true;
    }

    /**
     * Closes the controller, cancelling any outstanding peer mute retry.
     *
     * <p>Marks the controller closed so a late call or timer callback is a no op; idempotent.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        lock.lock();
        try {
            cancelPeerMuteRetry();
        } finally {
            lock.unlock();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mute controller closed");
    }

    /**
     * Checks the peer mute request deadline and emits a failure once it has elapsed.
     *
     * <p>Runs on the retry timer's virtual thread. When the live request's
     * {@linkplain PeerMuteRetry#deadlineNanos deadline} has passed the request is cancelled and a
     * {@link MuteRequestFailed} is emitted. The deadline coincides with the
     * {@linkplain #PEER_MUTE_RETRY_INTERVAL scheduling period}, so the first tick already finds the
     * deadline reached and fails without resending. A check whose target no longer matches the live
     * request, or that fires after close, is dropped.
     *
     * @param target the device JID this request was armed for
     */
    private void retryPeerMute(Jid target) {
        if (closed) {
            return;
        }
        lock.lock();
        try {
            if (peerMuteRetry == null || !peerMuteRetry.target.equals(target)) {
                return;
            }
            if (System.nanoTime() - peerMuteRetry.deadlineNanos < 0) {
                return;
            }
            cancelPeerMuteRetry();
        } finally {
            lock.unlock();
        }
        if (Log.WARNING) LOGGER.log(Level.WARNING, "peer mute request timed out for {0}", target);
        events.emit(new MuteRequestFailed(target));
    }

    /**
     * Cancels the live peer mute retry timer and clears its state.
     *
     * <p>Called under the lock; a {@code null} retry is ignored.
     */
    private void cancelPeerMuteRetry() {
        if (peerMuteRetry != null) {
            if (peerMuteRetry.handle != null) {
                peerMuteRetry.handle.cancel();
            }
            peerMuteRetry = null;
        }
    }

    /**
     * Returns the live peer mute target, if a request is outstanding.
     *
     * @return an {@link Optional} with the outstanding peer mute target, or empty when none is
     *         outstanding
     */
    public Optional<Jid> pendingPeerMuteTarget() {
        lock.lock();
        try {
            return peerMuteRetry == null ? Optional.empty() : Optional.of(peerMuteRetry.target);
        } finally {
            lock.unlock();
        }
    }

    /**
     * The mutable state of one outstanding peer mute request: the target, its retry handle, and the
     * deadline at which the request is given up.
     */
    private static final class PeerMuteRetry {
        /**
         * The device JID the peer mute request is addressed to.
         */
        private final Jid target;

        /**
         * The {@link System#nanoTime()} reading at which the request expires and is declared failed.
         *
         * <p>Computed once from the send as {@code now + }{@link MuteController#PEER_MUTE_REQUEST_DEADLINE},
         * the five second instant at which the unanswered request is declared failed.
         */
        private final long deadlineNanos;

        /**
         * The scheduled retry handle, or {@code null} before it is armed.
         */
        private ScheduledTask handle;

        /**
         * Constructs a retry record for a target, fixing its expiry deadline from the current clock.
         *
         * @param target the device JID the request is addressed to
         */
        private PeerMuteRetry(Jid target) {
            this.target = target;
            this.deadlineNanos = System.nanoTime() + PEER_MUTE_REQUEST_DEADLINE.toNanos();
        }
    }
}
