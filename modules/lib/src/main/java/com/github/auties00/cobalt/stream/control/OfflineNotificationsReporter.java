package com.github.auties00.cobalt.stream.control;

import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientOfflineResumeState;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.stream.notification.device.NotificationSyncStreamHandler;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MdAppStateOfflineNotificationsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.OfflineResumeStageEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.OfflineResumeModes;
import com.github.auties00.cobalt.wire.wam.type.OfflineResumeStages;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tracks the per-collection multiplicity of {@code server_sync} notifications received while the offline backlog is
 * still draining and emits both the {@code MdAppStateOfflineNotifications} redundancy event and the staged
 * {@code OfflineResumeStage} telemetry when the backlog window ends.
 *
 * <p>The reporter is shared between a producer and a consumer. Every time the server pushes a
 * {@code <notification type="server_sync">} stanza, the producer ({@link NotificationSyncStreamHandler}) calls
 * {@link #increment(SyncPatchType)} once per affected collection; when the offline-end bulletin arrives, the consumer
 * ({@link InfoBulletinStreamHandler}) calls {@link #report()}. The report first replays the offline-resume stage
 * timeline as a sequence of {@code OfflineResumeStage} WAM events (one per resume milestone), then flushes the
 * per-collection map into the {@code MdAppStateOfflineNotifications} event with a redundant count equal to the number
 * of duplicate notifications observed for the same collection. It is wired up once inside
 * {@link com.github.auties00.cobalt.stream.NodeStreamService}.
 *
 * @implNote This implementation collapses WA Web's producer/consumer pair into a single shared service so the two
 * distinct {@code SocketStreamHandler} implementations can observe the same map without exposing private state on the
 * {@link LinkedWhatsAppClient}; the map is cleared atomically on flush. WA Web's {@code OfflineResumeReporter} fires one
 * staged event per milestone as the milestone is reached; because Cobalt observes only the terminal offline-complete
 * bulletin from this service, the whole stage timeline is reconstructed at flush time as a coherent monotonic sequence.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleReportServerSyncNotification")
@WhatsAppWebModule(moduleName = "WAWebWamOfflineResumeReporter")
public final class OfflineNotificationsReporter {
    /**
     * The logger for {@link OfflineNotificationsReporter}.
     */
    private static final System.Logger LOGGER = Log.get(OfflineNotificationsReporter.class);

    /**
     * The number of milliseconds after the notional offline-resume start at which the {@code SOCKET_CONNECT} stage is
     * reported.
     *
     * @implNote This implementation anchors the reconstructed stage timeline near connect; the value approximates the
     * short gap between the resume start and the socket handshake completing on a warm client.
     */
    private static final long SOCKET_CONNECT_STAGE_MILLIS = 120L;

    /**
     * The number of milliseconds added to {@link #SOCKET_CONNECT_STAGE_MILLIS} to place the {@code OFFLINE_PREVIEW}
     * stage, representing the delay between socket connect and the server pushing the {@code <offline_preview/>}
     * bulletin.
     */
    private static final long OFFLINE_PREVIEW_STAGE_GAP_MILLIS = 280L;

    /**
     * The per-envelope wall-clock cost, in milliseconds, folded into the synthesized backlog drain duration.
     *
     * @implNote This implementation reuses the same {@code envelopeCount * 5} heuristic that the sibling offline-resume
     * emitter applies when no live drain interval is available.
     */
    private static final long PER_ENVELOPE_DRAIN_MILLIS = 5L;

    /**
     * The minimum synthesized backlog drain duration in milliseconds, applied so a small backlog still reports a
     * plausible non-trivial {@code PROCESS_COMPLETE} timestamp.
     */
    private static final long OFFLINE_MIN_DRAIN_MILLIS = 200L;

    /**
     * The rounding step applied to the live chat-thread count before it is reported, matching WA Web's coarsening of the
     * figure to the nearest ten.
     */
    private static final long CHAT_THREAD_ROUND_STEP = 10L;

    /**
     * The lower bound, in milliseconds, of the synthesized age of the push that woke the client, reported as the
     * {@code lastPushTimestampMs} on the {@code SOCKET_CONNECT} stage.
     *
     * @implNote This implementation fabricates a recent push completion because Cobalt keeps no push-complete
     * timestamp; WA Web reads the real value from {@code WAWebUserPrefsGeneral.getLastPushCompleteTimestamp}.
     */
    private static final long MIN_PUSH_RECENCY_MILLIS = 2_000L;

    /**
     * The upper bound, in milliseconds, of the synthesized age of the push that woke the client.
     */
    private static final long MAX_PUSH_RECENCY_MILLIS = 6_000L;

    /**
     * The {@link LinkedWhatsAppClient} used to read live store state (the chat-thread count and the offline-resume mode)
     * when synthesizing the staged {@code OfflineResumeStage} telemetry; the actual WAM emission is routed through
     * {@link #wamService}.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The {@link WamService} used to commit the {@code MdAppStateOfflineNotifications} event when {@link #report()}
     * runs.
     */
    private final WamService wamService;

    /**
     * The per-collection observation count for offline {@code server_sync} notifications received since the last flush.
     *
     * <p>Producers call {@link #increment(SyncPatchType)}; the consumer drains the map atomically when the offline
     * backlog window closes.
     */
    private final ConcurrentMap<SyncPatchType, Integer> offlineNotificationsCount;

    /**
     * Constructs a new reporter bound to the given client and WAM service.
     *
     * <p>The same instance is threaded through both the server-sync producer ({@link NotificationSyncStreamHandler})
     * and the offline-bulletin consumer ({@link InfoBulletinStreamHandler}).
     *
     * @param whatsapp   the {@link LinkedWhatsAppClient}; must not be {@code null}
     * @param wamService the {@link WamService} used to commit the offline-notifications event; must not be
     *                   {@code null}
     * @throws NullPointerException if {@code whatsapp} or {@code wamService} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleReportServerSyncNotification",
            exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public OfflineNotificationsReporter(LinkedWhatsAppClient whatsapp, WamService wamService) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.offlineNotificationsCount = new ConcurrentHashMap<>();
    }

    /**
     * Increments the observation count for the given {@link SyncPatchType} by one.
     *
     * <p>Called once per affected collection by {@link NotificationSyncStreamHandler} for every offline
     * {@code <notification type="server_sync">} stanza. The first notification for a given collection is informational;
     * subsequent notifications for the same collection inside the same offline window are redundant and inflate the WAM
     * event's redundant count.
     *
     * @implNote This implementation uses
     * {@link ConcurrentMap#merge(Object, Object, java.util.function.BiFunction)} with the
     * {@link Integer#sum(int, int)} combiner so the bump is lock-free against producers writing other keys and against
     * the consumer's atomic drain in {@link #report()}.
     *
     * @param collection the collection whose offline notification count should be bumped; must not be {@code null}
     * @throws NullPointerException if {@code collection} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleReportServerSyncNotification",
            exports = "offlineNotificationsCount", adaptation = WhatsAppAdaptation.ADAPTED)
    public void increment(SyncPatchType collection) {
        Objects.requireNonNull(collection, "collection cannot be null");
        offlineNotificationsCount.merge(collection, 1, Integer::sum);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "offline server_sync notification counted for {0}", collection);
        }
    }

    /**
     * Replays the offline-resume stage telemetry and flushes the accumulated offline-notification counts as a single
     * {@code MdAppStateOfflineNotifications} WAM event, then clears the map.
     *
     * <p>Called by {@link InfoBulletinStreamHandler} when the {@code <ib><offline/></ib>} bulletin announces that the
     * server has finished delivering the offline queue. The staged {@code OfflineResumeStage} events are always emitted
     * through {@link #emitOfflineResumeStages()} because the offline-complete bulletin unconditionally closes a resume
     * cycle. The {@code MdAppStateOfflineNotifications} event that follows carries the total number of redundant
     * notifications (the sum of {@code count - 1} over each tracked collection) as a wire-efficiency telemetry signal
     * and is skipped when no offline notifications were observed.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleReportServerSyncNotification",
            exports = "reportOfflineNotifications", adaptation = WhatsAppAdaptation.DIRECT)
    public void report() {
        emitOfflineResumeStages();

        if (offlineNotificationsCount.isEmpty()) {
            return;
        }

        var redundantCount = 0;
        for (var count : offlineNotificationsCount.values()) {
            redundantCount += count - 1;
        }

        wamService.commit(new MdAppStateOfflineNotificationsEventBuilder()
                .redundantCount(redundantCount)
                .build());

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "flushed offline notifications collectionCount={0} redundantCount={1}",
                    offlineNotificationsCount.size(), redundantCount);
        }

        offlineNotificationsCount.clear();
    }

    /**
     * Emits the staged {@code OfflineResumeStage} telemetry for the resume cycle that the just-arrived offline-complete
     * bulletin closes.
     *
     * <p>Three stages are committed in the order the client passes through them:
     * {@link OfflineResumeStages#SOCKET_CONNECT}, {@link OfflineResumeStages#OFFLINE_PREVIEW} and
     * {@link OfflineResumeStages#PROCESS_COMPLETE}, each carrying the shared resume-session snapshot plus its own
     * monotonically increasing {@code offlineStageTimestampMs}. The socket-connect stage additionally reports the age of
     * the push that woke the client. The observed server-sync notification total is read live from
     * {@link #offlineNotificationsCount} and the chat-thread count from {@link LinkedWhatsAppClient#store()}; the resume
     * mode is derived from the current {@link LinkedWhatsAppClientOfflineResumeState}.
     *
     * @implNote This implementation reconstructs the whole stage timeline at the single offline-complete point this
     * service observes, because Cobalt collapses WA Web's per-milestone reporter callbacks
     * ({@code logSocketConnect}/{@code logOfflinePreview}/{@code logProcessComplete}) into one flush. The
     * {@code SCREEN_LOAD} and {@code PAGE_LOAD} stages are omitted because a headless client has no render timeline. The
     * message, receipt and call counts are fabricated coherently from the real notification total (roughly three
     * messages and two receipts per observed notification, with jitter) because those categories are counted in
     * {@link InfoBulletinStreamHandler}, not in this service; {@code offlineDecryptErrorCount} and {@code mailboxAge}
     * report zero to match WA Web's clean-resume defaults (no decrypt failures, a freshly delivered mailbox).
     */
    @WhatsAppWebExport(moduleName = "WAWebWamOfflineResumeReporter", exports = "OfflineResumeReporter",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitOfflineResumeStages() {
        var notificationCount = 0L;
        for (var count : offlineNotificationsCount.values()) {
            notificationCount += count;
        }

        var random = ThreadLocalRandom.current();
        var messageCount = notificationCount > 0L
                ? notificationCount * 3L + random.nextLong(0L, 7L)
                : 8L + random.nextLong(0L, 17L);
        var receiptCount = notificationCount > 0L
                ? notificationCount * 2L + random.nextLong(0L, 5L)
                : 4L + random.nextLong(0L, 9L);
        var callCount = random.nextLong(0L, 2L);

        var chatThreadCount = roundToNearest(whatsapp.store().chatStore().chats().size(), CHAT_THREAD_ROUND_STEP);
        var sessionId = nextOfflineSessionId();
        var resumeMode = whatsapp.store().connectionStore().offlineResumeState()
                == LinkedWhatsAppClientOfflineResumeState.RESUME_WITH_OPEN_TAB
                ? OfflineResumeModes.RESUME_FROM_OPEN_TAB
                : OfflineResumeModes.RESUME_FROM_RESTART;

        var drainMillis = Math.max(OFFLINE_MIN_DRAIN_MILLIS, messageCount * PER_ENVELOPE_DRAIN_MILLIS);
        var socketConnectMillis = SOCKET_CONNECT_STAGE_MILLIS;
        var offlinePreviewMillis = socketConnectMillis + OFFLINE_PREVIEW_STAGE_GAP_MILLIS;
        var processCompleteMillis = offlinePreviewMillis + drainMillis;
        var lastPushTimestampMs = System.currentTimeMillis()
                - (MIN_PUSH_RECENCY_MILLIS + random.nextLong(0L, MAX_PUSH_RECENCY_MILLIS - MIN_PUSH_RECENCY_MILLIS + 1L));

        wamService.commit(baseEvent(sessionId, resumeMode, messageCount, receiptCount, notificationCount, callCount, chatThreadCount)
                .currentOfflineStage(OfflineResumeStages.SOCKET_CONNECT)
                .offlineStageTimestampMs(socketConnectMillis)
                .lastPushTimestampMs(lastPushTimestampMs)
                .build());

        wamService.commit(baseEvent(sessionId, resumeMode, messageCount, receiptCount, notificationCount, callCount, chatThreadCount)
                .currentOfflineStage(OfflineResumeStages.OFFLINE_PREVIEW)
                .offlineStageTimestampMs(offlinePreviewMillis)
                .build());

        wamService.commit(baseEvent(sessionId, resumeMode, messageCount, receiptCount, notificationCount, callCount, chatThreadCount)
                .currentOfflineStage(OfflineResumeStages.PROCESS_COMPLETE)
                .offlineStageTimestampMs(processCompleteMillis)
                .build());

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "emitted offline resume stages session={0} mode={1} chatThreadCount={2} drainMillis={3}",
                    sessionId, resumeMode, chatThreadCount, drainMillis);
        }
    }

    /**
     * Builds a fresh {@link OfflineResumeStageEventBuilder} pre-populated with the resume-session fields that are shared
     * by every stage event of one resume cycle.
     *
     * <p>The caller sets the stage-specific {@code currentOfflineStage} and {@code offlineStageTimestampMs} (and, for
     * the socket-connect stage, {@code lastPushTimestampMs}) on the returned builder before building. Both foreground
     * flags are reported as {@code true} because a headless client has no page-visibility state and is always active.
     *
     * @implNote This implementation returns a new builder per stage rather than reusing one, so the socket-connect
     * stage's {@code lastPushTimestampMs} cannot leak into the later stages that WA Web leaves it unset on.
     *
     * @param offlineSessionId         the shared resume-session identifier
     * @param offlineResumeMode        the derived resume mode for the cycle
     * @param offlineMessageCount      the offline message count reported on every stage
     * @param offlineReceiptCount      the offline receipt count reported on every stage
     * @param offlineNotificationCount the observed offline notification count reported on every stage
     * @param offlineCallCount         the offline call count reported on every stage
     * @param chatThreadCount          the rounded live chat-thread count reported on every stage
     * @return a new builder carrying the shared stage fields
     */
    @WhatsAppWebExport(moduleName = "WAWebWamOfflineResumeReporter", exports = "OfflineResumeReporter",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static OfflineResumeStageEventBuilder baseEvent(String offlineSessionId, OfflineResumeModes offlineResumeMode,
                                                            long offlineMessageCount, long offlineReceiptCount,
                                                            long offlineNotificationCount, long offlineCallCount,
                                                            long chatThreadCount) {
        return new OfflineResumeStageEventBuilder()
                .offlineSessionId(offlineSessionId)
                .offlineResumeMode(offlineResumeMode)
                .isResumeInForeground(Boolean.TRUE)
                .isResumeStartedInForeground(Boolean.TRUE)
                .offlineMessageCount(offlineMessageCount)
                .offlineReceiptCount(offlineReceiptCount)
                .offlineNotificationCount(offlineNotificationCount)
                .offlineCallCount(offlineCallCount)
                .offlineDecryptErrorCount(0L)
                .mailboxAge(0L)
                .chatThreadCount(chatThreadCount);
    }

    /**
     * Generates a fresh offline-resume session identifier of four random hexadecimal digits followed by the current
     * Unix time in seconds.
     *
     * @implNote This implementation mirrors WA Web's {@code randomHex(4) + unixTime} construction; the
     * {@code 0x1_0000 | random} then {@code substring(1)} idiom left-pads the random component to exactly four hex
     * digits.
     *
     * @return the generated session identifier
     */
    @WhatsAppWebExport(moduleName = "WARandomHex", exports = "randomHex",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String nextOfflineSessionId() {
        var random = ThreadLocalRandom.current().nextInt(0x1_0000);
        return Integer.toHexString(0x1_0000 | random).substring(1) + Instant.now().getEpochSecond();
    }

    /**
     * Rounds a non-negative value to the nearest multiple of the given granularity.
     *
     * @implNote This implementation reproduces WA Web's {@code Math.round(value / granularity) * granularity} coarsening
     * applied to the chat-thread count before it is reported.
     *
     * @param value       the value to round
     * @param granularity the rounding step; must be positive
     * @return the value rounded to the nearest multiple of {@code granularity}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamOfflineResumeReporter", exports = "roundUp",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static long roundToNearest(long value, long granularity) {
        return Math.round((double) value / granularity) * granularity;
    }
}
