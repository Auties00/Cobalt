package com.github.auties00.cobalt.calls.engine.timer;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;

/**
 * Enumerates the eleven per call timers the voip engine arms over a call's lifetime.
 *
 * <p>Each call owns its own set of timers. This enum names them so the {@link LifecycleController} can
 * arm and cancel them by kind through {@link CallTimerScheduler} without depending on the timer unit's
 * internal scheduling. The controller arms the lifecycle relevant timers directly (the {@link #CALLER_LONELY}
 * timer when an outbound call starts ringing, the {@link #PERIODIC} 1000ms watchdog while a call is being
 * set up) and cancels them all when the call tears down. The {@link #CONNECTED_LONELY} timer is instead
 * armed by the state transition guard as a call enters {@link CallLifecycleState#CONNECTED_LONELY} rather
 * than by the controller.
 *
 * <p>The eleven kinds are: {@link #PERIODIC} (the 1000ms setup watchdog), {@link #CALLER_LONELY} (outbound
 * ring timeout), {@link #OHAI} (keepalive request guard), {@link #HEARTBEAT} (group call heartbeat),
 * {@link #LOBBY} (waiting room poll), {@link #CONNECTED_LONELY} (connected but alone timeout),
 * {@link #UPDATE_ENCRYPTION_KEY} (group SFrame key rotation), {@link #REACTION_CLEAR} (reaction display
 * window), {@link #VIDEO_UPGRADE} (video upgrade request timeout), {@link #E2EE_RESTORE} (encryption
 * restore), and {@link #APP_DATA_STREAM_TEST} (application data stream probe). Teardown cancels every one
 * of these entries through {@link CallTimerScheduler#cancelAll(String)}.
 *
 * @implNote This implementation schedules the timers on a virtual thread scheduler rather than a native
 * timer heap, preserving the same interval constants.
 */
public enum CallTimerKind {
    /**
     * Identifies the 1000ms self rescheduling watchdog that enforces the per peer and per call setup
     * deadlines.
     *
     * <p>The watchdog sweeps for unanswered group call offers past the per peer timeout, unanswered mute
     * requests, and the two call setup deadlines, then reschedules itself. The controller arms it while a
     * call is being set up.
     */
    PERIODIC,

    /**
     * Identifies the timer that ends an outbound one to one call whose peer never answers.
     *
     * <p>The controller arms this when an outbound call begins ringing; on expiry the call ends with a
     * lonely state timeout.
     */
    CALLER_LONELY,

    /**
     * Identifies the timer guarding an outstanding {@code ohai} keepalive request.
     */
    OHAI,

    /**
     * Identifies the group call heartbeat timer that periodically resends a heartbeat signal.
     */
    HEARTBEAT,

    /**
     * Identifies the waiting room lobby poll timer, scheduled in minutes while a pending call sits in the
     * lobby.
     */
    LOBBY,

    /**
     * Identifies the timer that ends a connected but alone call when no peer connects through all of its
     * intervals.
     *
     * <p>Armed by the state transition guard as a call enters {@link CallLifecycleState#CONNECTED_LONELY}
     * and cancelled as it leaves; the controller cancels it on teardown but does not arm it.
     */
    CONNECTED_LONELY,

    /**
     * Identifies the group call SFrame key rotation tick that periodically rotates the end to end key.
     */
    UPDATE_ENCRYPTION_KEY,

    /**
     * Identifies the timer that clears a delivered reaction's local state after its display window.
     */
    REACTION_CLEAR,

    /**
     * Identifies the timer that downgrades a video upgrade request the peer never accepts.
     */
    VIDEO_UPGRADE,

    /**
     * Identifies the timer that restores end to end encryption after a transient escalation.
     */
    E2EE_RESTORE,

    /**
     * Identifies the timer that probes the application data stream during setup.
     */
    APP_DATA_STREAM_TEST
}
