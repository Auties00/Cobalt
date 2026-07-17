package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Tracks the four states of the offline-stanza replay that runs after a
 * web companion socket connects or reconnects.
 *
 * @apiNote
 * Listeners that need to defer actions while the server is replaying
 * queued stanzas (chat-list re-sorts, collection flushes, immediate
 * device syncs) read this value through the client.
 *
 * @implNote
 * This implementation collapses WhatsApp Web's separate blocking and
 * non-blocking offline-resume managers into a single state machine and
 * reproduces the companion timing constants (timeouts, batch limits,
 * debounce windows) as {@code public static final} fields on this enum
 * so consumers have a single import point.
 *
 * @see LinkedWhatsAppClient
 */
@WhatsAppWebModule(moduleName = "WAWebOfflineResumeConst")
@WhatsAppWebExport(
        moduleName = "WAWebOfflineResumeConst",
        exports = "ResumeStatus",
        adaptation = WhatsAppAdaptation.DIRECT)
public enum LinkedWhatsAppClientOfflineResumeState {
    /**
     * The state of a freshly constructed client before any offline
     * preview has arrived.
     *
     * @apiNote
     * Initial value held by the client until the first post-connect
     * offline preview lands.
     */
    INIT,

    /**
     * The client is replaying the backlog queued on the server during
     * the cold start after a process restart.
     *
     * @apiNote
     * During this phase chat-sort listeners are disabled and collection
     * flushes are deferred so the in-flight replay completes
     * deterministically.
     */
    RESUME_ON_RESTART,

    /**
     * The client is replaying the backlog queued on the server after a
     * live socket reconnect while the runtime stayed up.
     *
     * @apiNote
     * Distinct from {@link #RESUME_ON_RESTART} because the surrounding
     * runtime is already initialised. On entry chat-sort listeners are
     * disabled and a pending device sync is scheduled to run after
     * {@link #OFFLINE_DEVICE_SYNC_DELAY}.
     */
    RESUME_WITH_OPEN_TAB,

    /**
     * The replay has drained and the client is operating in real time.
     *
     * @apiNote
     * Reached either when the server signals that the offline backlog is
     * complete or when the local stale-stream watchdog fires after
     * {@link #OFFLINE_STANZA_TIMEOUT_MS}. Pending device sync has run by
     * this point; deferred operations may proceed.
     */
    COMPLETE;

    /**
     * Upper bound, in milliseconds, on how long the offline-resume
     * manager waits for the next offline stanza before treating the
     * current stanza count as stale and refreshing the window from the
     * server.
     *
     * @implNote
     * This implementation arms a timeout that refreshes the offline
     * window from the server once the interval elapses without progress;
     * the timeout is re-armed after every batch.
     */
    @WhatsAppWebExport(
            moduleName = "WAWebOfflineResumeConst",
            exports = "OFFLINE_STANZA_COUNT_CHECK_TIMEOUT_MS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final long OFFLINE_STANZA_COUNT_CHECK_TIMEOUT_MS = 20_000L;

    /**
     * Soft cap, in stanzas, on how many offline messages the resume
     * manager processes before it logs a warning that the count exceeded
     * the announced offline window.
     *
     * @implNote
     * This implementation compares the per-resume counter against this
     * value and logs a warning when it is exceeded; the resume itself is
     * not aborted.
     */
    @WhatsAppWebExport(
            moduleName = "WAWebOfflineResumeConst",
            exports = "OFFLINE_STANZA_COUNT_LIMIT",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int OFFLINE_STANZA_COUNT_LIMIT = 100;

    /**
     * Delay, in milliseconds, applied before running the pending device
     * sync once the offline backlog has drained.
     *
     * @implNote
     * This implementation runs the pending device sync after this delay
     * on the transition into {@link #RESUME_WITH_OPEN_TAB} and on the
     * transition into {@link #COMPLETE}.
     */
    @WhatsAppWebExport(
            moduleName = "WAWebOfflineResumeConst",
            exports = "OFFLINE_DEVICE_SYNC_DELAY",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final long OFFLINE_DEVICE_SYNC_DELAY = 2_000L;

    /**
     * Stale-stream watchdog, in milliseconds, after which the
     * offline-resume manager forces the state to {@link #COMPLETE} and
     * logs a missed-offline-complete event.
     *
     * @implNote
     * This implementation slides the watchdog forward with each new
     * offline stanza so it only fires when the stream goes silent for a
     * full minute.
     */
    @WhatsAppWebExport(
            moduleName = "WAWebOfflineResumeConst",
            exports = "OFFLINE_STANZA_TIMEOUT_MS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final long OFFLINE_STANZA_TIMEOUT_MS = 60_000L;

    /**
     * Debounce window, in milliseconds, during which a fresh offline
     * preview is treated as an update to the same resume rather than a
     * new one.
     *
     * @implNote
     * This implementation merges the counts of a second preview that
     * arrives within the window into the running resume; outside the
     * window the second preview restarts the state machine.
     */
    @WhatsAppWebExport(
            moduleName = "WAWebOfflineResumeConst",
            exports = "OFFLINE_PREVIEW_PERIOD_MS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final long OFFLINE_PREVIEW_PERIOD_MS = 1_000L;

    /**
     * Minimum interval, in milliseconds, between offline-resume UI
     * progress updates pushed to the chat-list surface.
     *
     * @implNote
     * This implementation throttles progress updates to at most one per
     * second during a flood of stanzas.
     */
    @WhatsAppWebExport(
            moduleName = "WAWebOfflineResumeConst",
            exports = "UI_UPDATE_TIME_MS",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final long UI_UPDATE_TIME_MS = 1_000L;
}
