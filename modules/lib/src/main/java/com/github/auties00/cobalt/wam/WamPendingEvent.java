package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wam.model.WamEventSpec;

import java.util.concurrent.CompletableFuture;

/**
 * Carries a committed {@link WamEventSpec} sitting in {@link WamService}'s
 * per-channel pending list while waiting for the next flush tick.
 *
 * <p>This record is the internal handoff between {@code WamService.commit},
 * which timestamps the event, and the flush worker, which drains pending
 * entries into a buffer for upload. The {@code flushFuture} component is only
 * present when the caller used the {@code commitAndWaitForFlush} variant;
 * ordinary {@code commit} calls are fire-and-forget and produce a record with
 * a {@code null} future.
 *
 * @param event             the WAM event payload to encode and upload
 * @param commitTimeSeconds the Unix epoch second pinned at commit time and
 *                          written into global field {@code 47}
 *                          ({@code commitTime}) when the buffer is sealed
 * @param flushFuture       the future completed when the buffer containing this
 *                          event finishes uploading, or {@code null} for
 *                          fire-and-forget commits
 */
@WhatsAppWebModule(moduleName = "WAWebWam")
record WamPendingEvent(WamEventSpec event, long commitTimeSeconds, CompletableFuture<Void> flushFuture) {
    /**
     * Constructs a fire-and-forget pending event whose upload outcome the
     * caller does not await.
     *
     * <p>Used by {@code WamService.commit(WamEventSpec)} for the default
     * non-blocking commit path; the {@code commitAndWaitForFlush} path uses the
     * three-argument canonical constructor and threads its own future through.
     *
     * @param event             the WAM event payload
     * @param commitTimeSeconds the Unix epoch second at commit
     */
    WamPendingEvent(WamEventSpec event, long commitTimeSeconds) {
        this(event, commitTimeSeconds, null);
    }
}
