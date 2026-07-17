package com.github.auties00.cobalt.net;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.vigil.ConnectivityMonitor;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.random.RandomGenerator;

/**
 * Drives reconnection on a dedicated virtual thread, retrying with capped
 * exponential backoff until the socket is re-established or the session ends.
 *
 * <p>This exists so the reconnect loop runs off the reader thread that detected
 * the drop: the client hands off by calling {@link #requestReconnect()} from its
 * disconnect path and returns immediately, never holding its disconnect guard
 * across a multi-minute outage. A single long-lived supervisor thread waits for
 * a reconnect request, then loops {@link ConnectivityMonitor#awaitOnline()
 * await-online} plus one {@link ConnectAttempt} until {@code connected} reports
 * success, sleeping a {@link ReconnectBackoff jittered backoff} between failures.
 *
 * <p>The loop retries indefinitely on transport failures: only a terminal
 * verdict (the {@code terminated} supplier flipping true, set by logout, ban,
 * conflict, or an explicit disconnect) or {@link #cancel()} stops it. Because it
 * retries until {@code connected} is observed true rather than until a single
 * attempt returns, a drop that races in just after a successful attempt is not
 * lost: the disconnect path re-arms the request and the loop simply runs again.
 *
 * @implNote This is Cobalt's analogue of WA Web's {@code MainSocketLoop} /
 * {@code WAWebOpenSocket} retry loop (which uses {@code WAPromiseRetryLoop});
 * the connectivity gating mirrors {@code WAWebNetworkStatus.waitIfOffline}.
 * Connectivity returning interrupts an in-progress backoff sleep through
 * {@link #onConnectivityRegained()} so the next attempt fires at once.
 */
@WhatsAppWebModule(moduleName = "WAWebOpenSocket")
public final class ReconnectSupervisor {
    /**
     * The logger for {@link ReconnectSupervisor}.
     */
    private static final System.Logger LOGGER = Log.get(ReconnectSupervisor.class);

    /**
     * The connection attempt invoked once per iteration.
     */
    private final ConnectAttempt attempt;

    /**
     * Reports whether the socket is currently established; the loop stops
     * attempting once this is true.
     */
    private final BooleanSupplier connected;

    /**
     * Reports whether the session has terminally ended; when true the loop
     * exits and never retries.
     */
    private final BooleanSupplier terminated;

    /**
     * Connectivity monitor that gates attempts while the host is offline and
     * accelerates them when the network returns.
     */
    private final ConnectivityMonitor monitor;

    /**
     * Backoff schedule between failed attempts; touched only on the supervisor
     * thread or by {@link #onConnectivityRegained()}.
     */
    private final ReconnectBackoff backoff;

    /**
     * Monitor guarding {@link #reconnectRequested} and backing the idle wait.
     */
    private final Object lock = new Object();

    /**
     * Set by {@link #requestReconnect()} to signal the supervisor that a
     * reconnect is needed; cleared when the loop picks it up.
     */
    private boolean reconnectRequested;

    /**
     * Set by {@link #cancel()} to stop the loop permanently.
     */
    private volatile boolean cancelled;

    /**
     * Ensures the supervisor thread is started at most once.
     */
    private volatile boolean started;

    /**
     * The supervisor thread, retained so connectivity and cancellation can
     * interrupt a backoff sleep.
     */
    private volatile Thread thread;

    /**
     * Constructs a supervisor over the given seams.
     *
     * @param attempt    the per-iteration connection attempt; must not be
     *                   {@code null}
     * @param connected  reports whether the socket is established; must not be
     *                   {@code null}
     * @param terminated reports whether the session has terminally ended; must
     *                   not be {@code null}
     * @param monitor    the connectivity monitor gating attempts; must not be
     *                   {@code null}
     * @param random     the jitter source for the backoff; must not be
     *                   {@code null}
     */
    public ReconnectSupervisor(ConnectAttempt attempt, BooleanSupplier connected, BooleanSupplier terminated, ConnectivityMonitor monitor, RandomGenerator random) {
        this.attempt = Objects.requireNonNull(attempt, "attempt cannot be null");
        this.connected = Objects.requireNonNull(connected, "connected cannot be null");
        this.terminated = Objects.requireNonNull(terminated, "terminated cannot be null");
        this.monitor = Objects.requireNonNull(monitor, "monitor cannot be null");
        this.backoff = new ReconnectBackoff(Objects.requireNonNull(random, "random cannot be null"));
    }

