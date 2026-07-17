package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedChatsListener;
import com.github.auties00.cobalt.listener.linked.LinkedContactsListener;
import com.github.auties00.cobalt.listener.linked.LinkedNewStatusListener;
import com.github.auties00.cobalt.listener.linked.LinkedStatusListener;
import com.github.auties00.cobalt.listener.linked.LinkedWebHistorySyncMessagesListener;
import com.github.auties00.cobalt.listener.linked.LinkedWebHistorySyncPastParticipantsListener;
import com.github.auties00.cobalt.listener.linked.LinkedWebHistorySyncProgressListener;
import com.github.auties00.cobalt.exception.linked.WhatsAppHistorySyncException;
import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaConnectionService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppWamStore;
import com.github.auties00.cobalt.util.BufferedProtobufInputStream;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.media.StickerMetadata;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncMessageAccessStatus;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncNotification;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncType;
import com.github.auties00.cobalt.wire.linked.preference.StickerBuilder;
import com.github.auties00.cobalt.wire.linked.sync.history.HistorySync;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.*;
import com.github.auties00.cobalt.wire.wam.type.*;
import it.auties.protobuf.stream.ProtobufInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.InflaterInputStream;

/**
 * Live implementation of {@link WebHistorySyncService} that pulls every
 * {@link HistorySyncNotification} the primary device sends into a decoded
 * {@link HistorySync} payload, fans the result out to the registered
 * {@code LinkedWhatsAppClientListener}s, and ingests the phone-number to LID
 * mappings carried with each chunk.
 *
 * <p>The class flattens three WA Web modules into a single pipeline:
 * <ul>
 *   <li>{@code WAWebHandleHistorySyncNotification} provides the entry
 *       point that receives the notification and the
 *       MdBootstrapHistoryDataReceived telemetry.</li>
 *   <li>{@code WAWebHandleHistorySyncChunk} provides the
 *       download/decode/store body and the
 *       MdBootstrapHistoryDataDownloaded /
 *       MdBootstrapDataApplied telemetry.</li>
 *   <li>{@code WAWebHistorySyncNotificationUtils} provides the metric
 *       builders shared by every emission point.</li>
 * </ul>
 *
 * <p>Processing runs on a dedicated virtual thread so the stanza
 * dispatch loop is not blocked while a multi-megabyte CDN blob is
 * fetched and decoded.
 *
 * <p>The service is driven by the message receiver every time a
 * {@link HistorySyncNotification} arrives; it is not called directly. To
 * consume the decoded chunks, callers register a
 * {@code LinkedWhatsAppClientListener} and override
 * {@code onWebHistorySyncMessages}, {@code onWebHistorySyncProgress},
 * {@code onWebHistorySyncPastParticipants}, {@code onChats},
 * {@code onContacts}, or {@code onStatus}.
 *
 * @implNote This implementation collapses the per-stage status writes
 * that WA Web persists in {@code WAWebUserPrefsHistorySync}
 * ({@code DOWNLOADING}, {@code DOWNLOADED}, {@code DECODED},
 * {@code APPLIED} per-chunk markers) because Cobalt has no equivalent
 * user-prefs surface; only the WAM emissions are mirrored. The
 * dynamic-throttling, queue-up, dedup-by-msg-key, and
 * receipt-fan-out branches in {@code WAWebHandleHistorySyncChunk} are
 * also omitted because Cobalt's listener API runs on virtual threads
 * with no per-chat back-pressure.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleHistorySyncNotification")
@WhatsAppWebModule(moduleName = "WAWebHistorySyncNotificationUtils")
@WhatsAppWebModule(moduleName = "WAWebHandleHistorySyncChunk")
@WhatsAppWebModule(moduleName = "WAWebLogHistorySyncStatusAfterPairingJob")
@WhatsAppWebModule(moduleName = "WAWebGroupHistoryReceiverUserJourneyLogger")
public final class LiveWebHistorySyncService implements WebHistorySyncService {
    /**
     * The logger for {@link LiveWebHistorySyncService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveWebHistorySyncService.class);

    /**
     * The {@link LinkedWhatsAppClient} the service is bound to; used to reach
     * the store for the shared {@link MediaConnectionService} and the listener
     * registry.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The {@link LidMigrationService} that consumes the
     * {@code phoneNumberToLidMappings} carried by every history sync
     * chunk.
     */
    private final LidMigrationService lidMigrationService;

    /**
     * The {@link WamService} used to commit the per-chunk telemetry
     * events.
     */
    private final WamService wamService;

    /**
     * The shared {@link MediaConnectionService} used to download the
     * encrypted history-sync chunks.
     */
    private final MediaConnectionService mediaConnectionService;

    /**
     * Builds a service bound to {@code whatsapp}.
     *
     * <p>Constructed once by the client during startup; the service is shared
     * across every stanza-dispatcher thread. All collaborators are required
     * and validated up front so that a mis-wired client fails fast.
     *
     * @param whatsapp            the {@link LinkedWhatsAppClient} that owns
     *                            this service
     * @param lidMigrationService the {@link LidMigrationService} that
     *                            ingests the LID mappings carried by
     *                            every chunk
     * @param wamService          the {@link WamService} used for
     *                            telemetry commits
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveWebHistorySyncService(LinkedWhatsAppClient whatsapp, LidMigrationService lidMigrationService, WamService wamService, MediaConnectionService mediaConnectionService) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.lidMigrationService = Objects.requireNonNull(lidMigrationService, "lidMigrationService cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.mediaConnectionService = Objects.requireNonNull(mediaConnectionService, "mediaConnectionService cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation hands the notification to a dedicated virtual thread so the
     * stanza dispatch loop is not blocked while a multi-megabyte CDN blob is fetched and decoded.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleHistorySyncNotification",
            exports = "handleHistorySyncNotification",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void process(HistorySyncNotification notification) {
        if (notification == null) {
            return;
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "history sync notification received: type={0} chunk={1}",
                    notification.syncType().orElse(null),
                    notification.chunkOrder().isPresent() ? notification.chunkOrder().getAsInt() : -1);
        }
        Thread.startVirtualThread(() -> processSync(notification));
    }

    /**
     * Applies a {@link HistorySyncType#MESSAGE_ACCESS_STATUS} notification by recording whether the
     * primary has granted this device complete on-demand access to the message history.
     *
     * <p>This sync type carries no downloadable chunk, so {@link #processSync} dispatches to it before
     * any download or metric step runs. The grant is the wire form of the primary's per-device "all chat history"
     * versus "limited" setting and bounds how far back the companion may pull history on demand; it
     * is only meaningful for desktop companions, so the notification is ignored on any other platform.
     *
     * @implNote This implementation generalises WhatsApp Web's {@code isWindows} gate to every desktop
     * platform Cobalt impersonates, since Cobalt supports both the Windows and macOS desktop clients.
     *
     * @param notification the access-status notification to apply
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleHistorySyncMessageAccessStatusChange",
            exports = "handleHistorySyncMessageAccessStatusChange", adaptation = WhatsAppAdaptation.ADAPTED)
    private void applyMessageAccessStatus(HistorySyncNotification notification) {
        var store = whatsapp.store();
        if (!store.accountStore().device().platform().isDesktop()) {
            return;
        }
        var granted = notification.messageAccessStatus()
                .map(HistorySyncMessageAccessStatus::completeAccessGranted)
                .orElse(false);
        store.syncStore().setCompleteHistoryAccessGranted(granted);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "history message access status updated: granted={0}", granted);
        }
    }

    /**
     * Runs the full download-decrypt-decode-fanout pipeline for one
     * notification on the current (virtual) thread.
     *
     * <p>Invoked from the virtual thread scheduled by
     * {@link #process(HistorySyncNotification)}. Every failure is logged at
     * {@code WARNING} and swallowed because history sync is non-fatal; the
     * companion can continue operating on partial history and the primary will
     * retry on the next bootstrap cycle.
     *
     * @implNote This implementation emits the data-received,
     * start-downloading, and downloaded WAM events plus the terminal
     * data-applied event. The download/decode failure path commits the
     * downloaded event with a FAILURE result before the data-applied event so
     * dashboards see both metrics on every catastrophic processing error.
     *
     * @param notification the non-{@code null} notification to process
     */
    private void processSync(HistorySyncNotification notification) {
        if (notification.syncType().orElse(null) == HistorySyncType.MESSAGE_ACCESS_STATUS) {
            applyMessageAccessStatus(notification);
            return;
        }
        var applyStartTs = System.currentTimeMillis();
        var inlinePayload = notification.initialHistBootstrapInlinePayload().orElse(null);
        var sentViaMms = inlinePayload == null || inlinePayload.length == 0;
        emitHistoryDataReceived(notification);
        emitHistoryDataStartDownloading(notification, applyStartTs);
        HistorySync historySync;
        try {
            historySync = decode(notification);
        } catch (WhatsAppHistorySyncException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "history sync chunk processing failed: {0}", exception.getMessage());
            }
            emitHistoryDataDownloaded(notification, null, applyStartTs,
                    MdBootstrapStepResult.FAILURE, exception.getMessage());
            emitHistoryDataApplied(notification, null, sentViaMms, applyStartTs,
                    MdBootstrapStepResult.FAILURE, exception.getMessage());
            emitBootstrapHistorySyncStatusAfterPairing(notification, null,
                    MdHistorySyncStatusResult.FAIL_TO_DOWNLOAD);
            return;
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "history sync chunk processing failed", exception);
            }
            emitHistoryDataDownloaded(notification, null, applyStartTs,
                    MdBootstrapStepResult.FAILURE, exception.getMessage());
            emitHistoryDataApplied(notification, null, sentViaMms, applyStartTs,
                    MdBootstrapStepResult.FAILURE, exception.getMessage());
            emitBootstrapHistorySyncStatusAfterPairing(notification, null,
                    MdHistorySyncStatusResult.OTHER_ERROR);
            return;
        }
        if (historySync == null) {
            return;
        }
        emitHistoryDataDownloaded(notification, historySync, applyStartTs,
                MdBootstrapStepResult.SUCCESS, null);
        try {
            dispatch(historySync);
            emitHistoryDataApplied(notification, historySync, sentViaMms, applyStartTs,
                    MdBootstrapStepResult.SUCCESS, null);
            var recentSyncStatus = historySync.progress().orElse(0) >= 100
                    ? MdHistorySyncStatusResult.SUCCESS
                    : MdHistorySyncStatusResult.IN_PROGRESS;
            emitBootstrapHistorySyncStatusAfterPairing(notification, historySync, recentSyncStatus);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "history sync chunk applied: type={0} progress={1}",
                        historySync.syncType(), historySync.progress().orElse(0));
            }
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "history sync chunk dispatch failed", exception);
            }
            emitHistoryDataApplied(notification, historySync, sentViaMms, applyStartTs,
                    MdBootstrapStepResult.FAILURE, exception.getMessage());
            emitBootstrapHistorySyncStatusAfterPairing(notification, historySync,
                    MdHistorySyncStatusResult.FAIL_TO_STORE);
        }
    }

    /**
     * Commits a
     * {@link MdBootstrapDataAppliedEventBuilder MdBootstrapDataAppliedEvent}
     * recording the terminal step of one chunk's processing.
     *
     * <p>Invoked from the success and failure paths of {@link #processSync}.
     *
     * @implNote This implementation builds and commits the event in one step
     * rather than seeding static fields up front and finalising later. The
     * {@code mdSessionId} field is omitted because it is an identity-key hash
     * that Cobalt has no equivalent derivation for.
     *
     * @param notification  the notification whose chunk was processed
     * @param historySync   the decoded payload, or {@code null} when
     *                      decoding failed and the caller is reporting
     *                      the failure
     * @param sentViaMms    {@code true} when the chunk was fetched via
     *                      the CDN, {@code false} when an inline
     *                      bootstrap payload was used
     * @param applyStartTs  the start-of-processing timestamp captured
     *                      at the top of {@link #processSync}
     * @param result        the step result to record
     * @param failureReason the failure reason, or {@code null} on the
     *                      success path
     */
    @WhatsAppWebExport(moduleName = "WAWebHistorySyncNotificationUtils",
            exports = "getHistorySyncMetrics", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebHistorySyncNotificationUtils",
            exports = "commitHistoryDataAppliedMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitHistoryDataApplied(
            HistorySyncNotification notification,
            HistorySync historySync,
            boolean sentViaMms,
            long applyStartTs,
            MdBootstrapStepResult result,
            String failureReason
    ) {
        var syncType = historySync != null ? historySync.syncType() : null;
        var payloadType = syncType == HistorySyncType.INITIAL_BOOTSTRAP
                ? MdBootstrapPayloadType.CRITICAL
                : MdBootstrapPayloadType.NON_CRITICAL;
        var historyPayloadType = mapHistoryPayloadType(syncType);
        var now = System.currentTimeMillis();
        var duration = (int) (now - applyStartTs);
        var builder = new MdBootstrapDataAppliedEventBuilder()
                .mdBootstrapPayloadType(payloadType)
                .mdBootstrapSource(MdBootstrapSource.HISTORY)
                .mdBootstrapHistoryPayloadType(historyPayloadType)
                .sentViaMms(sentViaMms)
                .mdTimestamp((int) now)
                .mdBootstrapStepDuration(duration)
                .mdBootstrapStepResult(result);
        var chunkOrder = notification.chunkOrder();
        if (chunkOrder.isPresent()) {
            builder.historySyncChunkOrder(chunkOrder.getAsInt());
        }
        var progress = resolveStageProgress(notification, syncType);
        builder.historySyncStageProgress(progress);
        if (failureReason != null) {
            builder.mdSyncFailureReason(failureReason);
        }
        wamService.commit(builder.build());
    }

    /**
     * Commits a
     * {@link MdBootstrapHistoryDataDownloadedEventBuilder MdBootstrapHistoryDataDownloadedEvent}
     * after the encrypted blob has been fetched (or inlined) and decoded into
     * a {@link HistorySync}.
     *
     * <p>Invoked from the success and failure paths of {@link #processSync}.
     *
     * @implNote This implementation builds and commits the event in one step.
     * Per-chunk message counts are aggregated locally: the chats path sums the
     * per-chat message count; the status-only fallback uses the status message
     * count; the pushname-only fallback uses the pushname count as a coarse
     * equivalent. The {@code mdSessionId} and storage-quota fields are omitted
     * because they are an identity-key hash and a browser storage estimate
     * that Cobalt has no equivalent for.
     *
     * @param notification  the notification whose payload was
     *                      downloaded
     * @param historySync   the decoded payload, or {@code null} when
     *                      the download or decode itself failed
     * @param startTs       the start-of-processing timestamp captured
     *                      at the top of {@link #processSync}
     * @param result        the step result to record
     * @param failureReason the failure reason, or {@code null} on the
     *                      success path
     */
    @WhatsAppWebExport(moduleName = "WAWebHistorySyncNotificationUtils",
            exports = "getHistorySyncMetrics", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebHistorySyncNotificationUtils",
            exports = "commitHistoryDownloadedMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitHistoryDataDownloaded(
            HistorySyncNotification notification,
            HistorySync historySync,
            long startTs,
            MdBootstrapStepResult result,
            String failureReason
    ) {
        var syncType = historySync != null
                ? historySync.syncType()
                : notification.syncType().orElse(null);
        var payloadType = syncType == HistorySyncType.INITIAL_BOOTSTRAP
                ? MdBootstrapPayloadType.CRITICAL
                : MdBootstrapPayloadType.NON_CRITICAL;
        var historyPayloadType = mapHistoryPayloadType(syncType);
        var now = System.currentTimeMillis();
        var duration = (int) (now - startTs);
        var builder = new MdBootstrapHistoryDataDownloadedEventBuilder()
                .mdBootstrapPayloadType(payloadType)
                .mdBootstrapHistoryPayloadType(historyPayloadType)
                .mdTimestamp((int) now)
                .mdBootstrapStepDuration(duration)
                .mdBootstrapStepResult(result);
        var payloadSize = resolvePayloadSize(notification);
        if (payloadSize != null) {
            builder.mdBootstrapPayloadSize(payloadSize);
        }
        var chunkOrder = notification.chunkOrder();
        if (chunkOrder.isPresent()) {
            builder.historySyncChunkOrder(chunkOrder.getAsInt());
        }
        var progress = resolveStageProgress(notification, syncType);
        builder.historySyncStageProgress(progress);
        if (historySync != null) {
            var chats = historySync.chats();
            if (!chats.isEmpty()) {
                builder.mdBootstrapChatsCount(chats.size());
                var messagesCount = 0;
                for (var chat : chats) {
                    messagesCount += chat.messageCount();
                }
                builder.mdBootstrapMessagesCount(messagesCount);
            } else if (!historySync.statusV3Messages().isEmpty()) {
                builder.mdBootstrapMessagesCount(historySync.statusV3Messages().size());
            } else if (!historySync.pushnames().isEmpty()) {
                builder.mdBootstrapMessagesCount(historySync.pushnames().size());
            }
        }
        if (failureReason != null) {
            builder.mdSyncFailureReason(failureReason);
        }
        wamService.commit(builder.build());
    }

    /**
     * Commits a
     * {@link MdBootstrapHistoryDataStartDownloadingEventBuilder MdBootstrapHistoryDataStartDownloadingEvent}
     * to mark the start of one chunk's download step.
     *
     * <p>Invoked from {@link #processSync} right after the notification has
     * been accepted and before the CDN fetch or inline-payload read. Cobalt
     * funnels every download path through {@link #processSync}, so this single
     * emission covers all of them.
     *
     * @implNote This implementation reads the sync type from the notification
     * (the decoded payload is not yet available) and computes a near-zero step
     * duration because the start-downloading step has no preceding work.
     *
     * @param notification the non-{@code null} notification whose
     *                     chunk is about to be downloaded
     * @param applyStartTs the start-of-processing timestamp captured
     *                     at the top of {@link #processSync}
     */
    @WhatsAppWebExport(moduleName = "WAWebHistorySyncNotificationUtils",
            exports = "getHistorySyncMetrics", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebHistorySyncNotificationUtils",
            exports = "commitHistoryStartDownloadingMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitHistoryDataStartDownloading(HistorySyncNotification notification, long applyStartTs) {
        var syncType = notification.syncType().orElse(null);
        var payloadType = syncType == HistorySyncType.INITIAL_BOOTSTRAP
                ? MdBootstrapPayloadType.CRITICAL
                : MdBootstrapPayloadType.NON_CRITICAL;
        var historyPayloadType = mapHistoryPayloadType(syncType);
        var now = System.currentTimeMillis();
        var duration = (int) (now - applyStartTs);
        var builder = new MdBootstrapHistoryDataStartDownloadingEventBuilder()
                .mdBootstrapPayloadType(payloadType)
                .mdBootstrapHistoryPayloadType(historyPayloadType)
                .mdTimestamp((int) now)
                .mdBootstrapStepDuration(duration);
        var payloadSize = resolvePayloadSize(notification);
        if (payloadSize != null) {
            builder.mdBootstrapPayloadSize(payloadSize);
        }
        var chunkOrder = notification.chunkOrder();
        if (chunkOrder.isPresent()) {
            builder.historySyncChunkOrder(chunkOrder.getAsInt());
        }
        var progress = resolveStageProgress(notification, syncType);
        builder.historySyncStageProgress(progress);
        wamService.commit(builder.build());
    }

    /**
     * Commits a
     * {@link MdBootstrapHistoryDataReceivedEventBuilder MdBootstrapHistoryDataReceivedEvent}
     * to record the arrival of a notification, before any download or decode
     * work happens.
     *
     * <p>Invoked from {@link #processSync} exactly once per notification,
     * immediately after the local processing context has been set up.
     *
     * @implNote This implementation fires the event only when the notification
     * carries at least one of an inline bootstrap payload or a
     * {@code (directPath, mediaKey)} CDN handle. Notifications without any of
     * these (the message-access-status / no-history markers) are silently
     * skipped.
     *
     * @param notification the notification whose arrival is being
     *                     reported
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleHistorySyncNotification",
            exports = "handleHistorySyncNotification", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitHistoryDataReceived(HistorySyncNotification notification) {
        var inlinePayload = notification.initialHistBootstrapInlinePayload().orElse(null);
        var hasInline = inlinePayload != null && inlinePayload.length > 0;
        var hasCdn = notification.directPath().isPresent() && notification.mediaKey().isPresent();
        if (!hasInline && !hasCdn) {
            return;
        }
        var syncType = notification.syncType().orElse(null);
        var payloadType = syncType == HistorySyncType.INITIAL_BOOTSTRAP
                ? MdBootstrapPayloadType.CRITICAL
                : MdBootstrapPayloadType.NON_CRITICAL;
        var historyPayloadType = mapHistoryPayloadType(syncType);
        var builder = new MdBootstrapHistoryDataReceivedEventBuilder()
                .mdBootstrapPayloadType(payloadType)
                .mdBootstrapHistoryPayloadType(historyPayloadType)
                .mdTimestamp((int) System.currentTimeMillis())
                .historySyncStageProgress(resolveStageProgress(notification, syncType));
        var chunkOrder = notification.chunkOrder();
        if (chunkOrder.isPresent()) {
            builder.historySyncChunkOrder(chunkOrder.getAsInt());
        }
        wamService.commit(builder.build());
    }

    /**
     * Commits a
     * {@link MdBootstrapHistorySyncStatusAfterPairingEventBuilder MdBootstrapHistorySyncStatusAfterPairingEvent}
     * summarising the state of the recent-history bootstrap sync at the moment a
     * {@link HistorySyncType#RECENT} chunk finishes processing.
     *
     * <p>WhatsApp Web fires this event from a scheduled post-pairing job
     * ({@code logHistorySyncStatusAfterPairing}) that wakes at fixed 5, 10, 20,
     * 40, and 60 minute checkpoints after pairing and reports the recent-sync
     * progress up to five times. Cobalt has no such scheduler in this service,
     * so the summary is instead emitted at the natural completion boundary of
     * every recent-history chunk: a {@link MdHistorySyncStatusResult#SUCCESS}
     * on the terminal chunk (progress at or above {@code 100}),
     * {@link MdHistorySyncStatusResult#IN_PROGRESS} on an intermediate chunk,
     * and the caller-supplied failure result on the download, decode, and store
     * failure paths. Non-recent sync types short-circuit without emitting,
     * matching WhatsApp Web's exclusive focus on the recent sync (the job always
     * reports {@link MdBootstrapHistoryPayloadType#RECENT_HISTORY}).
     *
     * @implNote This implementation derives {@code activeTimeAfterPairing} from
     * {@link LinkedWhatsAppAccountStore#pairingTimestamp()}, falling back to
     * {@link LinkedWhatsAppAccountStore#initializationTimeStamp()} when the
     * pairing instant has not been recorded, and clamps any elapsed time under
     * five minutes up to {@link ActiveTimeAfterPairing#MINS_5} because Cobalt
     * emits at chunk completion rather than on the timer that WhatsApp Web uses
     * to gate the sub-five-minute window out. The {@code mdSessionId} and
     * {@code missingNotificationCount} fields are omitted: the former is an
     * identity-key hash Cobalt has no equivalent derivation for, and the latter
     * is never set by the WhatsApp Web emitter. {@code unprocessedNotificationCount}
     * and {@code nextNotificationChunkOrder} are reported only on the
     * non-success paths, mirroring the WhatsApp Web control flow that commits the
     * completed case before those fields are computed; Cobalt processes one
     * chunk at a time with no notification backlog, so the unprocessed count is
     * a genuine zero and the next order is the successor of the current chunk.
     *
     * @param notification the notification whose recent-sync chunk was
     *                     processed
     * @param historySync  the decoded payload, or {@code null} when the
     *                     download or decode failed before a payload was
     *                     available
     * @param result       the recent-sync status result to record
     */
    @WhatsAppWebExport(moduleName = "WAWebLogHistorySyncStatusAfterPairingJob",
            exports = "commitHistorySyncStatusData", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitBootstrapHistorySyncStatusAfterPairing(
            HistorySyncNotification notification,
            HistorySync historySync,
            MdHistorySyncStatusResult result
    ) {
        var syncType = historySync != null
                ? historySync.syncType()
                : notification.syncType().orElse(null);
        if (syncType != HistorySyncType.RECENT) {
            return;
        }
        var accountStore = whatsapp.store().accountStore();
        var pairing = accountStore.pairingTimestamp()
                .orElseGet(accountStore::initializationTimeStamp);
        var elapsedMinutes = Duration.between(pairing, Instant.now()).toMinutes();
        var succeeded = result == MdHistorySyncStatusResult.SUCCESS;
        var chunkOrder = notification.chunkOrder();
        var lastChunkOrder = chunkOrder.isPresent() ? chunkOrder.getAsInt() : -1;
        var messagesCount = 0L;
        if (historySync != null) {
            for (var chat : historySync.chats()) {
                messagesCount += chat.messageCount();
            }
        }
        var builder = new MdBootstrapHistorySyncStatusAfterPairingEventBuilder()
                .mdBootstrapHistoryPayloadType(MdBootstrapHistoryPayloadType.RECENT_HISTORY)
                .activeTimeAfterPairing(mapActiveTimeAfterPairing(elapsedMinutes))
                .isLoopRunning(!succeeded)
                .mdTimestamp(System.currentTimeMillis())
                .mdHistorySyncStatusResult(result)
                .lastProcessedNotificationChunkOrder(lastChunkOrder)
                .lastProcessedNotificationChunkProgress(notification.progress().orElse(-1))
                .totalProcessedMessageCount(messagesCount);
        if (!succeeded) {
            builder.unprocessedNotificationCount(0);
            builder.nextNotificationChunkOrder(lastChunkOrder < 0 ? -1 : lastChunkOrder + 1);
        }
        wamService.commit(builder.build());
    }

    /**
     * Maps the number of minutes elapsed since pairing onto the
     * {@link ActiveTimeAfterPairing} bucket reported by the recent-history
     * bootstrap status summary.
     *
     * <p>Invoked from
     * {@link #emitBootstrapHistorySyncStatusAfterPairing(HistorySyncNotification, HistorySync, MdHistorySyncStatusResult)}.
     * The buckets follow WhatsApp Web's checkpoint schedule: 60 or more minutes
     * to {@link ActiveTimeAfterPairing#MINS_60}, 40 or more to
     * {@link ActiveTimeAfterPairing#MINS_40}, 20 or more to
     * {@link ActiveTimeAfterPairing#MINS_20}, 10 or more to
     * {@link ActiveTimeAfterPairing#MINS_10}, and anything below to
     * {@link ActiveTimeAfterPairing#MINS_5}.
     *
     * @implNote This implementation returns {@link ActiveTimeAfterPairing#MINS_5}
     * for any value under five minutes rather than declining to log, because
     * Cobalt emits the summary at recent-sync-chunk completion and cannot defer
     * an early completion to the next checkpoint the way WhatsApp Web's timer
     * does.
     *
     * @param elapsedMinutes the whole minutes elapsed since pairing
     * @return the matching {@link ActiveTimeAfterPairing} bucket
     */
    @WhatsAppWebExport(moduleName = "WAWebLogHistorySyncStatusAfterPairingJob",
            exports = "logHistorySyncStatusAfterPairing", adaptation = WhatsAppAdaptation.ADAPTED)
    private static ActiveTimeAfterPairing mapActiveTimeAfterPairing(long elapsedMinutes) {
        if (elapsedMinutes >= 60) {
            return ActiveTimeAfterPairing.MINS_60;
        }
        if (elapsedMinutes >= 40) {
            return ActiveTimeAfterPairing.MINS_40;
        }
        if (elapsedMinutes >= 20) {
            return ActiveTimeAfterPairing.MINS_20;
        }
        if (elapsedMinutes >= 10) {
            return ActiveTimeAfterPairing.MINS_10;
        }
        return ActiveTimeAfterPairing.MINS_5;
    }

    /**
     * Resolves the payload-size field for the downloaded and start-downloading
     * metrics from the notification.
     *
     * <p>Sources the size from the notification's file length.
     *
     * @implNote This implementation falls back to the inline bootstrap payload
     * byte count when the file length is absent, so inline-bootstrap
     * dashboards still receive a size signal even though the field is normally
     * set only on the CDN-announced path.
     *
     * @param notification the non-{@code null} notification being
     *                     processed
     * @return the encrypted blob size, the inline payload size, or
     *         {@code null} when neither is available
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleHistorySyncNotification",
            exports = "handleHistorySyncNotification",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static Integer resolvePayloadSize(HistorySyncNotification notification) {
        var mediaSize = notification.fileLength();
        if (mediaSize.isPresent()) {
            return (int) mediaSize.getAsLong();
        }
        var inline = notification.initialHistBootstrapInlinePayload().orElse(null);
        if (inline != null) {
            return inline.length;
        }
        return null;
    }

    /**
     * Maps a {@link HistorySyncType} onto the
     * {@link MdBootstrapHistoryPayloadType} value reported via the
     * history-payload-type property of every history sync metric.
     *
     * <p>Invoked from every metric emitter. The mapping follows the protobuf
     * wire value: {@link HistorySyncType#INITIAL_BOOTSTRAP} to
     * {@link MdBootstrapHistoryPayloadType#INITIAL},
     * {@link HistorySyncType#INITIAL_STATUS_V3} to
     * {@link MdBootstrapHistoryPayloadType#STATUS_V3},
     * {@link HistorySyncType#FULL} to
     * {@link MdBootstrapHistoryPayloadType#FULL_HISTORY},
     * {@link HistorySyncType#RECENT} to
     * {@link MdBootstrapHistoryPayloadType#RECENT_HISTORY},
     * {@link HistorySyncType#NON_BLOCKING_DATA} to
     * {@link MdBootstrapHistoryPayloadType#NON_BLOCKING_DATA},
     * {@link HistorySyncType#ON_DEMAND} to
     * {@link MdBootstrapHistoryPayloadType#ON_DEMAND}, and everything else to
     * {@link MdBootstrapHistoryPayloadType#PUSHNAME}.
     *
     * @implNote This implementation also returns
     * {@link MdBootstrapHistoryPayloadType#PUSHNAME} for a {@code null} input
     * so the decode-failure metric path does not throw.
     *
     * @param syncType the decoded sync type, or {@code null} when
     *                 decoding failed before the payload was available
     * @return the matching {@link MdBootstrapHistoryPayloadType}
     */
    @WhatsAppWebExport(moduleName = "WAWebGetMetricHistorySyncPayloadType",
            exports = "getMetricHistorySyncPayloadType", adaptation = WhatsAppAdaptation.DIRECT)
    private static MdBootstrapHistoryPayloadType mapHistoryPayloadType(HistorySyncType syncType) {
        return switch (syncType) {
            case INITIAL_BOOTSTRAP -> MdBootstrapHistoryPayloadType.INITIAL;
            case INITIAL_STATUS_V3 -> MdBootstrapHistoryPayloadType.STATUS_V3;
            case FULL -> MdBootstrapHistoryPayloadType.FULL_HISTORY;
            case RECENT -> MdBootstrapHistoryPayloadType.RECENT_HISTORY;
            case NON_BLOCKING_DATA -> MdBootstrapHistoryPayloadType.NON_BLOCKING_DATA;
            case ON_DEMAND -> MdBootstrapHistoryPayloadType.ON_DEMAND;
            case null, default -> MdBootstrapHistoryPayloadType.PUSHNAME;
        };
    }

    /**
     * Resolves the stage-progress percentage reported via every history sync
     * metric.
     *
     * <p>Invoked from every metric emitter. Returns the notification's
     * progress unless the sync is {@link HistorySyncType#FULL}, in which case
     * the progress is clamped to {@code 100}; the default for a missing
     * progress field is {@code 0}.
     *
     * @implNote This implementation cannot detect the last
     * {@link HistorySyncType#RECENT} chunk because that requires a user-prefs
     * chunk-count lookup Cobalt does not implement; the {@code RECENT} branch
     * instead relies on the notification's progress field, which the primary
     * device already clamps to {@code 100} on the terminal chunk.
     *
     * @param notification the chunk whose progress is being reported
     * @param syncType     the decoded sync type, or {@code null} on
     *                     decode failures
     * @return the stage progress percentage between {@code 0} and
     *         {@code 100}
     */
    @WhatsAppWebExport(moduleName = "WAWebGetHistorySyncProgress",
            exports = "getHistorySyncProgress", adaptation = WhatsAppAdaptation.ADAPTED)
    private static int resolveStageProgress(HistorySyncNotification notification, HistorySyncType syncType) {
        if (syncType == HistorySyncType.FULL) {
            return 100;
        }
        return notification.progress().orElse(0);
    }

    /**
     * Resolves a notification to a decoded {@link HistorySync} payload,
     * reading either the inline bootstrap payload or the decrypted CDN blob.
     *
     * <p>Invoked from {@link #processSync}. The inline payload short-circuits;
     * otherwise the CDN blob is downloaded with HMAC verification and AES-CBC
     * decryption.
     *
     * @implNote This implementation returns {@code null} when neither an
     * inline payload nor a {@code (directPath, mediaKey)} pair is available,
     * which can legitimately happen for the message-access-status or
     * no-history markers; the caller then short-circuits the dispatch step and
     * skips the downloaded and data-applied metric emissions.
     *
     * @param notification the non-{@code null} notification to decode
     * @return the decoded payload, or {@code null} when the
     *         notification carries no recoverable bytes
     * @throws WhatsAppHistorySyncException if the inline payload fails
     *                                      to inflate or the CDN
     *                                      download / decode fails
     */
    @WhatsAppWebExport(moduleName = "WAWebDownloadManager", exports = "downloadAndMaybeDecrypt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private HistorySync decode(HistorySyncNotification notification) {
        var inlinePayload = notification.initialHistBootstrapInlinePayload().orElse(null);
        if (inlinePayload != null && inlinePayload.length > 0) {
            try (var stream = new InflaterInputStream(new ByteArrayInputStream(inlinePayload))) {
                return decodeHistorySync(stream);
            } catch (Exception exception) {
                throw new WhatsAppHistorySyncException("Failed to decode inline history bootstrap payload", exception);
            }
        }

        if (notification.directPath().isEmpty() || notification.mediaKey().isEmpty()) {
            return null;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "downloading history sync chunk from cdn");
        }
        var downloadStart = Instant.now();
        try (var stream = mediaConnectionService.download(notification)) {
            var decoded = decodeHistorySync(stream);
            commitMediaDownload2Success(downloadStart);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "history sync chunk downloaded in {0} ms",
                        Duration.between(downloadStart, Instant.now()).toMillis());
            }
            return decoded;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WhatsAppHistorySyncException("Interrupted while waiting for media connection", exception);
        } catch (Exception exception) {
            commitMediaDownload2Failure(downloadStart, exception);
            throw new WhatsAppHistorySyncException("Failed to download or decode history sync chunk", exception);
        }
    }

    /**
     * Commits a successful
     * {@link MediaDownload2EventBuilder MediaDownload2Event} for the
     * just-finished history sync chunk download.
     *
     * <p>Invoked from the success branch of
     * {@link #decode(HistorySyncNotification)} with the constants for the
     * history-sync flow: media type {@link MediaType#MD_HISTORY_SYNC}, MMS v4,
     * {@link DownloadOriginType#MESSAGE_HISTORY_SYNC} origin, and
     * {@link MediaDownloadModeType#FULL} mode.
     *
     * @param downloadStart the wall-clock instant at which the
     *                      download attempt began
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaDownloadMetrics",
            exports = "createMediaDownloadMetrics",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMediaDownload2Success(Instant downloadStart) {
        var overallT = Instant.ofEpochMilli(Duration.between(downloadStart, Instant.now()).toMillis());
        wamService.commit(new MediaDownload2EventBuilder()
                .overallMediaType(MediaType.MD_HISTORY_SYNC)
                .overallMmsVersion(4)
                .overallDownloadOrigin(DownloadOriginType.MESSAGE_HISTORY_SYNC)
                .overallDownloadMode(MediaDownloadModeType.FULL)
                .overallDownloadResult(MediaDownloadResultType.OK)
                .overallIsFinal(Boolean.TRUE)
                .downloadHttpCode(200)
                .overallT(overallT)
                .overallAttemptCount(1)
                .overallRetryCount(0)
                .build());
    }

    /**
     * Commits a failing
     * {@link MediaDownload2EventBuilder MediaDownload2Event} for the
     * just-attempted history sync chunk download.
     *
     * <p>Invoked from the failure branch of
     * {@link #decode(HistorySyncNotification)}; the HTTP-code dimension is set
     * only when a status code is present on the failure.
     *
     * @param downloadStart the wall-clock instant at which the
     *                      download attempt began
     * @param throwable     the failure that aborted the download
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaDownloadMetrics",
            exports = "createMediaDownloadMetrics",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricDownloadErrorResultType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMediaDownload2Failure(Instant downloadStart, Throwable throwable) {
        var overallT = Instant.ofEpochMilli(Duration.between(downloadStart, Instant.now()).toMillis());
        var builder = new MediaDownload2EventBuilder()
                .overallMediaType(MediaType.MD_HISTORY_SYNC)
                .overallMmsVersion(4)
                .overallDownloadOrigin(DownloadOriginType.MESSAGE_HISTORY_SYNC)
                .overallDownloadMode(MediaDownloadModeType.FULL)
                .overallDownloadResult(classifyMediaDownloadError(throwable))
                .overallIsFinal(Boolean.TRUE)
                .overallT(overallT)
                .overallAttemptCount(1)
                .overallRetryCount(0);
        var statusCode = extractHttpStatusCode(throwable);
        if (statusCode != null) {
            builder.downloadHttpCode(statusCode);
        }
        wamService.commit(builder.build());
    }

    /**
     * Maps a download failure to the matching
     * {@link MediaDownloadResultType}.
     *
     * <p>Invoked from {@link #commitMediaDownload2Failure(Instant, Throwable)}.
     * The failure is classified by HTTP status code, with no status meaning
     * {@link MediaDownloadResultType#ERROR_NETWORK} and an unrecognised status
     * meaning {@link MediaDownloadResultType#ERROR_UNKNOWN}.
     *
     * @param throwable the failure raised by the download path
     * @return the mapped {@link MediaDownloadResultType}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricDownloadErrorResultType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MediaDownloadResultType classifyMediaDownloadError(Throwable throwable) {
        for (var current = throwable; current != null; current = current.getCause()) {
            if (current instanceof WhatsAppMediaException.Download download) {
                var optStatus = download.httpStatusCode();
                if (optStatus.isEmpty()) {
                    return MediaDownloadResultType.ERROR_NETWORK;
                }
                var status = optStatus.getAsInt();
                return switch (status) {
                    case 404, 410 -> MediaDownloadResultType.ERROR_TOO_OLD;
                    case 416 -> MediaDownloadResultType.ERROR_CANNOT_RESUME;
                    case 401 -> MediaDownloadResultType.ERROR_INVALID_URL;
                    case 429, 507 -> MediaDownloadResultType.ERROR_THROTTLE;
                    default -> MediaDownloadResultType.ERROR_UNKNOWN;
                };
            }
        }
        return MediaDownloadResultType.ERROR_UNKNOWN;
    }

    /**
     * Extracts the HTTP status code embedded in a download failure, if any.
     *
     * <p>Invoked from {@link #commitMediaDownload2Failure(Instant, Throwable)}.
     *
     * @param throwable the failure raised by the download path
     * @return the status code, or {@code null} when none is available
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getStatusCode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static Integer extractHttpStatusCode(Throwable throwable) {
        for (var current = throwable; current != null; current = current.getCause()) {
            if (current instanceof WhatsAppMediaException.Download download) {
                var optStatus = download.httpStatusCode();
                return optStatus.isPresent() ? optStatus.getAsInt() : null;
            }
        }
        return null;
    }

    /**
     * Decodes a plaintext history sync byte stream into a {@link HistorySync}
     * payload.
     *
     * <p>Invoked from {@link #decode(HistorySyncNotification)} for both the
     * inline and CDN paths.
     *
     * @implNote This implementation routes through the lightweight
     * {@link HistorySync#ofLight(ProtobufInputStream)} variant when the caller
     * configured the discard history mode, which trims the wire-decoded
     * payload to the fields Cobalt actually projects into the listener API;
     * the full decoder
     * ({@link HistorySync#ofFull(ProtobufInputStream)}) is used otherwise.
     *
     * @param stream the plaintext protobuf stream
     * @return the decoded payload
     */
    @WhatsAppWebExport(moduleName = "WAWebBinHistorySync", exports = "HistorySync",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private HistorySync decodeHistorySync(InputStream stream) {
        var protoStream = new BufferedProtobufInputStream(stream);
        if (whatsapp.store().syncStore().isHistoryDiscarded()) {
            return HistorySync.ofLight(protoStream);
        } else {
            return HistorySync.ofFull(protoStream);
        }
    }

    /**
     * Applies every wire-shape mutation carried by the decoded chunk to the
     * {@link LinkedWhatsAppStore} and fans the result out to every registered
     * {@code LinkedWhatsAppClientListener}.
     *
     * <p>Invoked from the success branch of {@link #processSync}; it ingests
     * the chats, pushnames, status messages, call logs, and recent stickers
     * carried by the chunk and notifies the registered listeners.
     *
     * @implNote This implementation runs every listener callback on a
     * dedicated virtual thread so a slow listener cannot block the next
     * chunk or the LID-mapping ingest. Per-chunk store mutations cover
     * the data shapes that have an equivalent in
     * {@link LinkedWhatsAppStore}: every {@code conversation} is merged into
     * the local {@link Chat} record (metadata fields plus embedded
     * messages); every {@code pushname} is folded into a
     * {@link com.github.auties00.cobalt.wire.linked.contact.Contact} via
     * {@link com.github.auties00.cobalt.wire.linked.contact.Contact#setChosenName(String)};
     * every {@code statusV3Messages} entry is appended to
     * {@link LinkedWhatsAppChatStore#status()}; every {@code callLogRecords}
     * entry that carries a callId is registered through
     * {@link LinkedWhatsAppChatStore#addCallLog(com.github.auties00.cobalt.wire.linked.call.CallLog)};
     * every {@code recentStickers} entry that carries a {@code fileSha256}
     * is folded into a {@link com.github.auties00.cobalt.wire.linked.preference.Sticker}
     * via {@link LinkedWhatsAppSettingsStore#addRecentSticker(String, com.github.auties00.cobalt.wire.linked.preference.Sticker)},
     * keyed by the base64-encoded plaintext hash to match WA Web's
     * {@code WAWebRecentStickerCollectionMd} row id; the
     * {@code companionMmsAuthNonce} (wire field {@code companionMetaNonce})
     * is captured into
     * {@link LinkedWhatsAppStore#setCompanionMmsAuthNonce(String)} so it can
     * authorise the post-apply MMS blob-deletion call, matching WA Web's
     * write to {@code userPrefsIdb["WAWebCompanionMetaNonce"]} inside
     * {@code WAWebHistoryMsgHandlerAction.handleInitialSyncMsgs};
     * the {@code shareableChatLinkKey} (wire field
     * {@code shareableChatIdentifierEncryptionKey}) is captured into
     * {@link LinkedWhatsAppStore#setShareableChatLinkKey(byte[])} and
     * persisted alongside the rest of the store so the
     * shareable-chat-link encryption material survives across restarts;
     * the WA Web bundle itself never reads the value (the deep-link
     * generator is server-driven), so capturing it is the full extent
     * of client-side responsibility.
     * Past participants are forwarded only as a listener event because
     * Cobalt's store has no past-participants collection. The
     * {@code threadIdUserSecret} and {@code threadDsTimeframeOffset}
     * fields are projected into
     * {@link LinkedWhatsAppWamStore#setChatThreadLoggingSecret(byte[])}
     * and {@link LinkedWhatsAppWamStore#setChatThreadLoggingOffset(Integer)}
     * to feed the ctlv2 thread-logging uploader (the HMAC seed and
     * disappearing-mode window offset for the {@code ThreadInteractionData}
     * WAM events).
     * The {@code globalSettings} sub-message is decoded for protobuf
     * parity but deliberately not projected onto the store, because
     * WA Web does not read it from the history-sync chunk either: the
     * authoritative sources for those preferences are the app-state
     * (syncd) collections instead. {@code chatLockSettings} arrives
     * via {@code WAWebChatLockSettingsSync}, the disappearing-mode
     * default and security-notifications flag arrive via separate
     * syncd / user-prefs flows, and the bundle never accesses
     * {@code historySync.globalSettings} at runtime.
     * Terminal "sync complete" callbacks mirror WA Web's {@code Y()}
     * dispatcher: the {@code syncedChats} flag flips when the
     * {@link HistorySyncType#INITIAL_BOOTSTRAP} chunk lands
     * (triggerInitialChatHistorySynced), {@code syncedContacts} flips
     * on the {@link HistorySyncType#PUSH_NAME} chunk, and
     * {@code syncedStatus} flips on the
     * {@link HistorySyncType#INITIAL_STATUS_V3} chunk; the
     * {@code onChats} / {@code onContacts} / {@code onStatus} callbacks
     * each fire exactly once per session, with the full store
     * collection, even when the originating chunk is empty.
     *
     * @param historySync the decoded payload to dispatch
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleHistorySyncChunk", exports = "handleHistorySyncChunk",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void dispatch(HistorySync historySync) {
        lidMigrationService.processHistorySync(historySync);

        var store = whatsapp.store();
        var listeners = store.listeners();
        var syncType = historySync.syncType();
        var progressValue = historySync.progress().orElse(0);
        var recent = syncType == HistorySyncType.RECENT;
        var isLastChunk = progressValue >= 100;

        for (var listener : listeners) {
            if (listener instanceof LinkedWebHistorySyncProgressListener typed) {
                Thread.startVirtualThread(() -> typed.onWebHistorySyncProgress(whatsapp, progressValue, recent));
            }
        }

        var historyChats = historySync.chats();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "dispatching history sync chunk: type={0} progress={1} chats={2}",
                    syncType, progressValue, historyChats.size());
        }
        var createdRegularUserChats = 0L;
        for (var historyChat : historyChats) {
            if (applyChat(historyChat, store) && isRegularUserChat(historyChat.jid())) {
                createdRegularUserChats++;
            }
            emitGroupHistoryReceiverJourney(historyChat);
        }
        emitWebcChatCreate(createdRegularUserChats);
        if (!historyChats.isEmpty()) {
            List<Chat> syncedChats = List.copyOf(historyChats);
            for (var listener : listeners) {
                if (listener instanceof LinkedWebHistorySyncMessagesListener typed) {
                    Thread.startVirtualThread(() -> typed.onWebHistorySyncMessages(whatsapp, syncedChats, isLastChunk));
                }
            }
        }

        for (var pushname : historySync.pushnames()) {
            applyPushname(pushname, store);
        }

        for (var statusMessage : historySync.statusV3Messages()) {
            store.chatStore().addStatus(statusMessage);
            for (var listener : listeners) {
                if (listener instanceof LinkedNewStatusListener typed) {
                    Thread.startVirtualThread(() -> typed.onNewStatus(whatsapp, statusMessage));
                }
            }
        }

        for (var callLog : historySync.callLogRecords()) {
            if (callLog.callId().isPresent()) {
                store.chatStore().addCallLog(callLog);
            }
        }

        for (var sticker : historySync.recentStickers()) {
            applyRecentSticker(sticker, store);
        }

        historySync.companionMmsAuthNonce()
                .ifPresent(store.accountStore()::setCompanionMmsAuthNonce);

        historySync.shareableChatLinkKey()
                .ifPresent(store.accountStore()::setShareableChatLinkKey);

        historySync.chatThreadLoggingSecret()
                .ifPresent(store.wamStore()::setChatThreadLoggingSecret);

        historySync.chatThreadLoggingDisappearingOffset()
                .ifPresent(offset -> store.wamStore().setChatThreadLoggingOffset(offset));

        for (var pastParticipants : historySync.pastParticipants()) {
            var groupJid = pastParticipants.groupJid().orElse(null);
            if (groupJid == null) {
                continue;
            }
            var participants = pastParticipants.pastParticipants();
            for (var listener : listeners) {
                if (listener instanceof LinkedWebHistorySyncPastParticipantsListener typed) {
                    Thread.startVirtualThread(() -> typed.onWebHistorySyncPastParticipants(whatsapp, groupJid, participants));
                }
            }
        }

        if (syncType == HistorySyncType.INITIAL_BOOTSTRAP && !store.syncStore().syncedChats()) {
            store.syncStore().setSyncedChats(true);
            var chats = store.chatStore().chats();
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "initial chat history sync complete: {0} chats", chats.size());
            }
            for (var listener : listeners) {
                if (listener instanceof LinkedChatsListener typed) {
                    Thread.startVirtualThread(() -> typed.onChats(whatsapp, chats));
                }
            }
        }

        if (syncType == HistorySyncType.PUSH_NAME && !store.syncStore().syncedContacts()) {
            store.syncStore().setSyncedContacts(true);
            var contacts = store.contactStore().contacts();
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "initial contact history sync complete: {0} contacts", contacts.size());
            }
            for (var listener : listeners) {
                if (listener instanceof LinkedContactsListener typed) {
                    Thread.startVirtualThread(() -> typed.onContacts(whatsapp, contacts));
                }
            }
        }

        if (syncType == HistorySyncType.INITIAL_STATUS_V3 && !store.syncStore().syncedStatus()) {
            store.syncStore().setSyncedStatus(true);
            var status = store.chatStore().status();
            if (Log.INFO) {
                LOGGER.log(Level.INFO, "initial status history sync complete: {0} statuses", status.size());
            }
            for (var listener : listeners) {
                if (listener instanceof LinkedStatusListener typed) {
                    Thread.startVirtualThread(() -> typed.onStatus(whatsapp, status));
                }
            }
        }
    }

    /**
     * Commits a
     * {@link GroupHistoryReceiverUserJourneyEventBuilder GroupHistoryReceiverUserJourneyEvent}
     * recording that one group's embedded message history was inserted into the
     * store from the current history-sync chunk.
     *
     * <p>Invoked from {@link #dispatch(HistorySync)} once per chat carried by
     * the chunk. WhatsApp Web tracks the group-history receiver journey through
     * a dedicated on-demand bundle download
     * ({@code WAWebDownloadHistoryBundleAction}) whose stages are download
     * started, download succeeded or failed, parse succeeded or failed, and
     * database inserted. Cobalt receives group history inline with the
     * bootstrap history-sync chunks rather than through a separate per-group
     * download, so only the terminal database-insert stage
     * ({@link GroupHistoryReceiverUserJourneyActionType#GROUP_HISTORY_DB_INSERTED})
     * has a faithful analogue here; it fires as the group's messages are merged
     * into the local {@link Chat}. Non-group chats and groups that carry no
     * embedded messages in this chunk are skipped so the event is emitted only
     * when history is actually inserted.
     *
     * @implNote This implementation reports {@code groupHistoryDbIgnoredOlderMessages}
     * as {@code false} and {@code groupHistoryOutWindowPinsCount} as {@code 0}
     * because Cobalt appends every message the chunk carries without an
     * age-based drop and applies no pin-retention-window filter. Matching
     * WhatsApp Web's own field, {@code userJourneyMs} is the current wall-clock
     * epoch millisecond rather than a duration. A fresh {@code unifiedSessionId}
     * is minted per emission with {@link UUID#randomUUID()} because Cobalt has no
     * cross-event unified-session tracker.
     *
     * @param historyChat the wire-shape chat carrying the group's chunk
     *                    metadata and embedded messages
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupHistoryReceiverUserJourneyLogger",
            exports = "dbInserted", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitGroupHistoryReceiverJourney(Chat historyChat) {
        if (!historyChat.jid().hasServer(JidServer.groupOrCommunity())) {
            return;
        }
        var messagesCount = historyChat.messageCount();
        if (messagesCount == 0) {
            return;
        }
        wamService.commit(new GroupHistoryReceiverUserJourneyEventBuilder()
                .groupHistoryReceiverActionType(GroupHistoryReceiverUserJourneyActionType.GROUP_HISTORY_DB_INSERTED)
                .groupHistoryReceiverGroupId(historyChat.jid().toString())
                .groupHistoryMessagesCount(messagesCount)
                .groupHistoryDbIgnoredOlderMessages(false)
                .groupHistoryOutWindowPinsCount(0)
                .userJourneyMs(System.currentTimeMillis())
                .unifiedSessionId(UUID.randomUUID().toString())
                .build());
    }

    /**
     * Commits a {@link WebcChatCreateEventBuilder WebcChatCreateEvent} recording
     * how many regular-user chats were implicitly created while ingesting the
     * current history-sync chunk.
     *
     * <p>Invoked from {@link #dispatch(HistorySync)} once per chunk, after every
     * carried conversation has been merged through
     * {@link #applyChat(Chat, LinkedWhatsAppStore)}. The event is emitted only
     * when at least one regular-user chat was newly created, mirroring
     * WhatsApp Web, which commits the metric solely on the {@code noCreated > 0}
     * branch. The creation method is always
     * {@link WebcChatCreateCreationMethod#MISSING_WHEN_SAVING_MESSAGE} because the
     * chats are materialised as a side effect of persisting the chunk's embedded
     * messages for conversations that had no existing chat row.
     *
     * @implNote This implementation counts only chats whose {@link Chat#jid()}
     * passes {@link #isRegularUserChat(Jid)}, matching WhatsApp Web's
     * {@code isRegularUserNoImply} filter that excludes groups, newsletters,
     * broadcasts, the announcements (PSA) account, and bots from the count.
     * WhatsApp Web derives the count by diffing the referenced chat ids against
     * the rows returned by a bulk chat lookup; Cobalt instead accumulates the
     * per-chat creation flag returned by
     * {@link #applyChat(Chat, LinkedWhatsAppStore)} across the dispatch loop,
     * which naturally deduplicates repeated ids because a second occurrence finds
     * the just-created chat.
     *
     * @param createdCount the number of regular-user chats created during the
     *                     current chunk; no event is emitted when it is not
     *                     positive
     */
    @WhatsAppWebExport(moduleName = "WAWebDBMessageBulkHelper",
            exports = "persistNewMessagesInBulk", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitWebcChatCreate(long createdCount) {
        if (createdCount <= 0) {
            return;
        }
        wamService.commit(new WebcChatCreateEventBuilder()
                .creationMethod(WebcChatCreateCreationMethod.MISSING_WHEN_SAVING_MESSAGE)
                .noCreated(createdCount)
                .build());
    }

    /**
     * Returns whether a chat JID identifies a regular WhatsApp user for the
     * purpose of the {@link #emitWebcChatCreate(long)} creation count.
     *
     * <p>Invoked from {@link #dispatch(HistorySync)} once per newly created chat.
     * A JID is a regular user when it lives on a user domain
     * ({@code s.whatsapp.net} or the legacy {@code c.us}) or the Linked Identity
     * domain ({@code lid}), is not the announcements (PSA) account
     * ({@code 0@s.whatsapp.net}), and is not a bot. Groups, communities,
     * broadcasts, newsletters, and every other addressable entity are excluded.
     *
     * @implNote This implementation mirrors WhatsApp Web's
     * {@code isRegularUserNoImply} ({@code isUser && !isPSA && !isBot}); the
     * user-or-LID domain check maps onto {@link Jid#hasUserServer()} and
     * {@link Jid#hasLidServer()}, the PSA exclusion onto the {@code "0"} user
     * check, and the bot exclusion onto {@link Jid#isBot()}, which already folds
     * in both the {@code bot} domain and the reserved phone-number bot ranges.
     *
     * @param jid the chat JID to classify
     * @return {@code true} when the JID identifies a regular user, {@code false}
     *         otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isRegularUserNoImply",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isRegularUserChat(Jid jid) {
        if (jid.isBot()) {
            return false;
        }
        if (jid.hasUserServer()) {
            return !"0".equals(jid.user());
        }
        return jid.hasLidServer();
    }

    /**
     * Folds one history-sync {@link StickerMetadata} entry into the local
     * recent-stickers collection.
     *
     * <p>Invoked from {@link #dispatch} once per recent-sticker record carried
     * by the chunk.
     *
     * @implNote This implementation keys the row on the standard base64
     * encoding of {@link StickerMetadata#fileSha256()} and drops the entry
     * outright when that digest is absent. The resulting
     * {@link com.github.auties00.cobalt.wire.linked.preference.Sticker} is built with
     * a non-favorite flag and no device-id hint because neither field is
     * carried by the history-sync wire shape; there is no post-batch
     * user-prefs status write because Cobalt has no equivalent surface.
     *
     * @param sticker the wire-shape sticker record
     * @param store   the store to mutate
     */
    @WhatsAppWebExport(moduleName = "WAWebHistorySyncStickers",
            exports = "processRecentStickers",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static void applyRecentSticker(StickerMetadata sticker, LinkedWhatsAppStore store) {
        var fileSha256 = sticker.fileSha256().orElse(null);
        if (fileSha256 == null) {
            return;
        }
        var stickerHash = Base64.getEncoder().encodeToString(fileSha256);
        var lastSent = sticker.lastStickerSentTs()
                .map(Instant::getEpochSecond)
                .orElse(null);
        var built = new StickerBuilder()
                .mediaUrl(sticker.url().orElse(null))
                .mediaEncryptedSha256(sticker.fileEncSha256().orElse(null))
                .mediaKey(sticker.mediaKey().orElse(null))
                .mimetype(sticker.mimetype().orElse(null))
                .height(sticker.height().isPresent() ? sticker.height().getAsInt() : null)
                .width(sticker.width().isPresent() ? sticker.width().getAsInt() : null)
                .mediaDirectPath(sticker.directPath().orElse(null))
                .mediaSize(sticker.fileLength().isPresent() ? sticker.fileLength().getAsLong() : null)
                .favorite(false)
                .timestamp(lastSent)
                .isAvatar(sticker.isAvatarSticker())
                .build();
        store.settingsStore().addRecentSticker(stickerHash, built);
    }

    /**
     * Merges one wire-shape history-sync chat into the local store and appends
     * its embedded messages to the corresponding store-side {@link Chat}.
     *
     * <p>Invoked from {@link #dispatch} once per chat carried by the chunk; the
     * returned creation flag feeds the per-batch {@link #emitWebcChatCreate(long)}
     * count.
     *
     * @implNote This implementation looks up the existing store chat keyed by
     * {@link Chat#jid()} and creates a fresh record via
     * {@link LinkedWhatsAppChatStore#addNewChat(Jid)} only when none exists; the
     * wire-shape chat is never inserted directly because the store is typed
     * against its concrete chat subclasses, none of which is the history-sync
     * variant. Metadata fields are transcribed field-by-field through
     * {@link #copyChatMetadata(Chat, Chat)}, preserving any local edits the
     * store already holds, and the embedded messages from the chunk are
     * appended to the local chat via
     * {@link Chat#addMessage(com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo)}.
     *
     * @param historyChat the wire-shape chat carrying chunk metadata and
     *                    embedded messages
     * @param store       the store to mutate
     * @return {@code true} when no store chat existed for the wire-shape chat's
     *         {@link Chat#jid()} and a fresh record was created, {@code false}
     *         when an existing chat was merged into
     */
    private static boolean applyChat(Chat historyChat, LinkedWhatsAppStore store) {
        var jid = historyChat.jid();
        var existing = store.chatStore().findChatByJid(jid);
        var localChat = existing.orElseGet(() -> store.chatStore().addNewChat(jid));
        if (localChat != historyChat) {
            copyChatMetadata(historyChat, localChat);
        }
        historyChat.messages().forEach(localChat::addMessage);
        return existing.isEmpty();
    }

    /**
     * Copies every present chat-metadata field from {@code from} to
     * {@code to}.
     *
     * <p>Invoked from {@link #applyChat(Chat, LinkedWhatsAppStore)} to transcribe
     * the chunk's chat metadata onto the store-resident chat.
     *
     * @implNote This implementation only touches the target chat when the
     * source carries a present value, so a partial chunk does not silently
     * null-out a field that a previous chunk or an app-state mutation has
     * already populated. The merge is best-effort; fields with no setter on
     * {@link Chat} (computed properties such as {@link Chat#messageCount()})
     * are skipped.
     *
     * @param from the wire-shape source chat
     * @param to   the store-resident target chat
     */
    private static void copyChatMetadata(Chat from, Chat to) {
        from.newJid().ifPresent(to::setNewJid);
        from.oldJid().ifPresent(to::setOldJid);
        from.lastMsgTimestamp().ifPresent(to::setLastMsgTimestamp);
        from.unreadCount().ifPresent(to::setUnreadCount);
        to.setReadOnly(from.readOnly());
        to.setEndOfHistoryTransfer(from.endOfHistoryTransfer());
        from.ephemeralExpiration().ifPresent(to::setEphemeralExpiration);
        from.ephemeralSettingTimestamp().ifPresent(to::setEphemeralSettingTimestamp);
        from.endOfHistoryTransferType().ifPresent(to::setEndOfHistoryTransferType);
        from.conversationTimestamp().ifPresent(to::setConversationTimestamp);
        from.name().ifPresent(to::setName);
        from.pHash().ifPresent(to::setPHash);
        to.setNotSpam(from.notSpam());
        to.setArchived(from.archived());
        from.disappearingMode().ifPresent(to::setDisappearingMode);
        from.unreadMentionCount().ifPresent(to::setUnreadMentionCount);
        to.setMarkedAsUnread(from.markedAsUnread());
        if (!from.participant().isEmpty()) {
            to.setParticipant(from.participant());
        }
        from.tcToken().ifPresent(to::setTcToken);
        from.tcTokenTimestamp().ifPresent(to::setTcTokenTimestamp);
        from.contactPrimaryIdentityKey().ifPresent(to::setContactPrimaryIdentityKey);
        from.pinnedTimestamp().ifPresent(to::setPinnedTimestamp);
        from.mute().ifPresent(to::setMute);
        from.wallpaper().ifPresent(to::setWallpaper);
        from.mediaVisibility().ifPresent(to::setMediaVisibility);
        from.tcTokenSenderTimestamp().ifPresent(to::setTcTokenSenderTimestamp);
        to.setSuspended(from.suspended());
        to.setTerminated(from.terminated());
        from.createdAt().ifPresent(to::setCreatedAt);
        from.createdBy().ifPresent(to::setCreatedBy);
        from.description().ifPresent(to::setDescription);
        to.setSupport(from.support());
        to.setParentGroup(from.isParentGroup());
        from.parentGroupId().ifPresent(to::setParentGroupId);
        to.setDefaultSubgroup(from.isDefaultSubgroup());
        from.displayName().ifPresent(to::setDisplayName);
        from.phoneNumberJid().ifPresent(to::setPhoneNumberJid);
        to.setShareOwnPhoneNumber(from.shareOwnPhoneNumber());
        to.setPhoneNumberDuplicateLidThread(from.phoneNumberhDuplicateLidThread());
        from.lid().ifPresent(to::setLid);
        from.username().ifPresent(to::setUsername);
        from.lidOriginType().ifPresent(to::setLidOriginType);
        from.commentsCount().ifPresent(to::setCommentsCount);
        to.setLocked(from.locked());
        from.systemMessageToInsert().ifPresent(to::setSystemMessageToInsert);
        to.setCapiCreatedGroup(from.capiCreatedGroup());
        from.accountLid().ifPresent(to::setAccountLid);
        to.setLimitSharing(from.limitSharing());
        from.limitSharingSettingTimestamp().ifPresent(to::setLimitSharingSettingTimestamp);
        from.limitSharingTrigger().ifPresent(to::setLimitSharingTrigger);
        to.setLimitSharingInitiatedByMe(from.limitSharingInitiatedByMe());
        to.setMaibaAiThreadEnabled(from.maibaAiThreadEnabled());
    }

    /**
     * Updates the local contact for one history-sync push-name record.
     *
     * <p>Invoked from {@link #dispatch} once per pushname carried by the
     * chunk.
     *
     * @implNote This implementation parses the wire-string identifier through
     * {@link Jid#of(String)} and silently drops entries whose id is
     * unparseable. A new contact is created via
     * {@link LinkedWhatsAppContactStore#addNewContact(Jid)} when no entry exists.
     * Empty-string push-names are skipped rather than written.
     *
     * @param pushname the wire-shape pushname record
     * @param store    the store to mutate
     */
    private static void applyPushname(HistorySync.Pushname pushname, LinkedWhatsAppStore store) {
        var rawId = pushname.id().orElse(null);
        if (rawId == null || rawId.isEmpty()) {
            return;
        }
        Jid jid;
        try {
            jid = Jid.of(rawId);
        } catch (IllegalArgumentException _) {
            return;
        }
        if (jid == null) {
            return;
        }
        var name = pushname.pushname().orElse(null);
        if (name == null || name.isEmpty()) {
            return;
        }
        var contact = store.contactStore().findContactByJid(jid)
                .orElseGet(() -> store.contactStore().addNewContact(jid));
        contact.setChosenName(name);
    }
}
