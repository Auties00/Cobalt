package com.github.auties00.cobalt.calls.engine.timer;

import com.github.auties00.cobalt.calls.config.CallsFeatureGate;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallManager;
import com.github.auties00.cobalt.calls.engine.GroupCallOutbound;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.event.CallLifecycleEventSink;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns one {@link CallTimers} driver per call, arms or cancels its timers by kind, and runs each
 * timer's callback body when it fires.
 *
 * <p>Each call gets its own virtual thread timer driver, created lazily on the first arm and stopped
 * when the call's timers are cancelled wholesale on teardown. A {@link CallTimerKind} maps one to one
 * onto a {@link CallTimers.Timer}. This scheduler holds no reference to the lifecycle controller: it is
 * a pure timer mechanism the controller drives, and every timer's fire action is supplied by the
 * controller as a {@link Runnable} at {@link #arm(String, CallTimerKind, Runnable) arm} time. The
 * {@link CallTimerKind#PERIODIC} watchdog rearms itself every second and runs the action each tick, the
 * {@link CallTimerKind#CALLER_LONELY} timeout runs the action once, and the
 * {@link CallTimerKind#CONNECTED_LONELY} timeout walks its interval sequence and runs the action after
 * the final interval. The remaining kinds are armed with their period but carry no action, because their
 * callbacks belong to the group call and in call control units that arm them.
 *
 * <p>A fired lonely action ends the call by calling back into the {@link LifecycleController} through the
 * {@link Runnable} the controller handed in; that action runs on the timer driver thread holding no call
 * lock, so the reentrant {@link LifecycleController#endCall(String, CallEndReason)} teardown (which
 * cancels this very driver) neither deadlocks nor joins itself, because {@link CallTimers#stop()} skips
 * joining the driver thread from within its own callback.
 */
public final class LiveCallTimerScheduler implements CallTimerScheduler {
    /**
     * The logger for {@link LiveCallTimerScheduler}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCallTimerScheduler.class);

    /**
     * The empty payload emitted with a timer driven lifecycle event.
     */
    // TODO: populate the timer driven lifecycle event payload once its byte layout is known
    private static final byte[] EMPTY_PAYLOAD = DataUtils.EMPTY_BYTE_ARRAY;

    /**
     * Holds the live timer driver for each call id, created on first arm and removed on cancel all.
     */
    private final ConcurrentHashMap<String, CallTimers> timers = new ConcurrentHashMap<>();

    /**
     * The call manager used to resolve a firing timer's call context for the watchdog sweep.
     */
    private final CallManager manager;

    /**
     * The event sink a fired lonely or watchdog timeout emits its lifecycle event onto.
     */
    private final CallLifecycleEventSink events;

    /**
     * The calls feature gate, read for the AB prop configured group call heartbeat cadence when
     * arming the {@link CallTimers.Timer#HEARTBEAT} timer.
     */
    private final CallsFeatureGate featureGate;

    /**
     * Constructs a timer scheduler over the call manager, event sink, and feature gate.
     *
     * <p>The scheduler holds no reference to the lifecycle controller: every timer's fire action is
     * supplied by the controller at {@link #arm(String, CallTimerKind, Runnable) arm} time, so the
     * scheduler is a pure mechanism the controller drives and a fired timeout reaches the controller only
     * through the {@link Runnable} the controller handed in.
     *
     * @param manager     the call manager used to resolve a firing timer's call context
     * @param events      the event sink a fired lonely timeout emits its cause event onto
     * @param featureGate the calls feature gate read for the group call heartbeat cadence
     * @throws NullPointerException if {@code manager}, {@code events}, or {@code featureGate} is
     *                              {@code null}
     */
    public LiveCallTimerScheduler(CallManager manager, CallLifecycleEventSink events,
                                  CallsFeatureGate featureGate) {
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
        this.featureGate = Objects.requireNonNull(featureGate, "featureGate cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation arms the matching {@link CallTimers.Timer} with a body that runs the
     * supplied {@code action}: {@link CallTimerKind#PERIODIC} is armed at the one second watchdog cadence
     * with the self rescheduling {@link #periodicWatchdog(String, CallTimers, Runnable)} sweep that runs
     * the action each tick, {@link CallTimerKind#CALLER_LONELY} at the short lonely state timeout running
     * the action once, and {@link CallTimerKind#CONNECTED_LONELY} at its direction picked first interval
     * (see {@link #firstLonelyIntervalMillis(String)}) with the interval walking
     * {@link #connectedLonelyTimeout(String, CallTimers, int, Runnable)} teardown that runs the action
     * after the final interval; {@link CallTimerKind#HEARTBEAT} rearms at the AB prop cadence and runs the
     * action each tick. The remaining kinds are armed with their period and an inert body, because their
     * callbacks are owned by the group call and in call control units that arm them.
     */
    @Override
    public void arm(String callId, CallTimerKind kind, Runnable action) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "arming timer {0} for call {1}", kind, callId);
        var driver = timers.computeIfAbsent(callId, CallTimers::new);
        switch (kind) {
            case PERIODIC ->
                    driver.arm(CallTimers.Timer.PERIODIC, CallTimers.Timer.PERIODIC.defaultPeriod(),
                            () -> periodicWatchdog(callId, driver, action));
            case CALLER_LONELY ->
                    driver.arm(CallTimers.Timer.CALLER_LONELY,
                            CallTimers.Timer.CALLER_LONELY.defaultPeriod(),
                            () -> callerLonelyTimeout(callId, action));
            case CONNECTED_LONELY -> armConnectedLonely(callId, driver, firstLonelyIntervalMillis(callId), action);
            case HEARTBEAT -> {
                var period = heartbeatPeriod();
                if (!period.isZero() && !period.isNegative()) {
                    driver.arm(CallTimers.Timer.HEARTBEAT, period, () -> heartbeatTick(callId, driver, action));
                }
            }
            default -> armUnownedTimer(driver, kind);
        }
    }

    /**
     * Runs the group call heartbeat action and reschedules itself for the next cadence tick.
     *
     * <p>The action (the controller's heartbeat send, itself doing nothing unless the call is an active
     * group call) runs and the timer rearms at the {@code heartbeat_interval_s} cadence as long as the
     * call's driver is still running, so the heartbeat stops once the call is torn down.
     *
     * @param callId the identifier of the call being heartbeated
     * @param driver the call's timer driver
     * @param action the controller supplied heartbeat action run each tick
     */
    private void heartbeatTick(String callId, CallTimers driver, Runnable action) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "heartbeat tick for call {0}", callId);
        action.run();
        var period = heartbeatPeriod();
        if (!period.isZero() && !period.isNegative()) {
            driver.armIfRunning(CallTimers.Timer.HEARTBEAT, period,
                    () -> heartbeatTick(callId, driver, action));
        }
    }

    /**
     * Returns the group call heartbeat cadence read from the feature gate.
     *
     * <p>The {@code heartbeat_interval_s} server setting is read per call, so a configuration change
     * is picked up the next time a call arms its heartbeat.
     *
     * @return the heartbeat period
     */
    private Duration heartbeatPeriod() {
        return Duration.ofSeconds(featureGate.heartbeatIntervalSeconds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel(String callId, CallTimerKind kind) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cancelling timer {0} for call {1}", kind, callId);
        var driver = timers.get(callId);
        if (driver != null) {
            driver.cancel(toTimer(kind));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelAll(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var driver = timers.remove(callId);
        if (driver != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cancelling all timers for call {0}", callId);
            driver.stop();
        }
    }

    /**
     * Arms the connected lonely timer at its first interval with the interval walking teardown.
     *
     * <p>The first interval fires {@link #connectedLonelyTimeout(String, CallTimers, int, Runnable)} with
     * the next index, which walks the remaining intervals of
     * {@link CallTimers#DEFAULT_CONNECTED_LONELY_INTERVALS_MS} and runs the teardown action after the
     * last. The controller arms this from the state transition that enters
     * {@link CallLifecycleState#CONNECTED_LONELY}, supplying the end call action.
     *
     * @param callId      the identifier of the call whose connected lonely timer is armed
     * @param driver      the call's timer driver
     * @param firstMillis the first interval, in milliseconds
     * @param action      the controller supplied teardown action run after the final interval
     */
    private void armConnectedLonely(String callId, CallTimers driver, long firstMillis, Runnable action) {
        driver.arm(CallTimers.Timer.CONNECTED_LONELY, Duration.ofMillis(firstMillis),
                () -> connectedLonelyTimeout(callId, driver, 1, action));
    }

    /**
     * Returns the first connected lonely interval for a call, in milliseconds.
     *
     * <p>The first interval is picked by direction from the call context's connected lonely
     * configuration when the manager still holds the context (the short interval for the caller, the
     * long interval for the callee); when the context is not resolvable it falls back to the first
     * entry of {@link CallTimers#DEFAULT_CONNECTED_LONELY_INTERVALS_MS}.
     *
     * @param callId the identifier of the call whose first interval is resolved
     * @return the first connected lonely interval in milliseconds
     */
    private long firstLonelyIntervalMillis(String callId) {
        return manager.getByCallId(callId)
                .map(context -> context.connectedLonelyConfig().intervalForDirection(context.direction()))
                .orElse(CallTimers.DEFAULT_CONNECTED_LONELY_INTERVALS_MS[0]);
    }

    /**
     * Runs the periodic watchdog sweep and reschedules itself for the next second.
     *
     * <p>The watchdog resolves the firing call's context and, for a group call, terminates any peer
     * whose offer has gone unanswered past the {@link CallTimers#UNANSWERED_GROUP_OFFER_TIMEOUT}
     * cutoff; it then rearms itself for the next second, matching the engine's self rescheduling
     * watchdog. The two one to one call setup deadlines the engine watchdog also enforces are enforced
     * on the host path elsewhere: the offer ack deadline by the synchronous offer ack wait in
     * {@link LifecycleController#startCall(com.github.auties00.cobalt.wire.core.jid.Jid,
     * com.github.auties00.cobalt.wire.core.jid.Jid, java.util.List, boolean, MediaStreams)}, and the no
     * answer deadline by the {@link CallTimerKind#CALLER_LONELY} timer, so the watchdog adds no
     * further one to one teardown here.
     *
     * @implNote This implementation runs the controller supplied sweep action each tick (the action
     * delegates to the call's {@link GroupCallOutbound} unit, which terminates a peer whose offer has
     * gone unanswered past {@link CallTimers#UNANSWERED_GROUP_OFFER_TIMEOUT}; a one to one call resolves
     * to no unit and is swept of nothing). A call the manager no longer holds is not rescheduled, so the
     * watchdog stops once the call is torn down.
     *
     * @param callId the identifier of the call whose watchdog fired
     * @param driver the call's timer driver the watchdog rearms itself on
     * @param action the controller supplied sweep action run each tick
     */
    private void periodicWatchdog(String callId, CallTimers driver, Runnable action) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "periodic watchdog sweep for call {0}", callId);
        var context = manager.getByCallId(callId).orElse(null);
        if (context == null) {
            return;
        }
        action.run();
        driver.armIfRunning(CallTimers.Timer.PERIODIC, CallTimers.Timer.PERIODIC.defaultPeriod(),
                () -> periodicWatchdog(callId, driver, action));
    }

    /**
     * Ends an unanswered outbound one to one call when the caller lonely timeout fires.
     *
     * <p>The caller lonely timer is armed when an outbound call starts ringing; its expiry means the
     * callee never answered within the lonely state window, so the call ends through the controller
     * supplied action with the {@link CallEventType#LONELY_STATE_TIMEOUT} cause event.
     *
     * @param callId the identifier of the call whose caller lonely timer fired
     * @param action the controller supplied end call action
     */
    private void callerLonelyTimeout(String callId, Runnable action) {
        failCall(callId, CallEventType.LONELY_STATE_TIMEOUT, action);
    }

    /**
     * Walks the connected lonely interval sequence, ending the call after the final interval.
     *
     * <p>The connected lonely timer walks {@link CallTimers#DEFAULT_CONNECTED_LONELY_INTERVALS_MS}: on
     * each interval before the last it rearms at the next interval so a connected but alone call
     * survives a transient peer absence, and on the final interval it ends the call with
     * {@link CallEndReason#TIMEOUT} and the {@link CallEventType#LONELY_STATE_TIMEOUT} event. The
     * {@code nextIndex} is the index of the interval to arm next, the host analogue of the engine's
     * current interval index.
     *
     * @param callId    the identifier of the call whose connected lonely timer fired
     * @param driver    the call's timer driver the timeout rearms itself on
     * @param nextIndex the index of the next interval to arm, or one past the last interval to end the
     *                  call
     * @param action    the controller supplied end call action run after the final interval
     */
    private void connectedLonelyTimeout(String callId, CallTimers driver, int nextIndex, Runnable action) {
        var intervals = CallTimers.DEFAULT_CONNECTED_LONELY_INTERVALS_MS;
        if (nextIndex >= intervals.length) {
            failCall(callId, CallEventType.LONELY_STATE_TIMEOUT, action);
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "connected lonely timer for call {0} advancing to interval index {1}", callId, nextIndex);
        driver.armIfRunning(CallTimers.Timer.CONNECTED_LONELY, Duration.ofMillis(intervals[nextIndex]),
                () -> connectedLonelyTimeout(callId, driver, nextIndex + 1, action));
    }

    /**
     * Emits the timer driven cause event and runs the controller supplied end call action.
     *
     * <p>Fires the cause event (the lonely timeout event) onto the shared event sink, then runs the
     * {@code action}, the controller's {@link LifecycleController#endCall(String, CallEndReason)} for the
     * fired call, which sends the terminate, cancels every per call timer, closes the media plane, drives
     * the state to {@link CallLifecycleState#NONE}, and emits the ending events. The callback runs on the
     * timer driver thread holding no call lock, so the reentrant teardown (which cancels this very driver)
     * is safe.
     *
     * @param callId     the identifier of the call to end
     * @param causeEvent the lifecycle event for the cause of the teardown
     * @param action     the controller supplied end call action
     */
    private void failCall(String callId, CallEventType causeEvent, Runnable action) {
        if (Log.WARNING) LOGGER.log(Level.WARNING, "call {0} timer timeout, event={1}", callId, causeEvent);
        events.emit(causeEvent, EMPTY_PAYLOAD);
        action.run();
    }

    /**
     * Arms a timer whose callback body is owned by another engine unit at its period.
     *
     * <p>The heartbeat, lobby, ohai, key rotation, reaction clear, video upgrade, end to end restore,
     * and app data stream test timers are armed by the group call and in call control units that own
     * their callbacks; this scheduler only holds the driver and the lifecycle relevant bodies, so it
     * arms these with their period (or the watchdog cadence for a timer whose negotiated delay is not
     * threaded here yet) and an inert body the owning unit replaces when it arms the timer with its own
     * callback.
     *
     * @param driver the call's timer driver
     * @param kind   the timer kind whose callback is owned by another unit
     */
    private void armUnownedTimer(CallTimers driver, CallTimerKind kind) {
        // UPDATE_ENCRYPTION_KEY reaches this seam with an inert body: the membership driven rekey runs
        // immediately in the lifecycle controller over the reconciled group roster diff, so no per
        // call rekey timer is armed. The remaining kinds (LOBBY, OHAI, REACTION_CLEAR, VIDEO_UPGRADE,
        // E2EE_RESTORE, APP_DATA_STREAM_TEST) reach this seam only when a caller drives one through it.
        var timer = toTimer(kind);
        var period = timer.defaultPeriod().isZero()
                ? CallTimers.Timer.PERIODIC.defaultPeriod()
                : timer.defaultPeriod();
        if (Log.TRACE) LOGGER.log(Level.TRACE, "arming unowned timer {0} with inert body, period {1}ms", kind, period.toMillis());
        driver.arm(timer, period, () -> {
        });
    }

    /**
     * Maps a controller timer kind onto its timer unit member.
     *
     * @param kind the controller facing timer kind
     * @return the matching {@link CallTimers.Timer}
     */
    private static CallTimers.Timer toTimer(CallTimerKind kind) {
        return switch (kind) {
            case PERIODIC -> CallTimers.Timer.PERIODIC;
            case CALLER_LONELY -> CallTimers.Timer.CALLER_LONELY;
            case OHAI -> CallTimers.Timer.OHAI;
            case HEARTBEAT -> CallTimers.Timer.HEARTBEAT;
            case LOBBY -> CallTimers.Timer.LOBBY;
            case CONNECTED_LONELY -> CallTimers.Timer.CONNECTED_LONELY;
            case UPDATE_ENCRYPTION_KEY -> CallTimers.Timer.UPDATE_ENCRYPTION_KEY;
            case REACTION_CLEAR -> CallTimers.Timer.REACTION_CLEAR;
            case VIDEO_UPGRADE -> CallTimers.Timer.VIDEO_UPGRADE;
            case E2EE_RESTORE -> CallTimers.Timer.E2EE_RESTORE;
            case APP_DATA_STREAM_TEST -> CallTimers.Timer.APP_DATA_STREAM_TEST;
        };
    }
}