    /**
     * Requests a reconnect, starting the supervisor thread on first use.
     *
     * <p>Idempotent and coalescing: repeated calls (the reader-loop close
     * callback and the error-handler reconnect verdict both fire) collapse into
     * a single in-flight reconnect cycle. Does nothing once cancelled or
     * terminated.
     */
    public void requestReconnect() {
        if (cancelled || terminated.getAsBoolean()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reconnect request ignored, cancelled={0} terminated={1}",
                    cancelled, terminated.getAsBoolean());
            return;
        }
        synchronized (lock) {
            reconnectRequested = true;
            lock.notifyAll();
        }
        if (!started) {
            synchronized (lock) {
                if (!started) {
                    started = true;
                    thread = Thread.ofVirtual()
                            .name("cobalt-reconnect-supervisor")
                            .start(this::loop);
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reconnect supervisor thread started");
                }
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reconnect requested");
    }

    /**
     * Signals that connectivity has returned, resetting the backoff and waking
     * the supervisor so the next attempt fires immediately.
     *
     * <p>Registered with the {@link ConnectivityMonitor} as a listener that
     * fires when connectivity returns.
     */
    public void onConnectivityRegained() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "connectivity regained, resetting backoff");
        backoff.reset();
        var current = thread;
        if (current != null) {
            current.interrupt();
        }
    }

    /**
     * Permanently stops the supervisor, waking it from any wait, backoff sleep,
     * or await-online so its thread can exit.
     *
     * <p>For full shutdown of the supervisor. Routine terminal disconnects do
     * not call this: they flip the {@code terminated} supplier instead, which
     * idles the loop while keeping its thread alive for a later reconnect.
     */
    public void cancel() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reconnect supervisor cancelled");
        cancelled = true;
        synchronized (lock) {
            lock.notifyAll();
        }
        var current = thread;
        if (current != null) {
            current.interrupt();
        }
    }

    /**
     * The supervisor thread body: waits for a reconnect request and runs the
     * retry loop, parking again afterwards.
     *
     * <p>The thread lives until {@link #cancel()}; a terminal session only
     * idles it (a request that arrives while terminated is skipped), so a later
     * reconnect after a fresh connect resumes on this same thread.
     */
    private void loop() {
        while (!cancelled) {
            if (!awaitReconnectRequest()) {
                return;
            }
            if (cancelled || terminated.getAsBoolean()) {
                continue;
            }
            backoff.reset();
            reconnectUntilConnected();
        }
    }

    /**
     * Blocks until a reconnect is requested or the supervisor is cancelled.
     *
     * @return {@code true} if a request was consumed, {@code false} if the
     *         supervisor was cancelled while waiting
     */
    private boolean awaitReconnectRequest() {
        synchronized (lock) {
            while (!reconnectRequested && !cancelled) {
                try {
                    lock.wait();
                } catch (InterruptedException _) {
                    // Woken by cancel() or a stray interrupt; re-check the loop conditions
                }
            }
            if (cancelled) {
                return false;
            }
            reconnectRequested = false;
            return true;
        }
    }

    /**
     * Attempts to reconnect, parking while offline and backing off between
     * failures, until the socket is connected or the session ends.
     */
    private void reconnectUntilConnected() {
        while (!cancelled && !terminated.getAsBoolean() && !connected.getAsBoolean()) {
            try {
                monitor.awaitOnline();
            } catch (InterruptedException _) {
                if (cancelled || terminated.getAsBoolean()) {
                    return;
                }
                continue;
            }
            if (cancelled || terminated.getAsBoolean()) {
                return;
            }
            try {
                attempt.run();
                backoff.reset();
                if (Log.INFO) LOGGER.log(Level.INFO, "reconnect attempt succeeded");
                return;
            } catch (IOException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "reconnect attempt failed", e);
                if (cancelled || terminated.getAsBoolean()) {
                    return;
                }
                sleepBackoff();
            }
        }
    }

    /**
     * Sleeps the next jittered backoff, returning early if interrupted by
     * connectivity returning or by cancellation.
     */
    private void sleepBackoff() {
        try {
            var delayMillis = backoff.nextDelayMillis();
            if (Log.TRACE) LOGGER.log(Level.TRACE, "backing off {0}ms before next reconnect attempt", delayMillis);
            Thread.sleep(delayMillis);
        } catch (InterruptedException _) {
            // Interrupted by onConnectivityRegained (backoff already reset) or cancel(); the
            // enclosing loop re-checks its conditions and either retries now or exits
        }
    }
}
