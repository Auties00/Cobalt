package com.github.auties00.cobalt.net;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Objects;

/**
 * Default {@link KeepAliveService} that pings the socket on a fixed cadence and,
 * when a ping fails, asks the client to reconnect.
 *
 * <p>A single virtual thread sleeps {@link #INTERVAL}, sends a ping bounded by
 * {@link #TIMEOUT}, and on any ping failure runs the supplied dead-link action
 * (which triggers a reconnecting disconnect) and exits; the next successful
 * connection starts a fresh loop. Because only one thread runs, at most one ping
 * is ever in flight, so no de-duplication of concurrent pings is needed.
 *
 * @implNote The cadence and timeout follow WA Web's keepalive: the timeout
 * mirrors the {@code web_offline_resume_wait_for_ping_timeout_seconds} A/B value
 * ({@code blockSendPing} treats a ping that exceeds it as offline), here a
 * hardcoded 10s because Cobalt does not yet model that property. Treating every
 * ping failure (timeout or closed stream) as a dead link matches
 * {@code blockSendPing} returning {@code false} on {@code TimeoutError}.
 */
@WhatsAppWebModule(moduleName = "WAWebCommsSendPing")
public final class LiveKeepAliveService implements KeepAliveService {
    /**
     * The logger for {@link LiveKeepAliveService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveKeepAliveService.class);

    /**
     * Delay between successive keepalive pings.
     */
    static final Duration INTERVAL = Duration.ofSeconds(30);

    /**
     * Maximum time to wait for a ping reply before treating the link as dead.
     */
    static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Sends a ping and blocks for its reply, throwing on failure.
     */
    private final KeepAlivePinger pinger;

    /**
     * Action run when a ping fails, triggering a reconnecting disconnect.
     */
    private final Runnable onDeadLink;

    /**
     * Delay between successive pings; {@link #INTERVAL} in production.
     */
    private final Duration interval;

    /**
     * Per-ping reply timeout; {@link #TIMEOUT} in production.
     */
    private final Duration timeout;

    /**
     * Guards {@link #running} and {@link #thread} and backs the interruptible
     * sleep.
     */
    private final Object lock = new Object();

    /**
     * Whether the loop is currently running.
     */
    private boolean running;

    /**
     * The keepalive thread, retained so {@link #stop()} can interrupt its sleep.
     */
    private Thread thread;

    /**
     * Constructs a keepalive service over the given seams with the production
     * {@link #INTERVAL} and {@link #TIMEOUT}.
     *
     * @param pinger     sends a ping and blocks for its reply; must not be
     *                   {@code null}
     * @param onDeadLink action run on a failed ping; must not be {@code null}
     */
    public LiveKeepAliveService(KeepAlivePinger pinger, Runnable onDeadLink) {
        this(pinger, onDeadLink, INTERVAL, TIMEOUT);
    }

    /**
     * Constructs a keepalive service with explicit timing, used by tests to
     * exercise the loop without the production cadence.
     *
     * @param pinger     sends a ping and blocks for its reply; must not be
     *                   {@code null}
     * @param onDeadLink action run on a failed ping; must not be {@code null}
     * @param interval   delay between pings; must not be {@code null}
     * @param timeout    per-ping reply timeout; must not be {@code null}
     */
    LiveKeepAliveService(KeepAlivePinger pinger, Runnable onDeadLink, Duration interval, Duration timeout) {
        this.pinger = Objects.requireNonNull(pinger, "pinger cannot be null");
        this.onDeadLink = Objects.requireNonNull(onDeadLink, "onDeadLink cannot be null");
        this.interval = Objects.requireNonNull(interval, "interval cannot be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        synchronized (lock) {
            if (running) {
                return;
            }
            running = true;
            thread = Thread.ofVirtual()
                    .name("cobalt-keepalive")
                    .start(this::loop);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "keepalive service started, interval={0}ms timeout={1}ms",
                interval.toMillis(), timeout.toMillis());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        Thread current;
        synchronized (lock) {
            if (!running) {
                return;
            }
            running = false;
            current = thread;
            thread = null;
            lock.notifyAll();
        }
        if (current != null) {
            current.interrupt();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "keepalive service stopped");
    }

    /**
     * The keepalive thread body: pings on each interval until stopped or a ping
     * fails.
     */
    private void loop() {
        while (isRunning()) {
            if (!sleepInterval()) {
                return;
            }
            if (!isRunning()) {
                return;
            }
            try {
                pinger.ping(timeout);
                if (Log.TRACE) LOGGER.log(Level.TRACE, "keepalive ping succeeded");
            } catch (RuntimeException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "keepalive ping failed, treating link as dead", e);
                onDeadLink.run();
                return;
            }
        }
    }

    /**
     * Returns whether the loop should keep running.
     *
     * @return {@code true} while started and not yet stopped
     */
    private boolean isRunning() {
        synchronized (lock) {
            return running;
        }
    }

    /**
     * Sleeps for one {@link #interval}, returning early if stopped.
     *
     * @return {@code true} if the interval elapsed and the loop should continue,
     *         {@code false} if it was interrupted by {@link #stop()}
     */
    private boolean sleepInterval() {
        try {
            Thread.sleep(interval.toMillis());
            return isRunning();
        } catch (InterruptedException _) {
            return false;
        }
    }
}
