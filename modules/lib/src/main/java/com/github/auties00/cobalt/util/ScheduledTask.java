package com.github.auties00.cobalt.util;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A one-shot or periodic task scheduled on a virtual thread, created through one of the static
 * factories and stopped through {@link #cancel()}.
 *
 * <p>A Cobalt subsystem that needs a deferred callback (retry backoff, debounced flush, lifecycle
 * teardown) or a recurring tick (keepalive sweeps, periodic re-broadcasts, expiry checks) schedules
 * it through {@link #scheduleDelayed(Duration, Runnable)} or {@link #schedule(Duration, Runnable)}
 * rather than standing up its own {@link java.util.concurrent.ScheduledExecutorService} or
 * hand-rolling a {@code Thread.sleep} loop. Each factory call owns exactly one virtual thread and
 * returns the handle bound to it; {@link #cancel()} disarms the task by waking a pending delay so it
 * never fires, stopping a periodic loop after the current execution, and interrupting an in-flight
 * execution. The handle is the only way to stop scheduled work, so a caller that may later need to
 * cancel must retain it.
 *
 * <p>Recurring tasks run with fixed-delay semantics: the configured period is the gap between the end
 * of one execution and the start of the next, never an independent rate that could let a slow
 * execution overlap the next. At most one execution of a given scheduled task is ever in flight. A
 * runtime exception thrown by a recurring task is logged and the next tick still runs; a one-shot
 * task that throws is logged and simply does not run again.
 *
 * @implNote This implementation uses one virtual thread per scheduled task, blocked in
 * {@link Thread#sleep(Duration)} between executions, rather than a shared
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}. A parked virtual thread costs only a
 * small heap continuation and a single timed unpark on the JDK's internal scheduler, so at Cobalt's
 * scale (tens of live timers per client) the per-task cost is negligible while the model stays
 * consistent with the codebase's virtual-thread-and-direct-blocking convention: cancellation is a
 * plain {@link Thread#interrupt()}, there is no JVM-lifetime daemon thread to shut down, and no
 * {@link java.util.concurrent.CompletableFuture} is involved. The handle holds no timer state of its
 * own because the worker thread, parked in {@code sleep}, is the timer.
 */
public final class ScheduledTask {
    /**
     * The logger for {@link ScheduledTask}.
     */
    private static final System.Logger LOGGER = Log.get(ScheduledTask.class);

    /**
     * Whether {@link #cancel()} has been called; polled by the worker thread between its sleep and run
     * phases so a cancellation that races a just-elapsed delay still suppresses the run.
     */
    private volatile boolean cancelled;

    /**
     * The virtual thread that owns the scheduled work, retained so {@link #cancel()} can interrupt a
     * blocking sleep or a blocking execution.
     */
    private volatile Thread worker;

    /**
     * Constructs an unbound handle whose worker thread is attached immediately afterwards by
     * {@link #bind(Thread)}.
     *
     * <p>Private because handles are created only by the static factories.
     */
    private ScheduledTask() {
    }

    /**
     * Schedules {@code task} to run once on a fresh virtual thread after {@code delay} has elapsed.
     *
     * <p>The task runs on its own virtual thread, so the caller shares no thread pool and the task may
     * block freely. {@link #cancel()} on the returned handle wakes the pending delay so the task never
     * runs, or interrupts it if cancellation races the firing. A non-positive {@code delay} runs the
     * task as soon as the worker thread is scheduled.
     *
     * @param delay the amount of time to wait before running the task; resolved with nanosecond
     *              precision
     * @param task  the task to execute once
     * @return a handle that can cancel the pending task
     * @throws NullPointerException if {@code delay} or {@code task} is {@code null}
     */
    public static ScheduledTask scheduleDelayed(Duration delay, Runnable task) {
        Objects.requireNonNull(delay, "delay cannot be null");
        Objects.requireNonNull(task, "task cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "scheduling delayed task, delay={0}", delay);
        return start("cobalt-scheduler-delayed", handle -> runOnce(delay, task, handle));
    }

    /**
     * Schedules {@code task} to run repeatedly on a fresh virtual thread, the first execution after
     * {@code period} and each subsequent execution {@code period} after the previous one finishes.
     *
     * <p>Equivalent to {@link #schedule(Duration, Duration, Runnable)} with the same value for the
     * initial delay and the period. The recurrence runs until {@link #cancel()} is called on the
     * returned handle.
     *
     * @param period the fixed delay between the end of one execution and the start of the next, and
     *               the delay before the first execution; must be positive
     * @param task   the task to execute on every tick
     * @return a handle that can cancel the recurring task
     * @throws NullPointerException     if {@code period} or {@code task} is {@code null}
     * @throws IllegalArgumentException if {@code period} is zero or negative
     */
    public static ScheduledTask schedule(Duration period, Runnable task) {
        return schedule(period, period, task);
    }

    /**
     * Schedules {@code task} to run repeatedly on a fresh virtual thread, the first execution after
     * {@code initialDelay} and each subsequent execution {@code period} after the previous one
     * finishes.
     *
     * <p>The task runs with fixed-delay semantics on a single virtual thread, so at most one execution
     * is ever in flight and a slow execution simply pushes the next tick back rather than overlapping
     * it. A runtime exception thrown by the task is logged and the recurrence continues with the next
     * tick. The loop runs until {@link #cancel()} is called on the returned handle, which wakes a
     * pending delay or interrupts an in-flight execution. A non-positive {@code initialDelay} runs the
     * first execution as soon as the worker thread is scheduled.
     *
     * @param initialDelay the delay before the first execution; resolved with nanosecond precision
     * @param period       the fixed delay between the end of one execution and the start of the next;
     *                     must be positive
     * @param task         the task to execute on every tick
     * @return a handle that can cancel the recurring task
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code period} is zero or negative
     */
    public static ScheduledTask schedule(Duration initialDelay, Duration period, Runnable task) {
        Objects.requireNonNull(initialDelay, "initialDelay cannot be null");
        Objects.requireNonNull(period, "period cannot be null");
        Objects.requireNonNull(task, "task cannot be null");
        if (period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("period must be positive");
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "scheduling periodic task, initialDelay={0} period={1}", initialDelay, period);
        return start("cobalt-scheduler-periodic", handle -> runPeriodic(initialDelay, period, task, handle));
    }

    /**
     * Cancels the scheduled work: no further executions run and any pending delay or in-flight
     * execution is unblocked.
     *
     * <p>Idempotent and safe to call from any thread, including from inside the scheduled task itself.
     * A task still waiting out its delay is woken and never runs; a periodic task is stopped after at
     * most its current execution; an execution blocked on interruptible work on another thread returns
     * through an {@link InterruptedException}. A non-interruptible execution already in progress runs to
     * completion, after which no further executions occur.
     *
     * @implNote This implementation interrupts the worker only when the caller is a different thread.
     * A task that cancels its own handle is not self-interrupted, because the run-and-sleep loop already
     * observes the cancelled flag and exits without sleeping again; self-interrupting would instead leave
     * a stray interrupt that could disrupt the rest of the running task.
     */
    public void cancel() {
        cancelled = true;
        var current = worker;
        if (current != null && current != Thread.currentThread()) {
            current.interrupt();
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "scheduled task cancelled");
    }

    /**
     * Returns whether this handle has been cancelled.
     *
     * @return {@code true} once {@link #cancel()} has been called
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Creates a handle, binds it to a fresh virtual thread running the given body, and starts it.
     *
     * @param threadName the name given to the worker thread for diagnosis
     * @param body       the worker body, receiving the handle so it can poll for cancellation
     * @return the started handle
     */
    private static ScheduledTask start(String threadName, Consumer<ScheduledTask> body) {
        var handle = new ScheduledTask();
        var worker = Thread.ofVirtual()
                .name(threadName)
                .unstarted(() -> body.accept(handle));
        handle.worker = worker;
        worker.start();
        return handle;
    }

    /**
     * Returns whether the scheduled work owned by this handle is still active.
     *
     * @return {@code true} until {@link #cancel()} has been called
     */
    private boolean isActive() {
        return !cancelled;
    }

    /**
     * Runs a one-shot task after its delay unless cancellation intervenes first.
     *
     * @param delay  the delay to wait before running
     * @param task   the task to run
     * @param handle the handle observed for cancellation
     */
    private static void runOnce(Duration delay, Runnable task, ScheduledTask handle) {
        if (!sleep(delay) || !handle.isActive()) {
            return;
        }
        runGuarded(task);
    }

    /**
     * Runs a periodic task with fixed-delay semantics until the handle is cancelled.
     *
     * <p>Sleeps the initial delay, then alternates run-and-sleep, polling the handle between phases so
     * a cancellation that races a just-elapsed sleep still stops the loop before the next execution.
     *
     * @param initialDelay the delay before the first execution
     * @param period       the fixed delay between executions
     * @param task         the task to run on every tick
     * @param handle       the handle observed for cancellation
     */
    private static void runPeriodic(Duration initialDelay, Duration period, Runnable task, ScheduledTask handle) {
        if (!sleep(initialDelay)) {
            return;
        }
        while (handle.isActive()) {
            runGuarded(task);
            if (!handle.isActive() || !sleep(period)) {
                return;
            }
        }
    }

    /**
     * Sleeps for the given duration, reporting whether it elapsed without interruption.
     *
     * <p>A zero or negative duration returns immediately as elapsed so a caller can schedule an
     * immediate first execution. An interrupt, raised by {@link #cancel()}, returns {@code false} so
     * the worker stops.
     *
     * @param duration the amount of time to sleep
     * @return {@code true} if the duration elapsed, {@code false} if the sleep was interrupted
     */
    private static boolean sleep(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return true;
        }
        try {
            Thread.sleep(duration);
            return true;
        } catch (InterruptedException _) {
            return false;
        }
    }

    /**
     * Runs a task, logging any runtime exception so a recurring loop survives it and a one-shot
     * failure is not lost silently.
     *
     * @param task the task to run
     */
    private static void runGuarded(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "scheduled task threw an exception", exception);
        }
    }
}
