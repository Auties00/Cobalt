package com.github.auties00.cobalt.wam;

/**
 * Emits the once-per-day WhatsApp Metrics {@code Daily} event (id 1158) and its accompanying
 * private-stats canary events on a recurring cadence.
 *
 * <p>WhatsApp Web logs a {@code Daily} snapshot of long-lived account state (locale, privacy
 * audiences, contact counts, decoder capabilities, username state) at most once every rolling
 * twenty-four hours, immediately followed by a fixed block of zero-field private-stats test events
 * that exercise the deidentified-telemetry rotation machinery. A Cobalt session that never emitted
 * {@code Daily} would be trivially distinguishable from a real Web client, so Cobalt mirrors that
 * heartbeat. The production implementation is {@link LiveDailyStatsService}.
 *
 * @see LiveDailyStatsService
 * @see com.github.auties00.cobalt.wire.wam.event.DailyEvent
 */
public interface DailyStatsService {
    /**
     * Arms the recurring daily-stats task.
     *
     * @implSpec This is idempotent: a call while the task is already running is a no-op. The
     * implementation evaluates a rolling-day gate and emits at most one {@code Daily} event, plus the
     * private-stats canary block, per rolling twenty-four hours, persisting the last run so the gate
     * survives process restarts.
     */
    void start();

    /**
     * Stops the recurring daily-stats task.
     *
     * @implSpec After this returns the task emits no further events until {@link #start()} re-arms it.
     */
    void stop();
}
