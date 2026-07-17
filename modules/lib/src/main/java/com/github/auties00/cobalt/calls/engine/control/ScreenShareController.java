package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.incall.ScreenShareStanza;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import com.github.auties00.cobalt.calls.engine.control.event.ScreenShareEvent;

/**
 * Drives the screen share control within a call: starting, stopping, and tracking a screen share stream.
 *
 * <p>This controller owns the local user's screen share lifecycle. {@link #start()} begins sharing and
 * {@link #stop()} ends it; each sends a {@code screen_share} action carrying the new state and the
 * negotiated protocol version and emits a {@link ScreenShareEvent} for the local user. {@link #fail()}
 * reports a failed start. The protocol version distinguishes the {@linkplain #VERSION_V2 V2} single stream
 * port swap path, where screen content replaces the camera stream on the existing video port, from the
 * {@linkplain #VERSION_V3 V3} dual stream path, where the screen rides a separate auxiliary media stream
 * alongside the camera.
 *
 * <p>Inbound peer reports are delivered through {@link #onPeerScreenShare(Jid, ScreenShareState, int)},
 * which emits a {@link ScreenShareEvent} for the reporting peer. The controller holds the local state
 * behind a {@link ReentrantLock} and is bound to one call's identity and its signaling and event seams at
 * construction; it owns no timers, so it needs no explicit shutdown.
 */
public final class ScreenShareController {
    /**
     * The logger for {@link ScreenShareController}.
     */
    private static final System.Logger LOGGER = Log.get(ScreenShareController.class);

    /**
     * The screen share protocol version for the single stream port swap path.
     *
     * <p>In V2 the screen content replaces the camera stream on the existing video port.
     */
    public static final int VERSION_V2 = 2;

    /**
     * The screen share protocol version for the dual stream auxiliary stream path.
     *
     * <p>In V3 the screen rides a separate auxiliary media stream alongside the camera.
     */
    public static final int VERSION_V3 = 3;

    /**
     * The call identity this controller stamps onto its screen share actions.
     */
    private final CallControlContext context;

    /**
     * The signaling egress screen share actions are sent through.
     */
    private final CallSignalingSender sender;

    /**
     * The event sink screen share events are emitted into.
     */
    private final CallEventSink events;

    /**
     * The negotiated screen share protocol version this controller advertises on its actions.
     */
    private final int version;

    /**
     * Guards the local screen share state.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * The local user's current screen share state.
     */
    private ScreenShareState state = ScreenShareState.STOPPED;

    /**
     * Constructs a screen share controller bound to a call, its seams, and the negotiated version.
     *
     * @param context the call identity to stamp onto screen share actions; never {@code null}
     * @param sender  the signaling egress to send screen share actions through; never {@code null}
     * @param events  the event sink to emit screen share events into; never {@code null}
     * @param version the negotiated screen share protocol version ({@link #VERSION_V2} or
     *                {@link #VERSION_V3})
     * @throws NullPointerException if {@code context}, {@code sender}, or {@code events} is {@code null}
     */
    public ScreenShareController(CallControlContext context, CallSignalingSender sender, CallEventSink events,
                                 int version) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
        this.version = version;
    }

    /**
     * Starts the local screen share stream, broadcasting the started state.
     *
     * <p>Transitions the local state to {@link ScreenShareState#STARTED}, sends a {@code screen_share}
     * action carrying it and the negotiated version, and emits a {@link ScreenShareEvent} for the local
     * user.
     */
    public void start() {
        transition(ScreenShareState.STARTED);
    }

    /**
     * Stops the local screen share stream, broadcasting the stopped state.
     *
     * <p>Transitions the local state to {@link ScreenShareState#STOPPED} and broadcasts it.
     */
    public void stop() {
        transition(ScreenShareState.STOPPED);
    }

    /**
     * Reports a failed local screen share start, broadcasting the failed state.
     *
     * <p>Transitions the local state to {@link ScreenShareState#FAILED} and broadcasts it.
     */
    public void fail() {
        transition(ScreenShareState.FAILED);
    }

    /**
     * Returns the negotiated screen share protocol version this controller advertises.
     *
     * @return the negotiated protocol version ({@link #VERSION_V2} or {@link #VERSION_V3})
     */
    public int version() {
        return version;
    }

    /**
     * Returns the local user's current screen share state.
     *
     * @return the current local screen share state; never {@code null}
     */
    public ScreenShareState state() {
        lock.lock();
        try {
            return state;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Handles an inbound peer screen share report, emitting the peer's change.
     *
     * <p>Emits a {@link ScreenShareEvent} for the reporting peer; it does not change the local state.
     *
     * @param peer    the device JID of the reporting peer; never {@code null}
     * @param state   the peer's reported screen share state; never {@code null}
     * @param version the peer's reported screen share protocol version
     * @throws NullPointerException if {@code peer} or {@code state} is {@code null}
     */
    public void onPeerScreenShare(Jid peer, ScreenShareState state, int version) {
        Objects.requireNonNull(peer, "peer cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "peer {0} screen share state {1}, version={2}", peer, state, version);
        }
        events.emit(new ScreenShareEvent(peer, state, version));
    }

    /**
     * Sets the local screen share state, broadcasts it, and emits the local change.
     *
     * <p>Records the new state under the lock, sends a {@code screen_share} action carrying it and the
     * negotiated version, and emits a {@link ScreenShareEvent} for the local user.
     *
     * @param next the new local screen share state
     */
    private void transition(ScreenShareState next) {
        ScreenShareState previous;
        lock.lock();
        try {
            previous = this.state;
            this.state = next;
        } finally {
            lock.unlock();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "screen share state {0} -> {1}", previous, next);
        sender.send(new ScreenShareStanza(context.callId(), context.callCreator(), next.code(), version));
        events.emit(new ScreenShareEvent(context.selfJid(), next, version));
    }
}
