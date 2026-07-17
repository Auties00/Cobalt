package com.github.auties00.cobalt.net;

import java.util.random.RandomGenerator;

/**
 * Computes the wait between successive reconnect attempts using truncated
 * exponential backoff with equal jitter.
 *
 * <p>The undelayed backoff for attempt {@code n} (zero-based) is
 * {@code min(CAP, BASE * FACTOR^n)}; the returned delay is the lower half of
 * that window plus a uniformly random point in the upper half, so it always
 * falls in {@code [capped/2, capped]}. Equal jitter keeps a guaranteed minimum
 * spacing (so attempts never degenerate into a hot loop) while still spreading
 * concurrent clients across the window to avoid a reconnection thundering herd.
 * The attempt counter advances on every call and is reset by {@link #reset()}
 * when a connection is regained or connectivity returns.
 *
 * <p>Instances are not thread-safe; the {@link ReconnectSupervisor} owns one and
 * only ever touches it from its own supervisor thread.
 *
 * @implNote The base (1s), factor (2.0), and cap (30s) follow common
 * exponential-backoff-with-jitter guidance (AWS architecture blog, gRPC
 * connection-backoff spec) rather than any WA Web constant; WA Web's
 * {@code WAWebOpenSocket}/{@code WAPromiseRetryLoop} drive an equivalent
 * capped retry loop. The {@link RandomGenerator} is injected so tests can pass a
 * fixed-seed generator and assert exact delays, the deterministic seam Cobalt
 * prefers over wall-clock or sleeper injection.
 */
final class ReconnectBackoff {
    /**
     * Base delay in milliseconds for the first attempt before jitter.
     */
    static final long BASE_MILLIS = 1_000L;

    /**
     * Multiplier applied per attempt to grow the undelayed backoff.
     */
    static final double FACTOR = 2.0;

    /**
     * Upper bound in milliseconds on the undelayed backoff, after which the
     * window stops growing.
     */
    static final long CAP_MILLIS = 30_000L;

    /**
     * Largest attempt exponent fed to {@link Math#pow(double, double)} so the
     * intermediate {@code BASE * FACTOR^n} can never overflow a {@code long}
     * before it is clamped to {@link #CAP_MILLIS}.
     */
    private static final int MAX_EXPONENT = 40;

    /**
     * Source of jitter randomness; injected for deterministic testing.
     */
    private final RandomGenerator random;

    /**
     * Zero-based count of delays produced since the last {@link #reset()}.
     */
    private int attempt;

    /**
     * Constructs a backoff that draws jitter from the given generator.
     *
     * @param random the randomness source used to place the delay within its
     *               jitter window; must not be {@code null}
     */
    ReconnectBackoff(RandomGenerator random) {
        this.random = random;
    }

    /**
     * Returns the delay before the next reconnect attempt and advances the
     * attempt counter.
     *
     * <p>The result is in {@code [capped/2, capped]} where
     * {@code capped = min(CAP_MILLIS, BASE_MILLIS * FACTOR^attempt)}, so it is
     * always strictly positive and never exceeds {@link #CAP_MILLIS}.
     *
     * @return the jittered delay in milliseconds
     */
    long nextDelayMillis() {
        var exponent = Math.min(attempt, MAX_EXPONENT);
        var undelayed = BASE_MILLIS * Math.pow(FACTOR, exponent);
        var capped = undelayed >= CAP_MILLIS ? CAP_MILLIS : (long) undelayed;
        attempt++;
        var half = capped / 2;
        return half + random.nextLong(capped - half + 1);
    }

    /**
     * Resets the attempt counter so the next {@link #nextDelayMillis()} starts
     * again from the base window.
     */
    void reset() {
        attempt = 0;
    }
}
