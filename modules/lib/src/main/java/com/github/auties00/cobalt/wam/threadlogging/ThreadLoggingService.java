package com.github.auties00.cobalt.wam.threadlogging;

import com.github.auties00.cobalt.wire.core.jid.JidProvider;

/**
 * Aggregates per-thread interaction activity and uploads it once per day as the family of
 * {@code ThreadInteractionData} WAM events.
 *
 * <p>WhatsApp Web's {@code WAWebChatThreadLogging} maintains an incremental counter row per thread per
 * day: producer hooks report each message, reaction, forward, edit, view-once interaction, call, and
 * commerce exchange, and the matching counter is bumped. Once a day has fully elapsed, the row is
 * enriched with live chat metadata, serialized into the {@code ThreadInteractionData} CoreConsumer,
 * Voip, Biz, Integrity, Ai, and Notification events, committed, and deleted. The per-thread identity
 * reported on the wire is an HMAC of the thread JID under a secret provisioned from the companion phone;
 * when the secret or the day-bucket offset is absent, nothing is bucketed or uploaded. The production
 * implementation is {@link LiveThreadLoggingService}.
 *
 * @see LiveThreadLoggingService
 * @see ThreadLoggingActivity
 * @see ThreadLoggingCounters
 */
public interface ThreadLoggingService {
    /**
     * Arms the recurring thread-logging upload task.
     *
     * @implSpec This is idempotent: a call while the task is already running is a no-op. The
     * implementation periodically invokes {@link #uploadEvents()}; the per-day cadence is enforced by
     * the upload watermark, so a wake that finds no fully-elapsed day buckets emits nothing.
     */
    void start();

    /**
     * Stops the recurring thread-logging upload task.
     *
     * @implSpec After this returns the task performs no further uploads until {@link #start()} re-arms
     * it; aggregation through {@link #recordActivity(JidProvider, ThreadLoggingActivity)} is unaffected.
     */
    void stop();

    /**
     * Records one thread-interaction activity against the day bucket for the given thread.
     *
     * <p>The activity is classified and the matching counters on the thread's current day-bucket row are
     * bumped. Activity is dropped when no day-bucket offset has been provisioned, or when the thread's
     * day bucket has already been uploaded.
     *
     * @implSpec This may be called concurrently from many threads; the implementation serializes the
     * get-or-create-and-bump of the counter row.
     *
     * @param chat     the thread the activity occurred in
     * @param activity the activity to record
     */
    void recordActivity(JidProvider chat, ThreadLoggingActivity activity);

    /**
     * Uploads every counter row whose day bucket has fully elapsed and advances the upload watermark.
     *
     * <p>This is bound to the recurring task and is also exposed so the client can drive an opportunistic
     * pass on reconnect. It is a no-op when the provisioned secret or day-bucket offset is absent.
     *
     * @implSpec The implementation removes the uploaded rows from the pending map and advances the
     * single global watermark, so each elapsed bucket is uploaded at most once.
     */
    void uploadEvents();
}
