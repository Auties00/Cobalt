package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.listener.linked.LinkedWebAppStateActionListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEntry;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEntryBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.mutation.OrphanMutationEntry;
import com.github.auties00.cobalt.wire.linked.sync.mutation.OrphanMutationEntryBuilder;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.sync.key.LiveMissingSyncKeyRequestService;
import com.github.auties00.cobalt.sync.key.LiveSyncKeyRotationService;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.media.MediaConnectionService;
import com.github.auties00.cobalt.message.send.id.MessageIdGenerator;
import com.github.auties00.cobalt.message.send.id.MessageIdVersion;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReference;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateFatalExceptionNotificationBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyData;
import com.github.auties00.cobalt.wire.linked.signal.KeyId;
import com.github.auties00.cobalt.wire.linked.sync.*;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ClearChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MarkChatAsReadAction;
import com.github.auties00.cobalt.wire.linked.sync.data.*;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.crypto.MutationIntegrityVerifier;
import com.github.auties00.cobalt.sync.crypto.MutationKeys;
import com.github.auties00.cobalt.sync.crypto.MutationLTHash;
import com.github.auties00.cobalt.sync.exchange.MutationRequestBuilder;
import com.github.auties00.cobalt.sync.exchange.MutationResponseParser;
import com.github.auties00.cobalt.sync.exchange.MutationSyncResponse;
import com.github.auties00.cobalt.sync.exchange.SyncRequest;
import com.github.auties00.cobalt.sync.key.MissingSyncKeyRequestService;
import com.github.auties00.cobalt.sync.key.MissingSyncKeyTimeoutScheduler;
import com.github.auties00.cobalt.sync.key.SyncKeyRotationService;
import com.github.auties00.cobalt.sync.handler.SyncdIndexUtils;
import com.github.auties00.cobalt.util.BufferedProtobufInputStream;
import com.github.auties00.cobalt.util.ScheduledTask;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MdAppStateMessageRangeEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdAppStateSyncMutationStatsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SyncdKeyCountEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdBootstrapAppStateCriticalDataProcessingEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdBootstrapAppStateDataDownloadedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdBootstrapDataAppliedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdAppStateSyncDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdSyncdBundleEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdSyncdMutationEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdSyncdMutationsSummaryEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MediaDownload2EventBuilder;
import com.github.auties00.cobalt.wire.wam.type.BootstrapAppStateDataStageCode;
import com.github.auties00.cobalt.wire.wam.type.DownloadOriginType;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapPayloadType;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapSource;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapStepResult;
import com.github.auties00.cobalt.wire.wam.type.MediaDownloadModeType;
import com.github.auties00.cobalt.wire.wam.type.MediaDownloadResultType;
import com.github.auties00.cobalt.wire.wam.type.KmpSyncdFlowEnum;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MutationBundleType;
import com.github.auties00.cobalt.wire.wam.type.MutationCountBucket;
import com.github.auties00.cobalt.wire.wam.type.MutationDirectionType;
import com.github.auties00.cobalt.wire.wam.type.MutationOperationType;
import com.github.auties00.cobalt.wire.wam.type.SyncdCollectionType;

import javax.crypto.Mac;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.lang.System.Logger.Level;


/**
 * Coordinates the WhatsApp Web app-state synchronization pipeline.
 *
 * <p>App state is the cross-device replicated key-value store that backs
 * settings such as chat archive/pin/mute flags, label assignments,
 * starred messages, and account-level preferences. Each logical collection
 * ({@link SyncPatchType#CRITICAL_BLOCK}, {@link SyncPatchType#CRITICAL_UNBLOCK_LOW},
 * {@link SyncPatchType#REGULAR}, {@link SyncPatchType#REGULAR_HIGH},
 * {@link SyncPatchType#REGULAR_LOW}) has its own version, LT-Hash, and
 * sync-state finite state machine; this class drives all of them.
 *
 * <p>The two top-level entry points are {@link #pushPatches} (outgoing
 * local mutations) and {@link #pullPatches} (incoming server patches and
 * snapshots). Both feed the same per-collection state machine: collections
 * are marked dirty, batched into a single IQ when possible, sent through
 * the {@link MutationRequestBuilder}, parsed back via
 * {@link MutationResponseParser}, mutations are decrypted and integrity
 * checked through {@link MutationIntegrityVerifier}, and resulting sync
 * actions are dispatched to the per-action handlers held in
 * {@link WebAppStateHandlerRegistry}. Outgoing uploads update the local
 * LT-Hash by replaying the same crypto stack against the acknowledged
 * mutations.
 *
 * <p>Sync key material is rotated via
 * {@link SyncKeyRotationService}. When mutations reference a key the
 * companion does not yet hold, the collection is parked in
 * {@link SyncCollectionState#BLOCKED} and
 * {@link MissingSyncKeyRequestService} requests the missing keys from the
 * primary device, with {@link MissingSyncKeyTimeoutScheduler} retrying on
 * a timer. Snapshot MAC mismatches trigger a peer recovery handshake via
 * {@link SnapshotRecoveryService}.
 *
 * <p>Per-collection {@code MdSyncdMutation}, {@code SyncdKeyCount}, and
 * {@code MdAppStateSyncMutationStats} telemetry is committed through
 * {@link WamService}; recurring stats jobs are managed by
 * {@link #startPeriodicReportSyncdStatsJob()} and
 * {@link #startPeriodicReportSyncdKeyStatsJob()}.
 *
 * <p>Most callers reach this service indirectly via
 * {@link LinkedWhatsAppClient#pushWebAppState} and
 * {@link LinkedWhatsAppClient#pullWebAppState}. Direct usage is reserved for
 * components that participate in the sync key share lifecycle, notably the
 * protocol-message receiver that dispatches incoming
 * {@code AppStateSyncKeyShare} messages, which calls
 * {@link #syncKeyRotationService()}.
 *
 * @implNote
 * This implementation collapses WA Web's twelve-IndexedDB schema into the
 * in-memory {@link LinkedWhatsAppStore}, so the {@code loadStatesFromDb} /
 * {@code persistToDb} cycle WA Web performs around every sync round
 * becomes a no-op. State is recovered after a JVM restart by
 * {@link #resumeAfterRestart()} from whatever the store happens to hold
 * at startup, which means in-flight sync rounds that crashed mid-flight
 * resume from the last persisted collection version on the next
 * {@link #pullPatches} call.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncd")
@WhatsAppWebModule(moduleName = "WAWebSyncdCollectionHandlerTypesConverter")
@WhatsAppWebModule(moduleName = "WAWebSyncdCollectionUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdDbCallbacksApi")
@WhatsAppWebModule(moduleName = "WAWebSyncdMMSDownload")
@WhatsAppWebModule(moduleName = "WAWebSyncdNetCallbacksApi")
@WhatsAppWebModule(moduleName = "WAWebGetCollectionVersion")
@WhatsAppWebModule(moduleName = "WAWebGetSyncAction")
@WhatsAppWebModule(moduleName = "WAWebSyncdBridgeApi")
@WhatsAppWebModule(moduleName = "WAWebSyncdCollectionsStateMachine")
@WhatsAppWebModule(moduleName = "WAWebSyncdDisabled")
@WhatsAppWebModule(moduleName = "WAWebSyncdFatal")
@WhatsAppWebModule(moduleName = "WAWebSyncdFatalExceptionNotificationApi")
@WhatsAppWebModule(moduleName = "WAWebSyncdServerSync")
@WhatsAppWebModule(moduleName = "WAWebSyncdReportSyncdStatJob")
@WhatsAppWebModule(moduleName = "WAWebSyncdWamUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdWamReportingUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdWamAppState")
public final class LiveWebAppStateService implements WebAppStateService {
    /**
     * The logger for {@link LiveWebAppStateService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveWebAppStateService.class);

    /**
     * The debounce window after the last mutation-processing activity before
     * the accumulated
     * {@link MdAppStateSyncDailyEventBuilder MdAppStateSyncDaily} aggregate is
     * committed.
     *
     * <p>Mirrors WhatsApp Web's five-minute {@code ShiftTimer} window, which
     * shifts forward on every counter update so the single daily aggregate is
     * flushed once the collection has quiesced rather than on a fixed clock.
     */
    private static final Duration APP_STATE_SYNC_DAILY_FLUSH_DELAY = Duration.ofMinutes(5);

    /**
     * The {@link LinkedWhatsAppClient} used for store access, stanza dispatch, and
     * peer-message routing during snapshot recovery.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The cached reference to {@link LinkedWhatsAppClient#store()}.
     *
     * @implNote
     * This implementation holds the store as a field rather than
     * dereferencing it on every call so that hot-path methods (LT-Hash
     * recomputation, version reads) do not pay for the indirection on
     * every mutation.
     */
    private final LinkedWhatsAppStore store;

    /**
     * The builder that constructs outgoing sync IQ nodes and encrypts
     * pending mutations.
     */
    private final MutationRequestBuilder requestBuilder;

    /**
     * The parser that converts incoming sync IQ responses into
     * {@link MutationSyncResponse} records.
     */
    private final MutationResponseParser responseParser;

    /**
     * The verifier that checks snapshot and patch MACs against the local
     * LT-Hash and value-MAC list.
     */
    private final MutationIntegrityVerifier integrityVerifier;

    /**
     * The registry mapping each sync action name to its registered
     * handler.
     */
    private final WebAppStateHandlerRegistry handlerRegistry;

    /**
     * The exponential-backoff scheduler driving retries for failed sync
     * rounds.
     */
    private final WebAppStateBackoffScheduler retryScheduler;

    /**
     * The scheduler enforcing timeout deadlines on outstanding missing
     * sync key requests.
     */
    private final MissingSyncKeyTimeoutScheduler missingSyncKeyTimeoutScheduler;

    /**
     * The service that dispatches peer messages requesting missing sync
     * keys from the primary device.
     */
    private final MissingSyncKeyRequestService missingSyncKeyRequestService;

    /**
     * The service that rotates sync keys and handles incoming
     * {@code AppStateSyncKeyShare} payloads.
     */
    private final SyncKeyRotationService syncKeyRotationService;

    /**
     * The provider of A/B-tested configuration values consulted on
     * feature-gated paths.
     */
    private final ABPropsService abPropsService;

    /**
     * The service that drives the peer-recovery handshake when a
     * snapshot MAC validation fails fatally.
     */
    private final SnapshotRecoveryService snapshotRecoveryService;

    /**
     * The {@link WamService} used to commit app-state sync telemetry
     * events ({@code MdSyncdMutation}, {@code MdAppStateSyncMutationStats},
     * {@code SyncdKeyCount}, bootstrap pipeline events).
     */
    private final WamService wamService;

    /**
     * The shared {@link MediaConnectionService}
     * used to upload external app-state patches and download external
     * snapshot blobs.
     */
    private final MediaConnectionService mediaConnectionService;

    /**
     * The handle of the currently scheduled periodic sync job, or
     * {@code null} when no job is scheduled.
     */
    private volatile ScheduledTask periodicSyncJob;

    /**
     * The handle of the currently scheduled daily syncd stats reporting
     * job, or {@code null} when no job is scheduled.
     */
    private volatile ScheduledTask periodicReportSyncdStatsJob;

    /**
     * The handle of the currently scheduled daily syncd key stats
     * reporting job, or {@code null} when no job is scheduled.
     */
    private volatile ScheduledTask periodicReportSyncdKeyStatsJob;

    /**
     * The handle of the pending debounced flush that ships the accumulated
     * {@link MdAppStateSyncDailyEventBuilder MdAppStateSyncDaily} aggregate, or
     * {@code null} when no flush is armed.
     *
     * <p>Rescheduled on every mutation-processing activity so the aggregate is
     * emitted on a shifting window that settles after the collection has been
     * idle for {@link #APP_STATE_SYNC_DAILY_FLUSH_DELAY}.
     */
    private volatile ScheduledTask appStateSyncDailyFlushTask;

    /**
     * The live accumulator behind the
     * {@link MdAppStateSyncDailyEventBuilder MdAppStateSyncDaily} aggregate.
     *
     * <p>Fed from the mutation-processing chokepoints
     * ({@link #applyMutations}, {@link #recordMutationState}, {@link #decryptMutations},
     * {@link #processUploadSuccess}) and drained by {@link #flushAppStateSyncDaily()}.
     */
    private final SyncdDailyStats syncdDailyStats;

    /**
     * The fabricated per-connection application session identifier reported on
     * the syncd bundle and mutation-summary telemetry.
     *
     * <p>WhatsApp Web sources this from {@code getSharedSessionId()}, a value
     * scoped to the browser page session; Cobalt has no page, so a stable
     * random identifier is minted once per service instance and reused across
     * the connection.
     */
    private final String wamAppSessionId;

    /**
     * The fabricated companion session identifier reported on the syncd bundle
     * and mutation-summary telemetry.
     *
     * <p>WhatsApp Web sources this from a six-character SHA-1 slice of the
     * multi-device session identifier; Cobalt has no such derivation, so a
     * stable six-character random identifier is minted once per service
     * instance.
     */
    private final String wamCompanionSessionId;

    /**
     * Serializes every syncd state mutation and tracks which collections are
     * mid-round.
     *
     * <p>A pull (driven by an incoming server-sync notification), a push (driven
     * by a local mutation), an inbound key share, a key rotation, a missing-key
     * timeout decision, and an orphan replay all read and write the same syncd
     * state. The coordinator runs each synchronous state segment under a single
     * reentrant monitor and is released between segments, so a blocking
     * {@code sendNode}, media transfer, or peer-reply wait never holds the lock;
     * its in-flight set excludes a collection that is already mid-round from a
     * concurrent round and re-triggers it afterward, so a collection's version
     * and LT-Hash cannot be touched by two rounds at once even across the
     * unlocked I/O gaps.
     *
     * @implNote
     * This implementation is the blocking-virtual-thread translation of WA Web's
     * {@code WAWebSyncd} serial queue plus its in-flight set (the {@code A} set in
     * its {@code triggerSync} driver). The whole subsystem ({@link #whatsapp},
     * the key rotation service, the missing-key request service, and the
     * missing-key timeout scheduler) shares this one coordinator so every state
     * mutation serializes against the same monitor.
     */
    private final SyncdCoordinator coordinator;

    /**
     * Constructs a new app-state service wired against the supplied
     * client, props provider, LID migration service, recovery service,
     * and telemetry surface.
     *
     * <p>The service is constructed by {@link LinkedWhatsAppClient} during connect.
     *
     * @implNote
     * This implementation constructs all collaborators eagerly
     * ({@link MutationRequestBuilder}, {@link MutationResponseParser},
     * {@link WebAppStateHandlerRegistry}, {@link MutationIntegrityVerifier},
     * {@link WebAppStateBackoffScheduler}, {@link MissingSyncKeyRequestService},
     * {@link MissingSyncKeyTimeoutScheduler}, {@link SyncKeyRotationService})
     * and stores them as final fields, so the wiring graph is fixed at
     * construction time. The {@link MissingSyncKeyRequestService} and the
     * {@link MissingSyncKeyTimeoutScheduler} have a mutual reference;
     * the timeout scheduler is registered on the request service after
     * both are built to break the cycle.
     *
     * @param whatsapp                the {@link LinkedWhatsAppClient} owning the
     *                                store, the socket, and the peer
     *                                message router
     * @param abPropsService          the {@link ABPropsService} consulted
     *                                for feature-gated branches
     * @param lidMigrationService     the {@link LidMigrationService}
     *                                injected into the handler registry
     *                                so the device-capabilities handler
     *                                can observe 1:1 LID migration
     *                                progress
     * @param snapshotRecoveryService the {@link SnapshotRecoveryService}
     *                                driving peer-recovery when a
     *                                snapshot MAC fails fatally
     * @param wamService              the {@link WamService} that commits
     *                                app-state sync telemetry events
     * @param mediaConnectionService  the {@link MediaConnectionService} used
     *                                to upload external app-state patches and
     *                                download external snapshot blobs
     */
    public LiveWebAppStateService(LinkedWhatsAppClient whatsapp, ABPropsService abPropsService, LidMigrationService lidMigrationService, SnapshotRecoveryService snapshotRecoveryService, WamService wamService, MediaConnectionService mediaConnectionService) {
        this.whatsapp = whatsapp;
        this.store = whatsapp.store();
        this.abPropsService = abPropsService;
        this.wamService = wamService;
        this.mediaConnectionService = Objects.requireNonNull(mediaConnectionService, "mediaConnectionService cannot be null");
        this.coordinator = new SyncdCoordinator();
        this.requestBuilder = new MutationRequestBuilder(whatsapp, abPropsService, wamService, mediaConnectionService);
        this.responseParser = new MutationResponseParser();
        this.handlerRegistry = new WebAppStateHandlerRegistry(abPropsService, lidMigrationService, wamService);
        this.integrityVerifier = new MutationIntegrityVerifier(store);
        this.retryScheduler = new WebAppStateBackoffScheduler();
        this.missingSyncKeyRequestService = new LiveMissingSyncKeyRequestService(whatsapp, wamService, coordinator);
        this.missingSyncKeyTimeoutScheduler = new MissingSyncKeyTimeoutScheduler(whatsapp, abPropsService, missingSyncKeyRequestService, coordinator);
        this.missingSyncKeyRequestService.setTimeoutScheduler(missingSyncKeyTimeoutScheduler);
        this.syncKeyRotationService = new LiveSyncKeyRotationService(whatsapp, this, abPropsService, wamService, coordinator);
        this.snapshotRecoveryService = snapshotRecoveryService;
        this.syncdDailyStats = new SyncdDailyStats();
        var sessionRandom = new SecureRandom();
        var appSessionBytes = new byte[12];
        sessionRandom.nextBytes(appSessionBytes);
        this.wamAppSessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(appSessionBytes);
        var companionSessionBytes = new byte[6];
        sessionRandom.nextBytes(companionSessionBytes);
        this.wamCompanionSessionId = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(companionSessionBytes)
                .substring(0, 6);
    }

    /**
     * Enqueues local mutations for the given collection and synchronously
     * drives a sync round to push them to the server.
     *
     * <p>Triggered whenever a local action produces an app-state change
     * (archiving a chat, pinning a message, starring a contact, applying a
     * label, toggling settings). Callers normally reach this through
     * {@link LinkedWhatsAppClient#pushWebAppState}.
     *
     * @implNote
     * This implementation runs the sync inline on the calling virtual
     * thread rather than via a debounce, so mutations are visible to the
     * server by the time the call returns; Cobalt's fire-and-forget API
     * surface relies on that.
     *
     * @param patchType the collection these mutations belong to
     * @param patches   the pending mutations to enqueue
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "markCollectionsForSync", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void pushPatches(SyncPatchType patchType, SequencedCollection<SyncPendingMutation> patches) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pushing {0} pending mutations for collection {1}", patches.size(), patchType);
        syncKeyRotationService.ensureActiveKey(true);
        coordinator.runLocked(() -> {
            store.syncStore().markWebAppStateDirty(patchType);
            whatsapp.store().syncStore().addPendingMutations(patchType, patches);
        });
        syncCollection(patchType);
    }

    /**
     * Drives a batched sync round for the given collections and reports
     * whether any response carried real state changes.
     *
     * <p>Called whenever the server announces that one or more collections
     * may have new data, typically through a dirty-bit notification. The
     * return value lets the caller detect false-positive dirty bits when no
     * collection response carried a patch or a snapshot.
     *
     * @param patchTypes the collection types to sync; empty causes a
     *                   no-op {@code false} return
     * @return {@code true} if at least one collection response carried a
     *         patch or a snapshot reference; {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "markCollectionsForSync", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public boolean pullPatches(SyncPatchType... patchTypes) {
        var allCollections = new LinkedHashSet<SyncPatchType>();
        Collections.addAll(allCollections, patchTypes);
        if (!allCollections.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pulling patches for {0} collections", allCollections.size());
            return syncCollectionsBatched(allCollections);
        }
        return false;
    }

    /**
     * Returns the {@link SyncKeyRotationService} owned by this service.
     *
     * <p>Exposed so the protocol-message receiver that dispatches incoming
     * {@code AppStateSyncKeyShare} payloads can hand them off to the rotation
     * service without a separately injected reference.
     *
     * @return the {@link SyncKeyRotationService} instance
     */
    @Override
    public SyncKeyRotationService syncKeyRotationService() {
        return syncKeyRotationService;
    }

    /**
     * Unblocks every collection parked in {@link SyncCollectionState#BLOCKED}
     * and drives a fresh sync round for them.
     *
     * <p>Invoked from {@link SyncKeyRotationService} after a successful
     * {@code AppStateSyncKeyShare} delivery has replenished the missing key
     * material, so collections that were stuck waiting for keys resume
     * syncing immediately.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "syncBlockedCollections", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void syncBlockedCollections() {
        var blockedCollections = new ArrayList<SyncPatchType>();
        coordinator.runLocked(() -> {
            for (var patchType : SyncPatchType.values()) {
                var metadata = store.syncStore().findWebAppState(patchType);
                if (metadata.state() == SyncCollectionState.BLOCKED) {
                    store.syncStore().markWebAppStateDirty(patchType);
                    blockedCollections.add(patchType);
                }
            }
        });
        if (!blockedCollections.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unblocking {0} collections after key replenishment", blockedCollections.size());
            pullPatches(blockedCollections.toArray(SyncPatchType[]::new));
        }
    }

    /**
     * Returns an unmodifiable snapshot of the collections currently
     * mid-flight (an IQ request has been dispatched and no response has been
     * parsed yet).
     *
     * <p>Surfaced for diagnostic logging.
     *
     * @implNote
     * This implementation derives the set from the store's
     * {@link SyncCollectionState#IN_FLIGHT} marker rather than from a
     * module-level Set as WA Web does, since Cobalt stores the per-
     * collection state machine in {@link LinkedWhatsAppStore}.
     *
     * @return an unmodifiable {@link Set} of collections in
     *         {@link SyncCollectionState#IN_FLIGHT}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "getInFlightCollections", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public Set<SyncPatchType> getInFlightCollections() {
        return coordinator.runLocked(() -> {
            var result = new LinkedHashSet<SyncPatchType>();
            for (var patchType : SyncPatchType.values()) {
                if (store.syncStore().findWebAppState(patchType).state() == SyncCollectionState.IN_FLIGHT) {
                    result.add(patchType);
                }
            }
            return Collections.unmodifiableSet(result);
        });
    }

    /**
     * Returns an unmodifiable snapshot of the collections that were
     * re-marked dirty while already in flight and therefore require another
     * sync round once the in-flight one completes.
     *
     * <p>Surfaced for diagnostic logging alongside
     * {@link #getInFlightCollections()}.
     *
     * @implNote
     * This implementation derives the set from the store's
     * {@link SyncCollectionState#PENDING} marker; WA Web tracks the same
     * information in a module-level Set.
     *
     * @return an unmodifiable {@link Set} of collections in
     *         {@link SyncCollectionState#PENDING}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "getPendingCollections", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public Set<SyncPatchType> getPendingCollections() {
        return coordinator.runLocked(() -> {
            var result = new LinkedHashSet<SyncPatchType>();
            for (var patchType : SyncPatchType.values()) {
                if (store.syncStore().findWebAppState(patchType).state() == SyncCollectionState.PENDING) {
                    result.add(patchType);
                }
            }
            return Collections.unmodifiableSet(result);
        });
    }

    /**
     * Commits the current syncd telemetry counters by forwarding to the
     * underlying stats and key-stats reporting passes.
     *
     * <p>Triggers an immediate flush of the syncd telemetry, equivalent to
     * the reporter that fires on application resume.
     *
     * @implNote
     * This implementation emits one {@code MdAppStateSyncMutationStats} event
     * per mutation name directly via {@link #reportSyncdStats()} plus one
     * {@code SyncdKeyCount} via {@link #reportSyncdKeyStats()}, with no
     * intermediate aggregate-then-flush counter map.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "reportWam", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void reportWam() {
        reportSyncdStats();
        reportSyncdKeyStats();
    }

    /**
     * Performs the syncd key-info logging that the WhatsApp internal build
     * channel exposes; a no-op in production.
     *
     * @implNote
     * This implementation does nothing because the corresponding WhatsApp
     * Web export is an empty stub for an internal-build logging pipeline that
     * Cobalt has no equivalent for.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "logKeysInfoInIntern", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void logKeysInfoInIntern() {

    }

    /**
     * Runs a batched sync round across multiple collections, handling
     * per-collection error routing, snapshot/patch application, upload
     * acknowledgements, and refetch follow-ups.
     *
     * <p>The single entry point for multi-collection pulls, called by
     * {@link #pullPatches}. The batched IQ packs one {@code <sync>} stanza with
     * one {@code <collection>} child per collection, halving round-trips when
     * multiple dirty bits fire together. Collections that still need
     * pagination after the batched round (the response has more patches) fall
     * back to {@link #syncCollection} per collection.
     *
     * @implNote
     * This implementation skips the local upload portion when a collection
     * has not yet been bootstrapped (its pending mutations are deferred until
     * after the first snapshot lands); the same collections are recorded in
     * {@code skippedUploads} and remarked dirty post-apply so a follow-up
     * round picks them up. Collection-level conflict responses bypass the
     * generic error handler and route directly through the post-apply
     * refetch/UP_TO_DATE decision.
     *
     * @param patchTypes the collections to include in the batched IQ
     * @return {@code true} if any response carried at least one patch or
     *         a snapshot reference
     */
    private boolean syncCollectionsBatched(Set<SyncPatchType> patchTypes) {
        var admitted = coordinator.admitForRound(patchTypes);
        if (admitted.isEmpty()) {
            return false;
        }
        var retrigger = new ArrayList<SyncPatchType>();
        var result = false;
        try {
            result = coordinator.runLocked(() -> syncCollectionsBatchedLocked(admitted));
        } finally {
            for (var patchType : admitted) {
                if (coordinator.clearInFlight(patchType)) {
                    retrigger.add(patchType);
                }
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "batched sync round complete for {0} collections, changes={1}, retrigger={2}", admitted.size(), result, retrigger.size());
        if (!retrigger.isEmpty()) {
            pullPatches(retrigger.toArray(SyncPatchType[]::new));
        }
        return result;
    }

    /**
     * Runs one batched sync round while the {@link #coordinator} in-flight set
     * established by {@link #syncCollectionsBatched} guarantees no concurrent
     * round mutates the same collection's version or LT-Hash and the monitor
     * serializes every synchronous segment.
     *
     * @param patchTypes the collections to include in the batched IQ
     * @return {@code true} if any response carried at least one patch or
     *         a snapshot reference
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdServerSync", exports = "serverSync", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean syncCollectionsBatchedLocked(Set<SyncPatchType> patchTypes) {
        var collectionPatches = new LinkedHashMap<SyncPatchType, SequencedCollection<SyncPendingMutation>>();
        var skippedUploads = new LinkedHashSet<SyncPatchType>();
        for (var patchType : patchTypes) {
            var pending = whatsapp.store().syncStore().findPendingMutations(patchType);
            if (!pending.isEmpty() && !store.syncStore().findWebAppState(patchType).bootstrapped()) {
                skippedUploads.add(patchType);
                pending = List.of();
            }
            collectionPatches.put(patchType, pending);
        }

        var hasAppStateChanges = false;

        try {
            for (var patchType : patchTypes) {
                store.syncStore().markWebAppStateInFlight(patchType);
            }

            var batchedRequest = coordinator.runWithMonitorReleased(() -> requestBuilder.buildBatchedSyncRequest(collectionPatches));
            logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.REQUEST_BUILT);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending batched sync iq for {0} collections", patchTypes.size());
            var responseNode = coordinator.runWithMonitorReleased(() -> whatsapp.sendNode(batchedRequest.node()));
            logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.RESPONSE_RECEIVED);
            var responses = responseParser.parseBatchedSyncResponse(responseNode);
            logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.RESPONSE_PARSED_VALID);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "batched sync response received, {0} collection responses", responses.size());

            for (var response : responses) {
                if (response.collectionError().isPresent()) {
                    var collectionError = response.collectionError().get();
                    if (collectionError instanceof WhatsAppWebAppStateSyncException.Conflict conflict) {
                        if (!response.patches().isEmpty() || response.snapshotReference().isPresent()) {
                            try {
                                handleSyncResponse(response);
                            } catch (Throwable throwable) {
                                handleSyncError(throwable, response.collectionName());
                                continue;
                            }
                        }
                        var hasPending = !whatsapp.store().syncStore().findPendingMutations(response.collectionName()).isEmpty();
                        if (hasPending || conflict.hasMorePatches()) {
                            store.syncStore().markWebAppStateDirty(response.collectionName());
                        } else {
                            store.syncStore().markWebAppStateUpToDate(response.collectionName());
                        }
                    } else {
                        handleSyncError(collectionError, response.collectionName());
                    }
                    continue;
                }

                if (!response.patches().isEmpty() || response.snapshotReference().isPresent()) {
                    hasAppStateChanges = true;
                }

                try {
                    handleSyncResponse(response);
                    var uploadInfo = batchedRequest.uploadInfos().get(response.collectionName());
                    if (uploadInfo != null) {
                        var state = store.syncStore().findWebAppState(response.collectionName()).state();
                        if (state != SyncCollectionState.ERROR_FATAL
                                && state != SyncCollectionState.ERROR_RETRY
                                && state != SyncCollectionState.BLOCKED) {
                            processUploadSuccess(uploadInfo);
                        }
                    }
                    retryScheduler.resetAttemptCounter();
                } catch (Throwable throwable) {
                    handleSyncError(throwable, response.collectionName());
                }
            }
        } catch (Throwable throwable) {
            for (var patchType : patchTypes) {
                handleSyncError(throwable, patchType);
            }
            return hasAppStateChanges;
        }

        for (var patchType : patchTypes) {
            if (skippedUploads.contains(patchType)
                    && store.syncStore().findWebAppState(patchType).bootstrapped()
                    && !whatsapp.store().syncStore().findPendingMutations(patchType).isEmpty()) {
                store.syncStore().markWebAppStateDirty(patchType);
            }
            var state = store.syncStore().findWebAppState(patchType).state();
            if (state == SyncCollectionState.PENDING || state == SyncCollectionState.DIRTY) {
                syncCollectionLocked(patchType);
            }
        }
        return hasAppStateChanges;
    }

    /**
     * Replays every previously deferred orphan mutation across all
     * collections.
     *
     * <p>Called as part of {@link #resumeAfterRestart()} so that mutations
     * whose target entity was not yet present when the snapshot or patch was
     * applied (the canonical orphan case) get a second chance once history
     * sync and incoming messages have caught up.
     *
     * @implNote
     * This implementation iterates the closed {@link SyncPatchType} enum and
     * dispatches each collection's orphans separately rather than querying a
     * unified table, matching {@link LinkedWhatsAppStore}'s per-collection storage
     * layout.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "applyAllOrphansAndUnsupported", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGetSyncAction", exports = "getSyncActionsByActionStatesInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void retryAllOrphanMutations() {
        for (var patchType : SyncPatchType.values()) {
            retryOrphanMutations(patchType);
        }
    }

    /**
     * Retries orphan mutations whose target entities now appear in the
     * supplied identifier sets.
     *
     * <p>Called when fresh entities become known (a history sync chunk
     * landed, an incoming message was accepted, a new thread surfaced). Each
     * call narrows the per-orphan retry to exactly those orphans whose model
     * id is now resolvable, avoiding the unconditional sweep performed by
     * {@link #retryAllOrphanMutations()}.
     *
     * @implNote
     * This implementation invokes the per-model variants in sequence on the
     * calling virtual thread; the orphan-store mutations are already
     * serialized by the store.
     *
     * @param msgIds    the message identifiers to match for
     *                  {@code Msg}-typed orphans
     * @param chatIds   the chat identifiers to match for both
     *                  {@code Chat}- and {@code Account}-typed orphans
     * @param threadIds the thread identifiers to match for
     *                  {@code Thread}-typed orphans; {@code null} or
     *                  empty skips the thread sweep
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "checkOrphanMutations", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdBridgeApi", exports = "SyncdBridgeApi", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void checkOrphanMutations(Collection<String> msgIds, Collection<String> chatIds, Collection<String> threadIds) {
        checkOrphanMessages(msgIds);
        checkOrphanChats(chatIds);
        // TODO: resolve LID accounts before retrying Account-typed orphans (WA Web's bulkGetAccountLid step);
        //       Cobalt currently passes chat ids straight through which loses orphans whose target is a LID-only account.
        retryOrphanMutationsByModelIds(chatIds, "Account");
        if (threadIds != null && !threadIds.isEmpty()) {
            checkOrphanThreads(threadIds);
        }
    }

    /**
     * Retries orphan mutations of model type {@code Msg} whose target message
     * id is in {@code msgIds}.
     *
     * <p>Called whenever a previously unknown message becomes known, so action
     * handlers that referenced it (star, pin, react) can run.
     *
     * @implNote
     * This implementation performs no separate LID enrichment pass; LID
     * resolution happens inside {@link LinkedWhatsAppStore} during lookup.
     *
     * @param msgIds the message identifiers to retry against;
     *               {@code null} or empty is a no-op
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "checkOrphanMessages", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void checkOrphanMessages(Collection<String> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) {
            return;
        }
        retryOrphanMutationsByModelIds(msgIds, "Msg");
    }

    /**
     * Retries orphan mutations of model type {@code Chat} whose target chat id
     * is in {@code chatIds}.
     *
     * <p>Called whenever a previously unknown chat becomes known, so orphan
     * handlers (mute, archive, pin) can re-run against it.
     *
     * @implNote
     * This implementation performs no forced-history enrichment; LID
     * resolution is owned by {@link LinkedWhatsAppStore}.
     *
     * @param chatIds the chat identifiers to retry against;
     *                {@code null} or empty is a no-op
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "checkOrphanChats", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void checkOrphanChats(Collection<String> chatIds) {
        if (chatIds == null || chatIds.isEmpty()) {
            return;
        }
        retryOrphanMutationsByModelIds(chatIds, "Chat");
    }

    /**
     * Retries orphan mutations of model type {@code Thread} whose target
     * thread id is in {@code threadIds}.
     *
     * <p>Called when message thread metadata (community subgroups, comment
     * threads) becomes available.
     *
     * @param threadIds the thread identifiers to retry against;
     *                  {@code null} or empty is a no-op
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "checkOrphanThreads", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void checkOrphanThreads(Collection<String> threadIds) {
        if (threadIds == null || threadIds.isEmpty()) {
            return;
        }
        retryOrphanMutationsByModelIds(threadIds, "Thread");
    }

    /**
     * Retries orphan mutations of model type {@code Agent} whose target agent
     * id is in {@code agentIds}.
     *
     * <p>Called when AI agent definitions are pushed; the agent settings sync
     * handlers re-run against the freshly known agents.
     *
     * @param agentIds the agent identifiers to retry against;
     *                 {@code null} or empty is a no-op
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "checkOrphanAgents", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void checkOrphanAgents(Collection<String> agentIds) {
        retryOrphanMutationsByModelIds(agentIds, "Agent");
    }

    /**
     * Retries orphan mutations of model type {@code ChatAssignment} whose
     * target assignment id is in {@code assignmentIds}.
     *
     * <p>Called when business chat-assignment metadata catches up so
     * assignment-mutation handlers can finalize.
     *
     * @param assignmentIds the chat-assignment identifiers to retry
     *                      against; {@code null} or empty is a no-op
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "checkOrphanChatAssignments", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void checkOrphanChatAssignments(Collection<String> assignmentIds) {
        retryOrphanMutationsByModelIds(assignmentIds, "ChatAssignment");
    }

    /**
     * Retries orphan mutations of model type {@code UserStatusMute} whose
     * target contact id is in {@code contactIds}.
     *
     * <p>Called when status-mute lists referencing previously unknown contacts
     * can finally be applied.
     *
     * @param contactIds the contact identifiers to retry against;
     *                   {@code null} or empty is a no-op
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "checkOrphanUserStatusMutes", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void checkOrphanUserStatusMutes(Collection<String> contactIds) {
        retryOrphanMutationsByModelIds(contactIds, "UserStatusMute");
    }

    /**
     * Retries every orphan whose {@code (modelType, modelId)} pair matches one
     * of the supplied identifiers, after first removing them from the orphan
     * store.
     *
     * <p>The shared helper behind every {@code checkOrphan*} method; applies
     * the matching orphans through the individual-mutation path so each runs
     * through its registered action handler.
     *
     * @implNote
     * This implementation scans every {@link SyncPatchType} because the orphan
     * store is partitioned per collection. Orphans are removed before the
     * handler retry so a handler exception does not leave them queued
     * indefinitely; a failed retry can always re-park them as orphans on the
     * next sync round.
     *
     * @param modelIds  the entity identifiers to retry against;
     *                  {@code null} or empty is a no-op
     * @param modelType the model type to filter orphans by
     *                  ({@code "Msg"}, {@code "Chat"}, ...)
     */
    @WhatsAppWebExport(moduleName = "WAWebGetSyncAction", exports = "getSyncActionsByModelInfosInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    private void retryOrphanMutationsByModelIds(Collection<String> modelIds, String modelType) {
        if (modelIds == null || modelIds.isEmpty()) {
            return;
        }

        var modelIdSet = modelIds instanceof Set ? (Set<String>) modelIds : new HashSet<>(modelIds);
        coordinator.runLocked(() -> {
            for (var patchType : SyncPatchType.values()) {
                var orphans = store.syncStore().findOrphanMutations(patchType);
                if (orphans.isEmpty()) {
                    continue;
                }

                var matching = orphans.stream()
                        .filter(o -> modelType.equals(o.modelType()) && o.modelId() != null && modelIdSet.contains(o.modelId()))
                        .toList();
                if (matching.isEmpty()) {
                    continue;
                }

                store.syncStore().removeOrphanMutations(patchType, matching);
                retryOrphanEntries(patchType, matching);
            }
        });
    }

    /**
     * Retries orphan mutations of type {@code FavoriteSticker}, gated by the
     * {@code favorite_sticker} primary feature and the
     * {@link ABProp#FAVORITE_STICKER_SYNC_AFTER_PAIRING_ENABLED_WEB} AB prop.
     *
     * <p>Triggered after a fresh pairing once the sticker pack inventory has
     * been fetched, so any favorite-sticker actions that referred to stickers
     * not yet downloaded at the time of the original snapshot can finally be
     * applied.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdOrphan", exports = "checkOrphanFavoriteStickers", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void checkOrphanFavoriteStickers() {
        if (!store.syncStore().primaryFeatures().contains("favorite_sticker")) {
            return;
        }
        retryOrphanMutationsByModelType("FavoriteSticker", () ->
                abPropsService.getBool(ABProp.FAVORITE_STICKER_SYNC_AFTER_PAIRING_ENABLED_WEB)
        );
    }

    /**
     * Retries every orphan of the given {@code modelType}, after confirming
     * the optional gate passes.
     *
     * <p>The shared helper behind {@link #checkOrphanFavoriteStickers()}; the
     * gate is evaluated only after orphan presence is confirmed.
     *
     * @implNote
     * This implementation collects all matching orphans across every
     * {@link SyncPatchType} into a single list before applying them, so the
     * gate is evaluated exactly once per call even though Cobalt fans out
     * across collections.
     *
     * @param modelType the model type to filter orphans by
     * @param condition the optional gate; if non-{@code null} and
     *                  returns {@code false}, no mutations are applied
     */
    @WhatsAppWebExport(moduleName = "WAWebGetSyncAction", exports = "getOrphanSyncActionsByModelTypeInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    private void retryOrphanMutationsByModelType(String modelType, BooleanSupplier condition) {
        coordinator.runLocked(() -> {
            var allMatching = new ArrayList<Map.Entry<SyncPatchType, List<OrphanMutationEntry>>>();
            for (var patchType : SyncPatchType.values()) {
                var orphans = store.syncStore().findOrphanMutations(patchType);
                if (orphans.isEmpty()) {
                    continue;
                }
                var matching = orphans.stream()
                        .filter(o -> modelType.equals(o.modelType()))
                        .toList();
                if (!matching.isEmpty()) {
                    allMatching.add(Map.entry(patchType, matching));
                }
            }

            if (allMatching.isEmpty()) {
                return;
            }

            if (condition != null && !condition.getAsBoolean()) {
                return;
            }

            for (var entry : allMatching) {
                store.syncStore().removeOrphanMutations(entry.getKey(), entry.getValue());
                retryOrphanEntries(entry.getKey(), entry.getValue());
            }
        });
    }

    /**
     * Recovers interrupted sync state after a JVM restart and kicks off
     * follow-up sync, orphan, and unsupported-mutation passes.
     *
     * <p>Called once on client startup to discharge state that was left in a
     * non-terminal sync state when the previous process died, then sync
     * everything that still has pending mutations or unfinished server-side
     * work. Each non-terminal state is rewritten back to
     * {@link SyncCollectionState#DIRTY} so the next pull picks it up:
     * <ul>
     *   <li>{@code IN_FLIGHT}: the in-flight IQ was lost on shutdown</li>
     *   <li>{@code PENDING}: a subsequent round was scheduled but never
     *       started</li>
     *   <li>{@code ERROR_RETRY}: a retryable error was pending a backoff
     *       retry</li>
     *   <li>{@code BLOCKED}: a missing key request is still in flight, so both
     *       the timeout check and the periodic re-request job are rearmed
     *       before dirtying the collection</li>
     * </ul>
     * Collections in {@code UP_TO_DATE} with non-empty pending mutations are
     * also dirtied so a fresh push round runs.
     *
     * @implNote
     * This implementation does the work of WA Web's combined
     * {@code initializeStateMachine}, {@code processOnAppResume}, and
     * {@code syncPendingMutationsAndBlockedCollections} in one pass;
     * Cobalt does not maintain the IndexedDB-backed state machine that
     * WA Web reloads on startup, so the equivalent of
     * {@code loadStatesFromDb} is implicit in {@link LinkedWhatsAppStore}'s
     * boot. {@code ERROR_FATAL} is treated as terminal because Cobalt
     * has no UI callback equivalent to
     * {@code WAWebSyncdDbCallbacksApi.handleSyncdFatal}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "initializeStateMachine", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncd", exports = "processOnAppResume", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void resumeAfterRestart() {
        var collectionsToSync = new LinkedHashSet<SyncPatchType>();
        coordinator.runLocked(() -> {
            for (var patchType : SyncPatchType.values()) {
                var metadata = store.syncStore().findWebAppState(patchType);
                switch (metadata.state()) {
                    case IN_FLIGHT, PENDING, ERROR_RETRY -> {
                        store.syncStore().markWebAppStateDirty(patchType);
                        collectionsToSync.add(patchType);
                    }
                    case BLOCKED -> {
                        missingSyncKeyTimeoutScheduler.scheduleTimeoutCheck();
                        missingSyncKeyTimeoutScheduler.startPeriodicReRequestJob();
                        store.syncStore().markWebAppStateDirty(patchType);
                        collectionsToSync.add(patchType);
                    }
                    case DIRTY -> collectionsToSync.add(patchType);
                    default -> {

                    }
                }

                if (!whatsapp.store().syncStore().findPendingMutations(patchType).isEmpty()
                        && store.syncStore().findWebAppState(patchType).state() != SyncCollectionState.ERROR_FATAL) {
                    if (store.syncStore().findWebAppState(patchType).state() == SyncCollectionState.UP_TO_DATE) {
                        store.syncStore().markWebAppStateDirty(patchType);
                    }
                    collectionsToSync.add(patchType);
                }
            }
        });

        if (Log.INFO) LOGGER.log(Level.INFO, "resuming app-state sync after restart, {0} collections need syncing", collectionsToSync.size());

        retryAllOrphanMutations();
        retryUnsupportedMutations();

        if (!collectionsToSync.isEmpty()) {
            pullPatches(collectionsToSync.toArray(SyncPatchType[]::new));
        }
    }

    /**
     * The maximum number of single-collection sync iterations before
     * the loop bails out and parks the collection in
     * {@link SyncCollectionState#ERROR_RETRY}.
     *
     * @implNote
     * This implementation matches WA Web's {@code C = 500} cap in
     * {@code WAWebSyncdServerSync.serverSync}; it is high enough to
     * accommodate a deep pagination chain without becoming a runaway
     * loop if the server lies about {@code has_more_patches}.
     */
    private static final int MAX_SYNC_ITERATIONS = 500;

    /**
     * The result of a single-collection sync round.
     *
     * <p>Carries the parsed response, the optional upload metadata, and a flag
     * indicating whether pending mutations had to be deferred because the
     * collection had not yet been bootstrapped.
     *
     * @param response             the parsed sync response
     * @param uploadInfo           the upload metadata, or {@code null}
     *                             if the request carried no local
     *                             patch
     * @param skippedPendingUpload {@code true} if pending mutations
     *                             were withheld because the collection
     *                             was not yet bootstrapped
     */
    private record SyncRoundResult(
            MutationSyncResponse response,
            SyncRequest.UploadedPatchInfo uploadInfo,
            boolean skippedPendingUpload
    ) {
    }

    /**
     * A pending sync action entry update produced during LT-Hash computation
     * and applied only after the version guard passes.
     *
     * <p>Holds either a {@link SyncActionEntry} to put under {@code indexMac}
     * or a remove marker; the choice is encoded by the {@code remove} flag
     * rather than by {@code entry} being {@code null}, since put-with-null is
     * semantically distinct.
     *
     * @param indexMac the index MAC identifying the entry
     * @param entry    the entry to persist, or {@code null} when this
     *                 update is a remove
     * @param remove   {@code true} when this update is a remove
     */
    private record SyncActionEntryUpdate(
            byte[] indexMac,
            SyncActionEntry entry,
            boolean remove
    ) {
    }

    /**
     * The output of an incremental LT-Hash computation.
     *
     * <p>Bundles the new LT-Hash with the deferred entry updates so the caller
     * can pass the new hash through the version-guard check and apply the
     * updates atomically only if the guard passes.
     *
     * @param newHash the computed LT-Hash
     * @param updates the sync action entry updates to persist if the
     *                version guard passes
     */
    private record LtHashComputation(
            byte[] newHash,
            List<SyncActionEntryUpdate> updates
    ) {
    }

    /**
     * Drives a single collection's sync loop until it lands in
     * {@link SyncCollectionState#UP_TO_DATE} or hits an unrecoverable error.
     *
     * <p>The per-collection fallback called from
     * {@link #syncCollectionsBatched} once the batched IQ has been applied and
     * one or more collections still have {@link SyncCollectionState#PENDING}
     * or {@link SyncCollectionState#DIRTY} state (typically because they
     * paginated). Each loop iteration is one IQ round-trip.
     *
     * @implNote
     * This implementation caps the loop at {@link #MAX_SYNC_ITERATIONS}
     * iterations and parks the collection in
     * {@link SyncCollectionState#ERROR_RETRY} when the cap is hit; a conflict
     * carrying more patches always re-loops, while a plain conflict
     * short-circuits to UP_TO_DATE when there are no pending mutations.
     *
     * @param patchType the collection to drive to completion
     */
    private void syncCollection(SyncPatchType patchType) {
        while (!coordinator.admitForRound(Set.of(patchType)).isEmpty()) {
            var retrigger = false;
            try {
                coordinator.runLocked(() -> syncCollectionLocked(patchType));
            } finally {
                retrigger = coordinator.clearInFlight(patchType);
            }
            if (!retrigger) {
                break;
            }
        }
    }

    /**
     * Drives a single collection's sync loop while the {@link #coordinator}
     * in-flight set established by {@link #syncCollection} guarantees no
     * concurrent round mutates the same collection's version or LT-Hash and the
     * monitor serializes every synchronous segment.
     *
     * @param patchType the collection to drive to completion
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdServerSync", exports = "serverSync", adaptation = WhatsAppAdaptation.ADAPTED)
    private void syncCollectionLocked(SyncPatchType patchType) {
        var iterations = 0;
        while(store.syncStore().findWebAppState(patchType).state() != SyncCollectionState.UP_TO_DATE) {
            if (++iterations > MAX_SYNC_ITERATIONS) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "iteration cap reached for collection {0} after {1} iterations", patchType, MAX_SYNC_ITERATIONS);
                store.syncStore().markWebAppStateErrorRetry(patchType);
                break;
            }

            try {
                var syncResult = sendSyncRequestOrThrow(patchType);
                var response = syncResult.response();

                if (response.collectionError().isPresent()) {
                    var conflict = (WhatsAppWebAppStateSyncException.Conflict) response.collectionError().get();
                    if (!response.patches().isEmpty() || response.snapshotReference().isPresent()) {
                        handleSyncResponse(response);
                    }
                    var hasPending = !whatsapp.store().syncStore().findPendingMutations(patchType).isEmpty();
                    if (hasPending || conflict.hasMorePatches()) {
                        store.syncStore().markWebAppStateDirty(patchType);
                    } else {
                        store.syncStore().markWebAppStateUpToDate(patchType);
                    }
                    retryScheduler.resetAttemptCounter();
                    continue;
                }

                handleSyncResponse(response);

                if (syncResult.uploadInfo() != null) {
                    processUploadSuccess(syncResult.uploadInfo());
                }

                if (syncResult.skippedPendingUpload()
                        && store.syncStore().findWebAppState(patchType).bootstrapped()
                        && !whatsapp.store().syncStore().findPendingMutations(patchType).isEmpty()) {
                    store.syncStore().markWebAppStateDirty(patchType);
                }

                retryScheduler.resetAttemptCounter();
            } catch (Throwable throwable) {
                handleSyncError(throwable, patchType);
                break;
            }
        }
    }

    /**
     * Builds, dispatches, and parses one sync IQ round for a single
     * collection, marking the collection in-flight for the duration.
     *
     * <p>The unit step of {@link #syncCollection}; runs synchronously on a
     * virtual thread because {@link LinkedWhatsAppClient#sendNode(StanzaBuilder)}
     * is a blocking call. Pending mutations are skipped (and signalled via
     * {@link SyncRoundResult#skippedPendingUpload()}) when the collection has
     * not yet been bootstrapped, so the snapshot lands first and the local
     * mutations replay on the next round.
     *
     * @implNote
     * This implementation emits the bootstrap-stage telemetry markers
     * ({@link BootstrapAppStateDataStageCode#REQUEST_BUILT},
     * {@link BootstrapAppStateDataStageCode#RESPONSE_RECEIVED},
     * {@link BootstrapAppStateDataStageCode#RESPONSE_PARSED_VALID}) at the
     * points the critical-bootstrap stage tracker expects them.
     *
     * @param patchType the collection to sync
     * @return the parsed response, upload metadata, and skipped-upload
     *         flag bundled in a {@link SyncRoundResult}
     */
    private SyncRoundResult sendSyncRequestOrThrow(SyncPatchType patchType) {
        var pendingMutations = whatsapp.store().syncStore().findPendingMutations(patchType);
        var skippedPendingUpload = false;

        if (!pendingMutations.isEmpty() && !store.syncStore().findWebAppState(patchType).bootstrapped()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "skipping pending mutations for unbootstrapped collection {0}", patchType);
            skippedPendingUpload = true;
            pendingMutations = List.of();
        }

        var pendingForBuild = pendingMutations;
        var syncRequest = coordinator.runWithMonitorReleased(() -> requestBuilder.buildSyncRequest(patchType, pendingForBuild));

        store.syncStore().markWebAppStateInFlight(patchType);

        logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.REQUEST_BUILT);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending sync iq for collection {0}", patchType);
        var response = coordinator.runWithMonitorReleased(() -> whatsapp.sendNode(syncRequest.node()));
        logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.RESPONSE_RECEIVED);

        var parsedResponse = responseParser.parseSyncResponse(response);
        logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.RESPONSE_PARSED_VALID);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sync response for collection {0}: patches={1}, hasMore={2}", patchType, parsedResponse.patches().size(), parsedResponse.hasMore());
        return new SyncRoundResult(parsedResponse, syncRequest.uploadInfo(), skippedPendingUpload);
    }

    /**
     * Applies a single-collection sync response: snapshot (if any) then
     * patches in version order, then state-machine transition.
     *
     * <p>The wrapper around {@link #handleSyncResponseInternal} that adds
     * bootstrap-download telemetry tracking. The inner method does the
     * snapshot decode, mutation decryption, MAC verification, LT-Hash
     * recomputation, sync-action persistence, and handler dispatch; this
     * wrapper measures total download size up front and emits the
     * data-downloaded event on success or failure once the inner call
     * returns.
     *
     * @implNote
     * This implementation tracks the bootstrap download only on the
     * first-time bootstrap path, so steady-state sync rounds skip the
     * telemetry tracker construction entirely.
     *
     * @param syncResponse the parsed response for a single collection
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandler", exports = "applyAppStateSyncResponse", adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleSyncResponse(MutationSyncResponse syncResponse) {
        var collectionName = syncResponse.collectionName();
        var wasBootstrapped = store.syncStore().findWebAppState(collectionName).bootstrapped();
        var localVersionBeforeSync = getCurrentVersion(collectionName);

        var bootstrapDownload = wasBootstrapped ? null : new BootstrapDownloadTracker(collectionName);
        if (bootstrapDownload != null) {
            if (syncResponse.snapshotReference().isPresent()) {
                var snapshotSize = syncResponse.snapshotReference().get().fileSizeBytes();
                if (snapshotSize.isPresent()) {
                    bootstrapDownload.addBytes(snapshotSize.getAsLong());
                }
            }
            for (var patch : syncResponse.patches()) {
                if (patch.externalMutations().isPresent()) {
                    var patchSize = patch.externalMutations().get().fileSizeBytes();
                    if (patchSize.isPresent()) {
                        bootstrapDownload.addBytes(patchSize.getAsLong());
                    }
                }
            }
        }

        try {
            handleSyncResponseInternal(syncResponse, collectionName, wasBootstrapped, localVersionBeforeSync);
        } catch (Throwable throwable) {
            if (bootstrapDownload != null) {
                emitBootstrapAppStateDataDownloaded(bootstrapDownload, MdBootstrapStepResult.FAILURE, throwable);
            }
            throw throwable;
        }

        if (bootstrapDownload != null) {
            emitBootstrapAppStateDataDownloaded(bootstrapDownload, MdBootstrapStepResult.SUCCESS, null);
        }
    }

    /**
     * Performs the snapshot decode, patch apply, integrity check, and state
     * transition for a single-collection response.
     *
     * <p>Extracted from {@link #handleSyncResponse} so the bootstrap download
     * metric wrap-around lives in one place; this method does everything else:
     * clears stale sync action entries, decrypts mutations, verifies snapshot
     * and patch MACs, drives the snapshot-recovery handshake if a snapshot MAC
     * fails fatally, emits per-mutation telemetry, and finally transitions the
     * collection to {@link SyncCollectionState#PENDING} or
     * {@link SyncCollectionState#UP_TO_DATE} based on
     * {@link MutationSyncResponse#hasMore()}.
     *
     * @implNote
     * This implementation hoists the "collection is already in mac-mismatch
     * fatal state" guard above the per-patch integrity call to avoid the
     * per-patch value-MAC list allocation when the collection is already known
     * bad; the inner guard in {@link MutationIntegrityVerifier} is kept as
     * defense in depth.
     *
     * @param syncResponse           the parsed response
     * @param collectionName         the collection being processed
     * @param wasBootstrapped        {@code true} if the collection had
     *                               already been bootstrapped before
     *                               this round
     * @param localVersionBeforeSync the local collection version
     *                               captured before any mutation was
     *                               applied; used for the
     *                               missing-patches guard
     */
    private void handleSyncResponseInternal(
            MutationSyncResponse syncResponse,
            SyncPatchType collectionName,
            boolean wasBootstrapped,
            long localVersionBeforeSync
    ) {
        var applyStartTs = System.currentTimeMillis();
        var snapshotAppliedFromServer = syncResponse.snapshotReference().isPresent();
        var patchesPresent = !syncResponse.patches().isEmpty();
        var recoveredFromSnapshot = false;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "applying sync response for {0}: bootstrapped={1}, snapshot={2}, patches={3}", collectionName, wasBootstrapped, snapshotAppliedFromServer, syncResponse.patches().size());
        if (syncResponse.snapshotReference().isPresent()) {
            store.syncStore().clearSyncActionEntries(collectionName);

            var snapshot = coordinator.runWithMonitorReleased(() -> downloadAndDecodeSnapshot(syncResponse.snapshotReference().get()));
            var snapshotVersionEntry = snapshot.version().orElse(null);
            var snapshotProtoVersion = snapshotVersionEntry == null
                    ? 0L
                    : snapshotVersionEntry.version().orElse(0L);
            if (snapshotProtoVersion <= 0L) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError("missing snapshot version", null);
            }
            var snapshotMutations = getMutationsFromSnapshot(snapshot);
            var untrusted = snapshotMutations.isEmpty()
                    ? List.<DecryptedMutation.Untrusted>of()
                    : decryptMutations(snapshotMutations);

            emitSyncdMutationWamEvents(
                    collectionName,
                    snapshotProtoVersion,
                    MutationDirectionType.INCOMING,
                    MutationBundleType.SNAPSHOT,
                    untrusted,
                    null);

            reportDecryptedMutationMessageRanges(untrusted);

            if (!untrusted.isEmpty()) {
                validateNoDuplicateIndices(collectionName, untrusted, false);
            }

            var newHash = computeNewLTHash(collectionName, MutationLTHash.EMPTY_HASH, untrusted);

            emitSyncdBundleAndSummaryWamEvents(
                    collectionName,
                    snapshotProtoVersion,
                    MutationDirectionType.INCOMING,
                    MutationBundleType.SNAPSHOT,
                    !wasBootstrapped,
                    untrusted,
                    newHash.newHash(),
                    snapshot.mac().orElse(null),
                    snapshot.mac().orElse(null),
                    null);

            try {
                integrityVerifier.verifySnapshotMac(collectionName, snapshotProtoVersion, snapshot, newHash.newHash());

                var versionApplied = updateCollectionState(collectionName, snapshotProtoVersion, newHash.newHash());
                if (versionApplied) {
                    applySyncActionEntryUpdates(collectionName, newHash.updates());
                }
                if (!untrusted.isEmpty()) {
                    var snapshotTrusted = new ArrayList<DecryptedMutation.Trusted>(untrusted.size());
                    for (var entry : untrusted) {
                        snapshotTrusted.add(new DecryptedMutation.Trusted(entry.index(), entry.value().orElse(null), entry.operation(), entry.timestamp(), entry.actionVersion()));
                    }
                    applyMutations(collectionName, snapshotTrusted);
                }
            } catch (WhatsAppWebAppStateSyncException e) {
                if (!e.isFatal() || !snapshotRecoveryService.shouldAttemptRecovery(collectionName, snapshotMutations.size())) {
                    throw e;
                }

                if (Log.WARNING) LOGGER.log(Level.WARNING, "snapshot mac validation failed for " + collectionName + ", requesting peer recovery", e);
                var recoveredSnapshot = coordinator.runWithMonitorReleased(() -> snapshotRecoveryService.requestRecovery(collectionName));
                if (recoveredSnapshot == null) {
                    throw e;
                }

                var recoveredName = recoveredSnapshot.collectionName()
                        .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                                "Recovery response missing collection name for " + collectionName,
                                null
                        ));
                if (!recoveredName.equals(collectionName.toString())) {
                    throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Recovery response collection mismatch: expected " + collectionName + " but got " + recoveredName, null);
                }

                store.syncStore().clearSyncActionEntries(collectionName);

                var recoveredTrusted = processRecoveredSnapshot(collectionName, recoveredSnapshot);
                if (!recoveredTrusted.isEmpty()) {
                    applyMutations(collectionName, recoveredTrusted);
                }
                recoveredFromSnapshot = true;
            }
        }

        if (recoveredFromSnapshot) {
            emitBootstrapDataAppliedIfNeeded(
                    collectionName,
                    wasBootstrapped,
                    snapshotAppliedFromServer,
                    patchesPresent,
                    true,
                    applyStartTs);
            store.syncStore().markWebAppStateUpToDate(collectionName);
            return;
        }

        if (syncResponse.snapshotReference().isEmpty()
                && syncResponse.patches().isEmpty()
                && !wasBootstrapped) {
            updateCollectionState(collectionName, 0L, MutationLTHash.EMPTY_HASH);
        }

        var sortedPatches = new ArrayList<>(syncResponse.patches());
        sortedPatches.sort(Comparator.comparingLong(patch -> patch.version()
                .map(version -> version.version().orElse(0L))
                .orElse(0L)));

        validateNoDuplicatePatchVersions(collectionName, sortedPatches);

        if (!sortedPatches.isEmpty()) {
            long minPatchVersion = sortedPatches.getFirst().version()
                    .map(version -> version.version().orElse(0L))
                    .orElse(0L);
            if (wasBootstrapped && minPatchVersion > localVersionBeforeSync + 1) {
                throw new WhatsAppWebAppStateSyncException.MissingPatches(collectionName, localVersionBeforeSync, minPatchVersion);
            }
        }

        for (var patch : sortedPatches) {
            if (patch.exitCode().isPresent()) {
                throw new WhatsAppWebAppStateSyncException.TerminalPatch(collectionName, patch.exitCode().get());
            }

            var patchVer = patch.version()
                    .map(v -> v.version().orElse(0L))
                    .orElse(0L);
            if (patchVer <= 0) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Patch missing required version field in " + collectionName, null);
            }
            if (patch.patchMac().isEmpty() || patch.patchMac().get().length == 0) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Patch missing required patchMac field in " + collectionName + " at version " + patchVer, null);
            }
            if (patch.snapshotMac().isEmpty() || patch.snapshotMac().get().length == 0) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Patch missing required snapshotMac field in " + collectionName + " at version " + patchVer, null);
            }

            var patchMutations = coordinator.runWithMonitorReleased(() -> getMutationsFromPatch(patch));
            if (patchMutations.isEmpty()) {
                continue;
            }

            var untrusted = decryptMutations(patchMutations);
            emitSyncdMutationWamEvents(
                    collectionName,
                    patch.version().map(v -> v.version().orElse(0L)).orElse(0L),
                    MutationDirectionType.INCOMING,
                    MutationBundleType.PATCH,
                    untrusted,
                    patch.patchMac().orElse(null));

            reportDecryptedMutationMessageRanges(untrusted);
            validateNoDuplicateIndices(collectionName, untrusted, true);

            var currentHash = getCurrentLTHash(collectionName);
            long patchVersion = patch.version()
                    .map(version -> version.version().orElse(0L))
                    .orElse(0L);
            if (patchVersion > 1 && Arrays.equals(currentHash, MutationLTHash.EMPTY_HASH)) {
                throw new WhatsAppWebAppStateSyncException.RetryableServerError(
                        "Empty LT-Hash for non-bootstrap patch version " + patchVersion + " in " + collectionName);
            }

            var newHash = computeNewLTHash(collectionName, currentHash, untrusted);

            emitSyncdBundleAndSummaryWamEvents(
                    collectionName,
                    patchVersion,
                    MutationDirectionType.INCOMING,
                    MutationBundleType.PATCH,
                    !wasBootstrapped,
                    untrusted,
                    newHash.newHash(),
                    patch.patchMac().orElse(null),
                    patch.snapshotMac().orElse(null),
                    patch.patchMac().orElse(null));

            if (!store.syncStore().isCollectionInMacMismatchFatal(collectionName)) {
                var patchValueMacs = untrusted.stream()
                        .map(DecryptedMutation.Untrusted::valueMac)
                        .toList();
                var snapshotMacValid = integrityVerifier.verifyPatchIntegrity(collectionName, patch, newHash.newHash(), patchValueMacs);
                if (!snapshotMacValid) {
                    store.syncStore().markWebAppStateMacMismatch(collectionName);
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "patch snapshot mac mismatch for {0} at version {1}, marking mac-mismatch", collectionName, patchVersion);
                }
            }

            var patchVersionApplied = updateCollectionState(collectionName, patchVersion, newHash.newHash());
            if (patchVersionApplied) {
                applySyncActionEntryUpdates(collectionName, newHash.updates());
            }

            var ordered = deduplicateAndOrder(untrusted);
            var patchTrusted = new ArrayList<DecryptedMutation.Trusted>(ordered.size());
            for (var entry : ordered) {
                patchTrusted.add(new DecryptedMutation.Trusted(entry.index(), entry.value().orElse(null), entry.operation(), entry.timestamp(), entry.actionVersion()));
            }
            if (!patchTrusted.isEmpty()) {
                applyMutations(collectionName, patchTrusted);
            }
        }

        emitBootstrapDataAppliedIfNeeded(
                collectionName,
                wasBootstrapped,
                snapshotAppliedFromServer,
                patchesPresent,
                snapshotAppliedFromServer,
                applyStartTs);

        if (syncResponse.hasMore()) {
            store.syncStore().markWebAppStatePending(collectionName);
        } else {
            store.syncStore().markWebAppStateUpToDate(collectionName);
        }
    }

    /**
     * Emits the data-applied event that marks the first-time bootstrap apply
     * of a non-critical collection.
     *
     * <p>Only fires when this round was the collection's bootstrap apply (the
     * collection had no prior version), the server payload actually carried
     * data (snapshot or patches), and the collection is non-critical. The
     * event surfaces in the WAM bootstrap dashboard as the per-collection
     * apply duration.
     *
     * @implNote
     * This implementation omits the {@code mdSessionId} field (a hash of
     * primary and companion identity keys) because Cobalt has no equivalent
     * derivation; the same omission applies to the data-downloaded and
     * critical-data-processing events. Critical collections have no separate
     * Cobalt event because bootstrap tracking is already per-collection on
     * {@link SyncCollectionMetadata}.
     *
     * @param collectionName            the collection being bootstrapped
     * @param wasBootstrapped           {@code true} if the collection
     *                                  had already been bootstrapped
     *                                  before this round, in which case
     *                                  no event fires
     * @param snapshotAppliedFromServer {@code true} if the server
     *                                  payload carried a snapshot
     *                                  reference
     * @param patchesPresent            {@code true} if the server
     *                                  payload carried at least one
     *                                  patch
     * @param usedSnapshot              the value to set on
     *                                  {@code usedSnapshot}
     *                                  ({@code true} when a snapshot
     *                                  was applied, including the
     *                                  peer-recovered path)
     * @param applyStartTs              the millisecond timestamp
     *                                  captured at the start of the
     *                                  apply phase
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandler", exports = "applyAppStateSyncResponse", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdMetrics", exports = "reportSyncdBootstrapDataApplied", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCollectionHandlerWamMutation", exports = "logMetricsForDataApplied", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitBootstrapDataAppliedIfNeeded(
            SyncPatchType collectionName,
            boolean wasBootstrapped,
            boolean snapshotAppliedFromServer,
            boolean patchesPresent,
            boolean usedSnapshot,
            long applyStartTs
    ) {
        if (!snapshotAppliedFromServer && !patchesPresent) {
            return;
        }
        if (wasBootstrapped) {
            return;
        }
        if (collectionName.isCritical()) {
            return;
        }
        var now = System.currentTimeMillis();
        var duration = (int) (now - applyStartTs);
        wamService.commit(new MdBootstrapDataAppliedEventBuilder()
                .mdBootstrapPayloadType(MdBootstrapPayloadType.NON_CRITICAL)
                .mdBootstrapSource(MdBootstrapSource.APP_STATE)
                .collection(mapCollection(collectionName))
                .mdBootstrapStepDuration(duration)
                .usedSnapshot(usedSnapshot)
                .mdTimestamp((int) now)
                .build());
    }

    /**
     * Maps a {@link SyncPatchType} to the matching
     * {@link com.github.auties00.cobalt.wire.wam.type.Collection} WAM enum constant.
     *
     * <p>Used by the bootstrap-data-applied event builder to translate the
     * internal collection type into the value consumed by the WAM pipeline.
     *
     * @implNote
     * This implementation relies on {@link SyncPatchType}'s closed enum domain
     * so the switch is exhaustive without a default.
     *
     * @param collectionName the non-{@code null} collection
     * @return the corresponding
     *         {@link com.github.auties00.cobalt.wire.wam.type.Collection}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMetrics", exports = "collectionNameToMetric", adaptation = WhatsAppAdaptation.DIRECT)
    private static com.github.auties00.cobalt.wire.wam.type.Collection mapCollection(SyncPatchType collectionName) {
        return switch (collectionName) {
            case CRITICAL_BLOCK -> com.github.auties00.cobalt.wire.wam.type.Collection.CRITICAL_BLOCK;
            case CRITICAL_UNBLOCK_LOW -> com.github.auties00.cobalt.wire.wam.type.Collection.CRITICAL_UNBLOCK_LOW;
            case REGULAR -> com.github.auties00.cobalt.wire.wam.type.Collection.REGULAR;
            case REGULAR_HIGH -> com.github.auties00.cobalt.wire.wam.type.Collection.REGULAR_HIGH;
            case REGULAR_LOW -> com.github.auties00.cobalt.wire.wam.type.Collection.REGULAR_LOW;
        };
    }

    /**
     * Maps a {@link SyncPatchType} to the matching {@link SyncdCollectionType}
     * WAM enum constant.
     *
     * <p>Used by the per-mutation and per-bucket stats events to identify
     * which collection the mutation belongs to.
     *
     * @param collectionName the non-{@code null} collection
     * @return the corresponding {@link SyncdCollectionType}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdWamReportingUtils", exports = "h", adaptation = WhatsAppAdaptation.DIRECT)
    private static SyncdCollectionType mapSyncdCollectionType(SyncPatchType collectionName) {
        return switch (collectionName) {
            case CRITICAL_BLOCK -> SyncdCollectionType.CRITICAL_BLOCK;
            case CRITICAL_UNBLOCK_LOW -> SyncdCollectionType.CRITICAL_UNBLOCK_LOW;
            case REGULAR -> SyncdCollectionType.REGULAR;
            case REGULAR_HIGH -> SyncdCollectionType.REGULAR_HIGH;
            case REGULAR_LOW -> SyncdCollectionType.REGULAR_LOW;
        };
    }

    /**
     * Commits one mutation event per decrypted mutation for an incoming
     * snapshot or patch.
     *
     * <p>Called from the snapshot and patch branches of
     * {@link #handleSyncResponseInternal} after mutations are decrypted
     * (post-verify, pre-apply), so the WAM record captures the mutation even
     * if the handler dispatch later fails.
     *
     * @implNote
     * This implementation has no per-call-site AB allowlist gate; Cobalt's WAM
     * pipeline filters upstream. The session-id fields are omitted for the
     * same identity-key-hashing reason as
     * {@link #emitBootstrapDataAppliedIfNeeded}.
     *
     * @param collectionName the collection the mutations belong to
     * @param seqNumber      the patch or snapshot version number
     * @param direction      {@link MutationDirectionType#INCOMING} for
     *                       snapshot/patch apply,
     *                       {@link MutationDirectionType#OUTGOING}
     *                       for upload acknowledgement
     * @param bundle         {@link MutationBundleType#SNAPSHOT} for
     *                       the snapshot branch,
     *                       {@link MutationBundleType#PATCH} for the
     *                       patch branch
     * @param mutations      the decrypted mutations
     * @param patchMac       the wire patch MAC for PATCH bundles, or
     *                       {@code null} for SNAPSHOT bundles
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdWamReportingUtils", exports = "syncReportMutationToWam", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitSyncdMutationWamEvents(
            SyncPatchType collectionName,
            long seqNumber,
            MutationDirectionType direction,
            MutationBundleType bundle,
            SequencedCollection<DecryptedMutation.Untrusted> mutations,
            byte[] patchMac) {
        if (mutations.isEmpty()) {
            return;
        }
        var syncdCollection = mapSyncdCollectionType(collectionName);
        var seqNumberInt = (int) seqNumber;
        var patchMacStr = patchMac == null || patchMac.length == 0
                ? ""
                : Base64.getUrlEncoder().withoutPadding().encodeToString(patchMac);
        for (var mutation : mutations) {
            var mutationName = SyncdIndexUtils
                    .getMutationNameFromIndex(null, mutation.index());
            if (mutationName == null || mutationName.isBlank()) {
                mutationName = "no-mutation-name";
            }
            var mutationMac = Base64.getUrlEncoder().withoutPadding().encodeToString(mutation.indexMac());
            var mutationOperation = mutation.operation() == SyncdOperation.REMOVE
                    ? MutationOperationType.REMOVE
                    : MutationOperationType.SET;
            wamService.commit(new MdSyncdMutationEventBuilder()
                    .contentLength(0)
                    .isInBootstrap(false)
                    .isUsingLid(false)
                    .mutationBundle(bundle)
                    .mutationDirection(direction)
                    .mutationMac(mutationMac)
                    .mutationName(mutationName)
                    .mutationOperation(mutationOperation)
                    .seqNumber(seqNumberInt)
                    .syncdCollection(syncdCollection)
                    .syncdKeyhash("")
                    .syncdKeyid("")
                    .patchMac(patchMacStr)
                    .build());
        }
    }

    /**
     * Commits one mutation event per acknowledged mutation after a successful
     * outgoing patch upload.
     *
     * <p>Called from {@link #processUploadSuccess} once the server
     * acknowledges the push; the event lets WAM dashboards count
     * outgoing-side mutations symmetrically with incoming snapshots and
     * patches.
     *
     * @implNote
     * This implementation always emits with {@code mutationBundle=PATCH} and
     * {@code mutationDirection=OUTGOING} because uploads are by definition
     * outgoing patches post-bootstrap; the per-mutation wire patch MAC is not
     * preserved on {@link SyncRequest.UploadedMutationInfo}, so the patch MAC
     * field is left at its default empty value.
     *
     * @param collectionName the collection that was uploaded
     * @param seqNumber      the new patch version acknowledged by the
     *                       server
     * @param mutations      the acknowledged mutations
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandler", exports = "$e", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitSyncdMutationWamEventsForUpload(
            SyncPatchType collectionName,
            long seqNumber,
            List<SyncRequest.UploadedMutationInfo> mutations) {
        if (mutations.isEmpty()) {
            return;
        }
        var syncdCollection = mapSyncdCollectionType(collectionName);
        var seqNumberInt = (int) seqNumber;
        for (var mutation : mutations) {
            var mutationName = SyncdIndexUtils
                    .getMutationNameFromIndex(null, mutation.actionIndex());
            if (mutationName == null || mutationName.isBlank()) {
                mutationName = "no-mutation-name";
            }
            var mutationMac = Base64.getUrlEncoder().withoutPadding().encodeToString(mutation.indexMac());
            var mutationOperation = mutation.operation() == SyncdOperation.REMOVE
                    ? MutationOperationType.REMOVE
                    : MutationOperationType.SET;
            wamService.commit(new MdSyncdMutationEventBuilder()
                    .contentLength(0)
                    .isInBootstrap(false)
                    .isUsingLid(false)
                    .mutationBundle(MutationBundleType.PATCH)
                    .mutationDirection(MutationDirectionType.OUTGOING)
                    .mutationMac(mutationMac)
                    .mutationName(mutationName)
                    .mutationOperation(mutationOperation)
                    .seqNumber(seqNumberInt)
                    .syncdCollection(syncdCollection)
                    .syncdKeyhash("")
                    .syncdKeyid("")
                    .build());
        }
    }

    /**
     * Commits the per-bundle
     * {@link MdSyncdBundleEventBuilder MdSyncdBundle} and per-collection
     * {@link MdSyncdMutationsSummaryEventBuilder MdSyncdMutationsSummary}
     * telemetry for a single processed snapshot or patch.
     *
     * <p>Called from the snapshot and patch branches of
     * {@link #handleSyncResponseInternal} once the LT-Hash has been recomputed
     * (so {@code computedLtHash} is available) but before the collection version
     * is advanced. The bundle event records the wire and computed MACs, the
     * bundle version, and the encoded byte counts; the summary event records the
     * per-mutation-name histograms of set, remove, and LID-scoped mutations for
     * the same bundle. Both are skipped when {@code mutations} is empty, since
     * an empty bundle carries no mutation identity to report.
     *
     * @implNote
     * This implementation has no per-call-site AB allowlist gate; WhatsApp Web
     * gates the bundle event on the {@code md_syncd_bundle_logging} list and the
     * summary event on {@code md_syncd_mutation_summary_logging}, but Cobalt's
     * WAM pipeline filters upstream. The {@code kmpSyncdFlow} is always reported
     * as {@link KmpSyncdFlowEnum#NONE} because Cobalt drives a single non-KMP
     * app-state path, and both {@code syncdKeyhash}/{@code syncdKeyid} on the
     * bundle are left empty for the same key-derivation reason as
     * {@link #emitSyncdMutationWamEvents}. The {@code patchSize} and
     * {@code snapshotSize} carry the mutation count of the bundle, matching
     * WhatsApp Web's outgoing accumulator which reports the mutation-array
     * length rather than the serialized byte size.
     *
     * @param collectionName the collection the bundle belongs to
     * @param seqNumber      the snapshot or patch version number
     * @param direction      {@link MutationDirectionType#INCOMING} for
     *                       snapshot/patch apply
     * @param bundle         {@link MutationBundleType#SNAPSHOT} for the
     *                       snapshot branch,
     *                       {@link MutationBundleType#PATCH} for the patch
     *                       branch
     * @param isInBootstrap  {@code true} when the collection had not yet
     *                       been bootstrapped before this round
     * @param mutations      the decrypted mutations carried by the bundle
     * @param computedLtHash the LT-Hash recomputed over the bundle's
     *                       mutations
     * @param wireExpectedMac the wire snapshot MAC for a snapshot bundle or
     *                       the wire patch MAC for a patch bundle
     * @param snapshotMac    the snapshot MAC to report; the snapshot's own
     *                       MAC for a snapshot bundle or the patch's carried
     *                       snapshot MAC for a patch bundle
     * @param patchMac       the wire patch MAC for a patch bundle, or
     *                       {@code null} for a snapshot bundle
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdWamReportingUtils", exports = "reportSyncdWamAccumulator", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitSyncdBundleAndSummaryWamEvents(
            SyncPatchType collectionName,
            long seqNumber,
            MutationDirectionType direction,
            MutationBundleType bundle,
            boolean isInBootstrap,
            SequencedCollection<DecryptedMutation.Untrusted> mutations,
            byte[] computedLtHash,
            byte[] wireExpectedMac,
            byte[] snapshotMac,
            byte[] patchMac) {
        if (mutations.isEmpty()) {
            return;
        }
        var syncdCollection = mapSyncdCollectionType(collectionName);
        var isPatch = bundle == MutationBundleType.PATCH;

        var bundleBuilder = new MdSyncdBundleEventBuilder()
                .appSessionId(wamAppSessionId)
                .companionSessionIds(wamCompanionSessionId)
                .bundleVersion(seqNumber)
                .seqNumber(seqNumber)
                .kmpSyncdFlow(KmpSyncdFlowEnum.NONE)
                .mutationBundle(bundle)
                .mutationDirection(direction)
                .computedLthash(encodeWamMac(computedLtHash))
                .expectedMac(encodeWamMac(wireExpectedMac))
                .snapshotMac(encodeWamMac(snapshotMac))
                .syncdCollection(syncdCollection)
                .syncdKeyhash("")
                .syncdKeyid("");
        if (isPatch) {
            bundleBuilder.patchSize(mutations.size());
            if (patchMac != null && patchMac.length != 0) {
                bundleBuilder.patchMac(encodeWamMac(patchMac));
            }
        } else {
            bundleBuilder.snapshotSize(mutations.size());
        }
        wamService.commit(bundleBuilder.build());

        var setMutations = new LinkedHashMap<String, Integer>();
        var removeMutations = new LinkedHashMap<String, Integer>();
        var lidMutations = new LinkedHashMap<String, Integer>();
        var keyidKeyhash = new LinkedHashMap<String, String>();
        for (var mutation : mutations) {
            var mutationName = SyncdIndexUtils
                    .getMutationNameFromIndex(collectionName.toString(), mutation.index());
            if (mutationName == null || mutationName.isBlank()) {
                mutationName = "unknown";
            }
            if (mutation.operation() == SyncdOperation.SET) {
                setMutations.merge(mutationName, 1, Integer::sum);
            } else {
                removeMutations.merge(mutationName, 1, Integer::sum);
            }
            if (mutation.index().contains("@lid")) {
                lidMutations.merge(mutationName, 1, Integer::sum);
            }
            var keyId = mutation.keyId();
            if (keyId != null && keyId.length != 0) {
                keyidKeyhash.putIfAbsent(encodeWamMac(keyId), "");
            }
        }

        var summaryBuilder = new MdSyncdMutationsSummaryEventBuilder()
                .appSessionId(wamAppSessionId)
                .companionSessionIds(wamCompanionSessionId)
                .isInBootstrap(isInBootstrap)
                .mutationBundle(bundle)
                .mutationDirection(direction)
                .seqNumber(seqNumber)
                .setMutations(JSON.toJSONString(setMutations))
                .removeMutations(JSON.toJSONString(removeMutations))
                .lidMutations(JSON.toJSONString(lidMutations))
                .snapshotMac(encodeWamMac(snapshotMac))
                .syncdCollection(syncdCollection)
                .syncdKeyidKeyhash(JSON.toJSONString(keyidKeyhash));
        if (isPatch && patchMac != null && patchMac.length != 0) {
            summaryBuilder.patchMac(encodeWamMac(patchMac));
        }
        wamService.commit(summaryBuilder.build());
    }

    /**
     * Encodes a MAC or key blob for WAM telemetry, returning the empty string
     * for a {@code null} or empty input.
     *
     * <p>Uses the unpadded URL-safe Base64 alphabet that WhatsApp Web applies
     * to every MAC, hash, and key identifier it ships in syncd telemetry.
     *
     * @param value the raw bytes to encode, or {@code null}
     * @return the unpadded URL-safe Base64 encoding, or the empty string when
     *         {@code value} is {@code null} or empty
     */
    private static String encodeWamMac(byte[] value) {
        if (value == null || value.length == 0) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    /**
     * Finalizes a successful outgoing patch upload by persisting sync action
     * entries, advancing the collection version and LT-Hash, and dropping the
     * pending mutations that the server acknowledged.
     *
     * <p>Called from {@link #syncCollection} and
     * {@link #syncCollectionsBatched} after the server returns success for an
     * upload IQ. The version check guards against the race where the server's
     * reported version disagrees with the locally computed expected version
     * (which would indicate that another device pushed a competing patch
     * concurrently).
     *
     * @implNote
     * This implementation stores REMOVE mutations implicitly via
     * {@link LinkedWhatsAppSyncStore#removeSyncActionEntry} rather than keeping a
     * tombstone row, and omits the {@code SyncActionEntry} collection,
     * timestamp, and action fields because they are derived on demand from the
     * patch-type key, the action value's timestamp, and the action value
     * itself.
     *
     * @param uploadInfo the upload metadata captured during request
     *                   building
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandlerTypesConverter", exports = "encryptedUploadMutationsToSyncActions", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandlerTypesConverter", exports = "setMutationToSyncAction", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandlerTypesConverter", exports = "syncActionToSyncData", adaptation = WhatsAppAdaptation.ADAPTED)
    private void processUploadSuccess(SyncRequest.UploadedPatchInfo uploadInfo) {
        var patchType = uploadInfo.patchType();
        var expectedVersion = uploadInfo.newVersion();

        var currentVersion = getCurrentVersion(patchType);
        if (expectedVersion != currentVersion + 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "unexpected version after upload for {0}: expected {1} but computed {2}", patchType, currentVersion + 1, expectedVersion);
            syncdDailyStats.uploadConflictCount.incrementAndGet();
            scheduleAppStateSyncDailyFlush();
            return;
        }

        var uploadApplied = updateCollectionState(patchType, expectedVersion, uploadInfo.newLtHash());
        if (!uploadApplied) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "upload acknowledged for {0}: version={1}, mutations={2}", patchType, expectedVersion, uploadInfo.mutations().size());

        for (var mutation : uploadInfo.mutations()) {
            if (mutation.operation() == SyncdOperation.SET) {
                store.syncStore().putSyncActionEntry(patchType, mutation.indexMac(), new SyncActionEntryBuilder()
                        .indexMac(mutation.indexMac())
                        .valueMac(mutation.valueMac())
                        .keyId(mutation.keyId())
                        .actionIndex(mutation.actionIndex())
                        .actionValue(mutation.actionValue())
                        .actionVersion(mutation.actionVersion())
                        .actionState(SyncActionState.SUCCESS)
                        .modelType(resolveActionNameSafe(new DecryptedMutation.Trusted(
                                mutation.actionIndex(),
                                mutation.actionValue(),
                                mutation.operation(),
                                mutation.actionValue().timestamp().orElse(Instant.EPOCH),
                                mutation.actionVersion()
                        )))
                        .modelId(extractModelId(mutation.actionIndex()))
                        .build());
            } else {
                store.syncStore().removeSyncActionEntry(patchType, mutation.indexMac());
            }
        }

        emitSyncdMutationWamEventsForUpload(patchType, expectedVersion, uploadInfo.mutations());

        var uploadedPendingMutationIds = new HashSet<>(uploadInfo.uploadedPendingMutationIds());
        if (uploadedPendingMutationIds.isEmpty()) {
            return;
        }
        whatsapp.store().syncStore().removePendingMutations(patchType, uploadedPendingMutationIds);
    }

    /**
     * Reorders decrypted mutations from a single patch so that
     * {@link SyncdOperation#REMOVE} entries come first and any {@code REMOVE}
     * shadowed by a later {@link SyncdOperation#SET} on the same index is
     * dropped.
     *
     * <p>Called on the in-patch mutation list just before
     * {@link #computeNewLTHash(SyncPatchType, byte[], SequencedCollection)} so
     * the LT-Hash subtract-then-add sequence sees REMOVEs ahead of SETs and
     * never tries to subtract a value MAC that the same patch is about to add
     * fresh; this preserves the net "SET wins" semantics of single-patch
     * ordering.
     *
     * @param mutations the decrypted mutations from a single patch
     * @return the deduplicated and ordered mutations
     */
    private SequencedCollection<DecryptedMutation.Untrusted> deduplicateAndOrder(SequencedCollection<DecryptedMutation.Untrusted> mutations) {
        var setIndices = new HashSet<String>();
        for (var mutation : mutations) {
            if (mutation.operation() == SyncdOperation.SET) {
                setIndices.add(mutation.index());
            }
        }

        var removes = new ArrayList<DecryptedMutation.Untrusted>();
        var sets = new ArrayList<DecryptedMutation.Untrusted>();
        for (var mutation : mutations) {
            if (mutation.operation() == SyncdOperation.SET) {
                sets.add(mutation);
            } else if (!setIndices.contains(mutation.index())) {
                removes.add(mutation);
            }
        }

        var result = new ArrayList<DecryptedMutation.Untrusted>(removes.size() + sets.size());
        result.addAll(removes);
        result.addAll(sets);
        return result;
    }

    /**
     * Rejects mutation batches in which the same index appears more than once
     * under the same operation.
     *
     * <p>Called from the patch and snapshot ingest paths before
     * {@link #computeNewLTHash(SyncPatchType, byte[], SequencedCollection)}; a
     * duplicate index within a single payload is the canary for a server-side
     * or sender-side encoding bug, since the LT-Hash math depends on each
     * index appearing at most once per direction. The {@code fatal} flag
     * distinguishes patches (fatal) from snapshots (downgraded to a warning).
     *
     * @implNote
     * This implementation uses a boolean rather than a three-arm switch
     * because Cobalt only ever invokes this helper on server-delivered patches
     * and snapshots, never on locally generated mutations.
     *
     * @param collectionName the collection being processed
     * @param mutations      the decrypted mutations to validate
     * @param fatal          {@code true} to raise
     *                       {@link WhatsAppWebAppStateSyncException.DuplicateIndexInPatch}
     *                       on a duplicate (patch path); {@code false} to
     *                       log a warning and continue (snapshot path)
     * @throws WhatsAppWebAppStateSyncException.DuplicateIndexInPatch if
     *                       {@code fatal} is {@code true} and a duplicate
     *                       index is observed
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdValidateMutations", exports = "validateNoSameIndexForMultipleMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    private void validateNoDuplicateIndices(
            SyncPatchType collectionName,
            SequencedCollection<DecryptedMutation.Untrusted> mutations,
            boolean fatal
    ) {
        var setIndices = new HashSet<String>();
        var removeIndices = new HashSet<String>();
        for (var mutation : mutations) {
            var index = mutation.index();
            var isDuplicate = mutation.operation() == SyncdOperation.SET
                    ? !setIndices.add(index)
                    : !removeIndices.add(index);
            if (isDuplicate) {
                if (fatal) {
                    throw new WhatsAppWebAppStateSyncException.DuplicateIndexInPatch(collectionName);
                } else {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "duplicate index in snapshot for collection {0}", collectionName);
                }
            }
        }
    }

    /**
     * Rejects a server sync response in which two patches share the same
     * version within a single collection.
     *
     * <p>Called from the patch ingest path before any patch is applied. A
     * duplicate version is a server corruption signal that would otherwise
     * collide LT-Hash computations and corrupt the local collection state;
     * raising {@link WhatsAppWebAppStateSyncException.DuplicatePatchVersion}
     * aborts the round-trip and lets the pluggable error handler decide
     * whether to retry the collection.
     *
     * @param collectionName the collection being processed
     * @param patches        the patches to validate
     * @throws WhatsAppWebAppStateSyncException.DuplicatePatchVersion if
     *                       two patches carry the same version
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdValidateMutations", exports = "validateNoDuplicatePatchVersionInCollection", adaptation = WhatsAppAdaptation.ADAPTED)
    private void validateNoDuplicatePatchVersions(SyncPatchType collectionName, List<SyncdPatch> patches) {
        var seenVersions = new HashSet<Long>();
        for (var patch : patches) {
            var version = patch.version()
                    .map(v -> v.version().orElse(0L))
                    .orElse(0L);
            if (!seenVersions.add(version)) {
                throw new WhatsAppWebAppStateSyncException.DuplicatePatchVersion(collectionName, version);
            }
        }
    }

    /**
     * Returns the locally persisted version counter for the specified
     * collection.
     *
     * <p>Used by the patch ingest path to enforce monotonic version order
     * before each apply and by the upload path to compute the expected
     * version that the server must echo back; bootstrap collections return
     * {@code 0} until the first snapshot lands.
     *
     * @implNote
     * This implementation reads a single in-memory lookup via
     * {@link LinkedWhatsAppSyncStore#findWebAppState}; callers that need a bulk view
     * (such as the resume path) iterate {@link SyncPatchType#values()}
     * themselves rather than going through a dedicated bulk API.
     *
     * @param patchType the collection to query
     * @return the current version number, or {@code 0} if the collection
     *         has never been synced
     */
    @WhatsAppWebExport(moduleName = "WAWebGetCollectionVersion", exports = "getCollectionVersionInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGetCollectionVersion", exports = "bulkGetCollectionVersionsInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGetCollectionVersion", exports = "getAllCollectionVersionsInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    private long getCurrentVersion(SyncPatchType patchType) {
        return store.syncStore().findWebAppState(patchType).version();
    }

    /**
     * Returns the locally persisted LT-Hash for the specified collection,
     * falling back to {@link MutationLTHash#EMPTY_HASH} when the collection
     * has never been seeded.
     *
     * <p>Used as the base value when
     * {@link #computeNewLTHash(SyncPatchType, byte[], SequencedCollection)}
     * folds a fresh batch of mutations into a new hash; the empty-hash
     * fallback lets the first snapshot land on a freshly initialised
     * collection without a spurious mismatch.
     *
     * @param patchType the collection to query
     * @return the current LT-Hash bytes, or {@link MutationLTHash#EMPTY_HASH}
     *         if no hash state exists
     */
    @WhatsAppWebExport(moduleName = "WAWebGetCollectionVersion", exports = "getCollectionVersionLtHashInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    private byte[] getCurrentLTHash(SyncPatchType patchType) {
        var metadata = store.syncStore().findWebAppState(patchType);
        return metadata.ltHash() != null ? metadata.ltHash() : MutationLTHash.EMPTY_HASH;
    }

    /**
     * Downloads, decodes, and validates a {@link SyncdSnapshot} from the CDN
     * reference embedded in a server sync response.
     *
     * <p>Used during bootstrap and during snapshot recovery when the server
     * returns an external snapshot pointer instead of inline records;
     * caller-side MAC and key validation runs separately in
     * {@link MutationIntegrityVerifier#verifySnapshotMac}.
     *
     * @implNote
     * This implementation classifies the download failure into one of two
     * exception flavours: a CDN 404 / not-found surfaces as a fatal
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError} with message
     * {@code "external patch expired"}, while every other download failure
     * surfaces as
     * {@link WhatsAppWebAppStateSyncException.ExternalDownloadFailed} so the
     * retry scheduler can re-queue the collection.
     *
     * @param snapshotRef the external blob reference for the snapshot
     * @return the decoded snapshot
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if the
     *         blob reference is invalid, decoding fails, or the CDN
     *         reports the blob is gone
     * @throws WhatsAppWebAppStateSyncException.ExternalDownloadFailed if
     *         the download fails with a retryable error
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMMSDownload", exports = "downloadSnapshot", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdNetCallbacksApi", exports = "downloadSyncBlob", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdDecode", exports = "decodeSyncdSnapshot", adaptation = WhatsAppAdaptation.ADAPTED)
    private SyncdSnapshot downloadAndDecodeSnapshot(ExternalBlobReference snapshotRef) {
        validateExternalBlobReference(snapshotRef);

        var downloadStart = Instant.now();
        try {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "downloading app-state snapshot blob");
            var downloadedData = mediaConnectionService.download(snapshotRef);
            try (var protobufStream = new BufferedProtobufInputStream(downloadedData)) {
                var decoded = SyncdSnapshotSpec.decode(protobufStream);
                commitMediaDownload2Success(downloadStart);
                return decoded;
            } catch (Throwable throwable) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError("Failed to decode snapshot", throwable);
            }
        } catch (Throwable throwable) {
            if (throwable instanceof WhatsAppWebAppStateSyncException exception) {
                throw exception;
            }
            commitMediaDownload2Failure(downloadStart, throwable);
            if (isExternalBlobNotFound(throwable)) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError("external patch expired", throwable);
            }
            throw new WhatsAppWebAppStateSyncException.ExternalDownloadFailed(throwable);
        }
    }

    /**
     * Wraps every record from a decoded {@link SyncdSnapshot} as a synthetic
     * {@link SyncdOperation#SET} mutation.
     *
     * <p>Called by the snapshot ingest path so the same downstream pipeline
     * ({@link #decryptMutations}, {@link #computeNewLTHash(SyncPatchType, byte[], SequencedCollection)},
     * {@link #applyMutations}) can process snapshots and patches without
     * branching; a snapshot represents the full state at a given version, so
     * every record is by definition a SET.
     *
     * @param snapshot the decoded snapshot
     * @return the mutations derived from the snapshot records
     */
    private SequencedCollection<SyncdMutation> getMutationsFromSnapshot(SyncdSnapshot snapshot) {
        var result = new ArrayList<SyncdMutation>();
        for (var record : snapshot.records()) {
            var sync = new SyncdMutationBuilder()
                    .operation(SyncdOperation.SET)
                    .record(record)
                    .build();
            result.add(sync);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Converts a recovery snapshot delivered by the primary device into
     * trusted mutations and seeds the local sync action entry store with the
     * recovered records.
     *
     * <p>Called when a snapshot MAC mismatch on the companion triggers the
     * peer-recovery path: the primary ships its own decrypted view of the
     * collection, the companion re-derives index MACs locally with the same
     * sync key, trusts the primary's value MACs and LT-Hash, and replaces the
     * local state in one shot.
     *
     * @implNote
     * This implementation also calls
     * {@link #updateCollectionState(SyncPatchType, long, byte[])} with the
     * recovery payload's version and LT-Hash so the caller can apply the
     * returned mutations through the standard handler pipeline without an
     * extra version bump; the call-site has no other use for the raw recovery
     * fields, so the update is folded in here.
     *
     * @param collectionName    the collection being recovered
     * @param recoveredSnapshot the decoded recovery snapshot from the
     *                          primary device
     * @return the trusted mutations extracted from the recovery data
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if a
     *         required field on the recovery payload is missing
     * @throws WhatsAppWebAppStateSyncException.MissingKey if the
     *         referenced sync key is not available locally
     * @throws WhatsAppWebAppStateSyncException.DecryptionFailed if
     *         index-MAC re-derivation fails
     * @throws WhatsAppWebAppStateSyncException.MissingActionTimestamp if
     *         the recovery record carries no action timestamp
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandlerTypesConverter", exports = "syncActionsToDecryptedMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    private SequencedCollection<DecryptedMutation.Trusted> processRecoveredSnapshot(
            SyncPatchType collectionName,
            SyncdSnapshotRecovery recoveredSnapshot
    ) {
        var records = recoveredSnapshot.mutationRecords();
        var trusted = new ArrayList<DecryptedMutation.Trusted>(records.size());

        for (var record : records) {
            var actionData = record.value()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Recovery record missing SyncActionData", null));
            var keyId = record.keyId()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Recovery record missing key ID", null));
            var valueMac = record.mac()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Recovery record missing value MAC", null));
            var indexBytes = actionData.index()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Recovery record missing index", null));
            var actionValue = actionData.value()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Recovery record missing action value", null));
            var actionVersion = actionData.version()
                    .orElse(0);
            if(actionVersion <= 0) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError("Recovery record missing action version", null);
            }

            var timestamp = actionValue.timestamp()
                    .orElseThrow(WhatsAppWebAppStateSyncException.MissingActionTimestamp::new);

            var syncKeyData = whatsapp.store().syncStore().findWebAppStateKeyById(keyId)
                    .flatMap(AppStateSyncKey::keyData)
                    .flatMap(AppStateSyncKeyData::keyData)
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.MissingKey(keyId));

            try (var keys = MutationKeys.ofSyncKey(syncKeyData)) {
                var indexMac = Mac.getInstance("HmacSHA256");
                indexMac.init(keys.indexKey());
                var indexMacResult = indexMac.doFinal(indexBytes);

                var indexString = new String(indexBytes, StandardCharsets.UTF_8);
                store.syncStore().putSyncActionEntry(collectionName, indexMacResult, new SyncActionEntryBuilder()
                        .indexMac(indexMacResult)
                        .valueMac(valueMac)
                        .keyId(keyId)
                        .actionIndex(indexString)
                        .actionValue(actionValue)
                        .actionVersion(actionVersion)
                        .build());

                trusted.add(new DecryptedMutation.Trusted(
                        indexString,
                        actionValue,
                        SyncdOperation.SET,
                        timestamp,
                        actionVersion
                ));
            } catch (GeneralSecurityException e) {
                throw new WhatsAppWebAppStateSyncException.DecryptionFailed(e);
            }
        }

        var recoveryVersion = recoveredSnapshot.version()
                .map(entry -> entry.version().orElse(0))
                .filter(version -> version > 0)
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Recovery response missing version for " + collectionName,
                        null
                ));
        var recoveryLtHash = recoveredSnapshot.collectionLthash()
                .filter(hash -> hash.length == MutationLTHash.EMPTY_HASH.length)
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Recovery response missing LT-Hash for " + collectionName,
                        null
                ));
        updateCollectionState(collectionName, recoveryVersion, recoveryLtHash);

        return trusted;
    }

    /**
     * Materialises the raw mutations carried by a {@link SyncdPatch},
     * downloading the external blob when the patch is delivered by reference
     * instead of inline.
     *
     * <p>Called by the patch ingest path before {@link #decryptMutations}; a
     * patch is permitted to carry either an inline mutation list or an
     * external blob reference but never both, so the both-present case is
     * treated as a server protocol violation.
     *
     * @param patch the decoded patch
     * @return the mutations from this patch, inline or downloaded from
     *         the CDN
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if the
     *         patch carries both inline and external mutations
     * @throws WhatsAppWebAppStateSyncException.ExternalDownloadFailed if
     *         the external blob download fails with a retryable error
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMMSDownload", exports = "downloadExternalPatch", adaptation = WhatsAppAdaptation.ADAPTED)
    private SequencedCollection<SyncdMutation> getMutationsFromPatch(SyncdPatch patch) {
        var hasInline = patch.mutations() != null && !patch.mutations().isEmpty();
        var hasExternal = patch.externalMutations().isPresent();

        if (hasInline && hasExternal) {
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "Patch contains both inline and external mutations", null);
        }

        if (hasExternal) {
            var downloadedData = downloadExternalMutation(patch.externalMutations().get());
            var externalMutations = decodeExternalMutation(downloadedData);
            return externalMutations.mutations() != null
                    ? Collections.unmodifiableList(externalMutations.mutations())
                    : List.of();
        }

        return hasInline
                ? Collections.unmodifiableList(patch.mutations())
                : List.of();
    }

    /**
     * Downloads the encrypted blob backing an externally referenced patch via
     * the live media connection.
     *
     * <p>Used by {@link #getMutationsFromPatch} when the server delivers a
     * patch's mutation set by CDN reference rather than inline; the caller
     * passes the returned stream to {@link #decodeExternalMutation} before
     * decryption.
     *
     * @implNote
     * This implementation mirrors {@link #downloadAndDecodeSnapshot} in its
     * error classification: a 404 / not-found surfaces as a fatal
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError} with message
     * {@code "external patch expired"}, while every other failure becomes the
     * retryable
     * {@link WhatsAppWebAppStateSyncException.ExternalDownloadFailed}.
     *
     * @param externalRef the external blob reference
     * @return the downloaded data stream
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if the
     *         blob reference is invalid or the CDN reports the blob is
     *         gone
     * @throws WhatsAppWebAppStateSyncException.ExternalDownloadFailed if
     *         the download fails with a retryable error
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdNetCallbacksApi", exports = "downloadSyncBlob", adaptation = WhatsAppAdaptation.ADAPTED)
    private InputStream downloadExternalMutation(ExternalBlobReference externalRef) {
        validateExternalBlobReference(externalRef);

        var downloadStart = Instant.now();
        try {
            var downloaded = mediaConnectionService.download(externalRef);
            commitMediaDownload2Success(downloadStart);
            return downloaded;
        } catch (Throwable throwable) {
            commitMediaDownload2Failure(downloadStart, throwable);
            if (isExternalBlobNotFound(throwable)) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError("external patch expired", throwable);
            }
            throw new WhatsAppWebAppStateSyncException.ExternalDownloadFailed(throwable);
        }
    }

    /**
     * Commits a successful
     * {@link MediaDownload2EventBuilder MediaDownload2Event} for an app-state
     * CDN blob download.
     *
     * <p>Called from {@link #downloadAndDecodeSnapshot} and
     * {@link #downloadExternalMutation} after the CDN bytes have been read;
     * the event keeps the WAM dashboards' app-state download metrics current.
     *
     * @implNote
     * This implementation hard-codes the WAM dimensions: media type
     * {@link MediaType#MD_APP_STATE}, MMS v4,
     * {@link DownloadOriginType#MESSAGE_HISTORY_SYNC} origin,
     * {@link MediaDownloadModeType#FULL} mode, HTTP 200, single attempt, zero
     * retries; each download commits a self-contained event rather than going
     * through a shared accumulator object.
     *
     * @param downloadStart the instant at which the download attempt
     *                      began
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaDownloadMetrics",
            exports = "createMediaDownloadMetrics",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMediaDownload2Success(Instant downloadStart) {
        var overallT = Instant.ofEpochMilli(Duration.between(downloadStart, Instant.now()).toMillis());
        wamService.commit(new MediaDownload2EventBuilder()
                .overallMediaType(MediaType.MD_APP_STATE)
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
     * {@link MediaDownload2EventBuilder MediaDownload2Event} for an app-state
     * CDN blob download.
     *
     * <p>Called from {@link #downloadAndDecodeSnapshot} and
     * {@link #downloadExternalMutation} on every failed download attempt; the
     * event lets WAM dashboards distinguish network errors, 404s, throttling,
     * and unknown failures via the
     * {@link #classifyMediaDownloadError(Throwable)} mapping.
     *
     * @implNote
     * This implementation populates the same fixed WAM dimensions as
     * {@link #commitMediaDownload2Success(Instant)} and additionally attaches
     * the HTTP status code when {@link #extractHttpStatusCode(Throwable)}
     * surfaces one; the status-code lookup walks the cause chain because the
     * download path wraps the status inside
     * {@link WhatsAppMediaException.Download}.
     *
     * @param downloadStart the instant at which the download attempt
     *                      began
     * @param throwable     the error that aborted the download
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
                .overallMediaType(MediaType.MD_APP_STATE)
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
     * Maps a download failure to the corresponding
     * {@link MediaDownloadResultType} value.
     *
     * <p>Called by {@link #commitMediaDownload2Failure(Instant, Throwable)} to
     * produce the download-result dimension on the emitted WAM event: HTTP
     * 404/410 map to {@link MediaDownloadResultType#ERROR_TOO_OLD}, 416 to
     * {@link MediaDownloadResultType#ERROR_CANNOT_RESUME}, 401 to
     * {@link MediaDownloadResultType#ERROR_INVALID_URL}, 429/507 to
     * {@link MediaDownloadResultType#ERROR_THROTTLE}, a missing status to
     * {@link MediaDownloadResultType#ERROR_NETWORK}, and anything else to
     * {@link MediaDownloadResultType#ERROR_UNKNOWN}.
     *
     * @implNote
     * This implementation walks the cause chain because the download stack
     * wraps the originating {@link WhatsAppMediaException.Download} inside
     * transport-level exceptions before it surfaces here.
     *
     * @param throwable the error raised by the download path
     * @return the mapped result type, never {@code null}
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
     * Returns the HTTP status code embedded in a download failure, if one is
     * available.
     *
     * <p>Called by {@link #commitMediaDownload2Failure(Instant, Throwable)} to
     * populate the HTTP-code dimension on the WAM event so dashboards can
     * break failures down by status code; the symmetric helper to
     * {@link #classifyMediaDownloadError(Throwable)}.
     *
     * @param throwable the error raised by the download path
     * @return the status code, or {@code null} if the cause chain
     *         carries no
     *         {@link WhatsAppMediaException.Download} with a status code
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
     * Returns whether the given download failure represents a CDN not-found /
     * expired blob.
     *
     * <p>Used by {@link #downloadAndDecodeSnapshot} and
     * {@link #downloadExternalMutation} to decide whether to re-throw a
     * download error as the fatal
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError} with message
     * {@code "external patch expired"} or to surface it as the retryable
     * {@link WhatsAppWebAppStateSyncException.ExternalDownloadFailed}.
     *
     * @implNote
     * This implementation walks the cause chain and inspects exception
     * messages because the media download stack does not expose a typed
     * not-found subclass; messages such as {@code "status code 404"} or
     * {@code "not found"} drive the classification. A self-referencing cause
     * is treated as a termination condition to guard against pathological
     * chains.
     *
     * @param throwable the exception thrown by the media download path
     * @return {@code true} if the throwable indicates a 404 / not-found
     *         blob, {@code false} otherwise
     */
    private boolean isExternalBlobNotFound(Throwable throwable) {
        for (var current = throwable; current != null; current = current.getCause()) {
            if (current instanceof WhatsAppMediaException.Download) {
                var message = current.getMessage();
                if (message != null && (message.contains("status code 404") || message.contains("404") && message.toLowerCase().contains("not found"))) {
                    return true;
                }
            }
            var message = current.getMessage();
            if (message != null) {
                var lower = message.toLowerCase();
                if (lower.contains("404") || lower.contains("not found") || lower.contains("notfound")) {
                    return true;
                }
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    /**
     * Asserts that every field required to download an
     * {@link ExternalBlobReference} from the CDN is populated.
     *
     * <p>Called by {@link #downloadAndDecodeSnapshot} and
     * {@link #downloadExternalMutation} before issuing the media download; the
     * ordered field checks (mediaKey, directPath, fileSha256, fileEncSha256)
     * report the first missing field, which downstream WAM dashboards group
     * by.
     *
     * @param ref the external blob reference to validate
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if any
     *         required field is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdValidateServerSyncProtobuf", exports = "validateExternalBlobReference", adaptation = WhatsAppAdaptation.ADAPTED)
    private void validateExternalBlobReference(ExternalBlobReference ref) {
        if (ref.mediaKey().isEmpty() || ref.mediaKey().get().length == 0) {
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "External blob reference missing mediaKey",
                    null
            );
        }
        if (ref.mediaDirectPath().isEmpty() || ref.mediaDirectPath().get().isEmpty()) {
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "External blob reference missing directPath",
                    null
            );
        }
        if (ref.fileSha256().isEmpty()) {
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "External blob reference missing fileSha256",
                    null
            );
        }
        if (ref.fileEncSha256().isEmpty()) {
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "External blob reference missing fileEncSha256",
                    null
            );
        }
    }

    /**
     * Decodes a downloaded external mutation blob into a
     * {@link SyncdMutations} container.
     *
     * <p>Used by {@link #getMutationsFromPatch} after
     * {@link #downloadExternalMutation} has fetched the bytes; the resulting
     * mutations are handed to {@link #decryptMutations} like any other patch
     * payload.
     *
     * @param downloadedData the downloaded data stream
     * @return the decoded mutations container
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if
     *         protobuf decoding fails
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdDecode", exports = "decodeSyncdMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    private SyncdMutations decodeExternalMutation(InputStream downloadedData) {
        try (var protobufStream = new BufferedProtobufInputStream(downloadedData)) {
            return SyncdMutationsSpec.decode(protobufStream);
        } catch (Throwable throwable) {
            throw new WhatsAppWebAppStateSyncException.UnexpectedError("Failed to decode external mutations", throwable);
        }
    }

    /**
     * Decrypts a batch of {@link SyncdMutation} records into
     * {@link DecryptedMutation.Untrusted} values, failing fast with the full set
     * of absent key ids when any referenced sync key is missing.
     *
     * <p>Called by the snapshot and patch ingest paths after
     * {@link #getMutationsFromSnapshot} or {@link #getMutationsFromPatch}
     * produce the raw mutation list. Every absent key id is collected, and the
     * batch surfaces as a single {@link WhatsAppWebAppStateSyncException.MissingKey}
     * carrying all of them. The actual key-share request is issued by
     * {@link #handleSyncError(Throwable, SyncPatchType)} after the collection has
     * been transitioned to {@link SyncCollectionState#BLOCKED}, so the request
     * (a peer-message send) never runs while the syncd monitor is held.
     *
     * @implNote
     * This implementation walks the mutation list twice. The first pass collects
     * every missing key id into a hex-keyed {@link LinkedHashMap} so duplicates
     * are dropped, then throws the batched
     * {@link WhatsAppWebAppStateSyncException.MissingKey} without sending anything
     * itself; deferring the send keeps the decrypt-time key-presence check and
     * the {@link SyncCollectionState#BLOCKED} transition atomic under the monitor,
     * which closes the lost-wakeup window against a concurrent inbound key share.
     * The second pass decrypts each mutation. The REMOVE-with-missing-key branch
     * obeys the {@link ABProp#WEB_REQUEST_MISSING_KEYS_FOR_REMOVES} AB prop:
     * {@code true} routes the same way as SET, {@code false} raises a fatal
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError} with message
     * {@code "no key data for remove mutations"}. The inner catch preserves the
     * original {@link WhatsAppWebAppStateSyncException} subtype so the caller can
     * distinguish MAC mismatches from generic decryption failures.
     *
     * @param mutations the raw mutations to decrypt
     * @return the decrypted untrusted mutations
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if a
     *         required field on a mutation record is missing, or if a
     *         REMOVE references an unknown key while
     *         {@link ABProp#WEB_REQUEST_MISSING_KEYS_FOR_REMOVES} is
     *         disabled
     * @throws WhatsAppWebAppStateSyncException.MissingKey if one or more
     *         required sync keys are not available locally
     * @throws WhatsAppWebAppStateSyncException.DecryptionFailed if
     *         decryption fails for a reason other than missing key data
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdDecryptMutationsWrapper", exports = "tryDecryptSnapshot", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdDecryptMutationsWrapper", exports = "tryDecryptPatch", adaptation = WhatsAppAdaptation.ADAPTED)
    private SequencedCollection<DecryptedMutation.Untrusted> decryptMutations(SequencedCollection<SyncdMutation> mutations) {
        var requestMissingKeysForRemoves = abPropsService.getBool(ABProp.WEB_REQUEST_MISSING_KEYS_FOR_REMOVES);
        var missingKeyIds = new LinkedHashMap<String, byte[]>();
        for (var mutation : mutations) {
            var keyId = mutation.record()
                    .flatMap(SyncdRecord::keyId)
                    .flatMap(KeyId::id)
                    .orElse(null);
            if (keyId != null && whatsapp.store().syncStore().findWebAppStateKeyById(keyId).isEmpty()) {
                var operation = mutation.operation().orElse(null);
                if (operation == SyncdOperation.REMOVE && !requestMissingKeysForRemoves) {
                    throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "no key data for remove mutations", null);
                }
                missingKeyIds.putIfAbsent(HexFormat.of().formatHex(keyId), keyId);
            }
        }
        if (!missingKeyIds.isEmpty()) {
            syncdDailyStats.missingKeyCount.addAndGet(missingKeyIds.size());
            scheduleAppStateSyncDailyFlush();
            throw new WhatsAppWebAppStateSyncException.MissingKey(new ArrayList<>(missingKeyIds.values()));
        }

        var decrypted = new ArrayList<DecryptedMutation.Untrusted>(mutations.size());

        for (var mutation : mutations) {
            var record = mutation.record()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError("Missing record in mutation", null));

            var operation = mutation.operation()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError("Missing operation in mutation", null));

            var recordValueBlob = record.value()
                    .flatMap(SyncdValue::blob)
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError("Missing value blob in mutation record", null));

            var recordIndexBlob = record.index()
                    .flatMap(SyncdIndex::blob)
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError("Missing index blob in mutation record", null));

            var keyId = record.keyId()
                    .flatMap(KeyId::id)
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError("Missing key ID in mutation record", null));
            if (keyId.length == 0) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError("Empty key ID in mutation record", null);
            }

            var syncKey = whatsapp.store().syncStore().findWebAppStateKeyById(keyId)
                    .flatMap(AppStateSyncKey::keyData)
                    .flatMap(AppStateSyncKeyData::keyData)
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.MissingKey(keyId));

            try (var keys = MutationKeys.ofSyncKey(syncKey)) {
                var decryptedMutation = DecryptedMutation.Untrusted.of(
                        recordValueBlob,
                        recordIndexBlob,
                        keys,
                        operation,
                        keyId
                );
                decrypted.add(decryptedMutation);
            } catch (Exception e) {
                if (e instanceof WhatsAppWebAppStateSyncException syncEx) {
                    throw syncEx;
                }
                throw new WhatsAppWebAppStateSyncException.DecryptionFailed(e);
            }
        }

        return Collections.unmodifiableSequencedCollection(decrypted);
    }

    /**
     * Commits one
     * {@link MdAppStateMessageRangeEventBuilder MdAppStateMessageRange} event
     * per chat-action mutation that carries a non-empty per-chat message
     * range, and emits the
     * {@link BootstrapAppStateDataStageCode#MUTATIONS_DECRYPTED} bootstrap
     * stage marker.
     *
     * <p>Called by the snapshot and patch ingest paths immediately after
     * {@link #decryptMutations} returns, so WAM dashboards can correlate
     * per-mutation message-range sizes against the bootstrap stage timeline.
     *
     * @implNote
     * This implementation handles the four chat-action variants
     * ({@link ArchiveChatAction}, {@link MarkChatAsReadAction},
     * {@link ClearChatAction}, {@link DeleteChatAction}) via a pattern-switch;
     * other action types contribute no event.
     *
     * @param untrusted the decrypted mutations to inspect for chat
     *                  action message ranges
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMetricCriticalBootstrapStage", exports = "reportSyncdDecryptedMutations", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebCollectionHandlerWamMutation", exports = "logMetricsForMutationLength", adaptation = WhatsAppAdaptation.DIRECT)
    private void reportDecryptedMutationMessageRanges(SequencedCollection<DecryptedMutation.Untrusted> untrusted) {
        logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.MUTATIONS_DECRYPTED);
        for (var mutation : untrusted) {
            var messageRange = mutation.value().flatMap(sav -> sav.action())
                    .flatMap(action -> switch (action) {
                        case ArchiveChatAction a -> a.messageRange();
                        case MarkChatAsReadAction a -> a.messageRange();
                        case ClearChatAction a -> a.messageRange();
                        case DeleteChatAction a -> a.messageRange();
                        default -> Optional.<SyncActionMessageRange>empty();
                    })
                    .orElse(null);
            if (messageRange == null) {
                continue;
            }
            wamService.commit(new MdAppStateMessageRangeEventBuilder()
                    .additionalMessagesCount(messageRange.messages().size())
                    .build());
        }
    }

    /**
     * Commits a
     * {@link MdBootstrapAppStateCriticalDataProcessingEventBuilder MdBootstrapAppStateCriticalDataProcessing}
     * event when a bootstrap stage is reached while the first-run critical
     * data sync is still in progress.
     *
     * <p>Called at every observable stage transition during the initial
     * app-state sync (mutations decrypted, about-to-apply, applied, entered
     * retry mode, and so on) so the WAM dashboards can chart how long each
     * stage takes; once the {@link SyncPatchType#CRITICAL_BLOCK} collection
     * has been bootstrapped the helper becomes a no-op.
     *
     * @implNote
     * This implementation approximates a global first-run state machine by
     * checking the bootstrapped flag on {@link SyncPatchType#CRITICAL_BLOCK},
     * since Cobalt maintains no separate first-run state. The
     * {@code mdSessionId} field is omitted because Cobalt has no equivalent
     * identity-key-hash derivation.
     *
     * @param stage the bootstrap stage reached; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCriticalBootstrapProcessingApi", exports = "logCriticalBootstrapStageIfNecessary", adaptation = WhatsAppAdaptation.ADAPTED)
    private void logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode stage) {
        if (store.syncStore().findWebAppState(SyncPatchType.CRITICAL_BLOCK).bootstrapped()) {
            return;
        }
        wamService.commit(new MdBootstrapAppStateCriticalDataProcessingEventBuilder()
                .bootstrapAppStateDataStage(stage)
                .mdTimestamp((int) System.currentTimeMillis())
                .build());
    }

    /**
     * Commits the
     * {@link MdBootstrapAppStateDataDownloadedEventBuilder MdBootstrapAppStateDataDownloaded}
     * WAM event summarising a first-time collection's snapshot and
     * external-patch download phase.
     *
     * <p>Called from the snapshot ingest path only when the collection was not
     * previously bootstrapped, immediately after every CDN download for the
     * collection has either completed or failed; lets WAM dashboards chart
     * per-collection bootstrap latency, payload size, and success rate against
     * the {@link MdBootstrapPayloadType#CRITICAL CRITICAL} versus
     * {@link MdBootstrapPayloadType#NON_CRITICAL NON_CRITICAL} dimension.
     *
     * @implNote
     * This implementation omits the storage-quota fields because there is no
     * JVM equivalent of the browser storage estimate, and omits
     * {@code mdSessionId} for the same reason as
     * {@link #logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode)}.
     * The {@code failure} parameter is accepted for symmetry with the failure
     * branch but is not folded into a failure-reason field because the WAM
     * event never writes that field.
     *
     * @param tracker the download tracker carrying start timestamp and
     *                accumulated payload size
     * @param result  the step result; {@link MdBootstrapStepResult#SUCCESS}
     *                if every download completed, otherwise
     *                {@link MdBootstrapStepResult#FAILURE}
     * @param failure the throwable that caused the failure, or
     *                {@code null} on success
     */
    @WhatsAppWebExport(moduleName = "WAWebCollectionHandlerWamSyncUtil", exports = "commitBootstrapAppStateDownloadMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdMetrics", exports = "reportSyncdBootstrapAppStateDownloadMetric", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitBootstrapAppStateDataDownloaded(
            BootstrapDownloadTracker tracker,
            MdBootstrapStepResult result,
            Throwable failure
    ) {
        var now = System.currentTimeMillis();
        var duration = now - tracker.startTs();
        var builder = new MdBootstrapAppStateDataDownloadedEventBuilder()
                .mdBootstrapPayloadType(tracker.collectionName().isCritical()
                        ? MdBootstrapPayloadType.CRITICAL
                        : MdBootstrapPayloadType.NON_CRITICAL)
                .mdTimestamp((int) now)
                .mdBootstrapStepDuration((int) duration)
                .mdBootstrapStepResult(result);
        if (tracker.totalBytes() > 0 && tracker.totalBytes() <= Integer.MAX_VALUE) {
            builder.mdBootstrapPayloadSize((int) tracker.totalBytes());
        }
        assert failure == null || result == MdBootstrapStepResult.FAILURE;
        wamService.commit(builder.build());
    }

    /**
     * Mutable accumulator that pairs a collection name, a start timestamp, and
     * a running payload-size tally for the bootstrap download metric.
     *
     * <p>Held by the snapshot ingest path across the snapshot and
     * external-patch download phase of a single sync response so the eventual
     * call to
     * {@link #emitBootstrapAppStateDataDownloaded(BootstrapDownloadTracker, MdBootstrapStepResult, Throwable)}
     * can fill the step-duration and payload-size dimensions with consistent
     * inputs.
     *
     * @implNote
     * This implementation captures the start timestamp eagerly in the
     * constructor so it bounds the full download window; per-blob sizes accrue
     * via {@link #addBytes(long)}.
     */
    private static final class BootstrapDownloadTracker {
        /**
         * The collection currently being downloaded; feeds the
         * {@code mdBootstrapPayloadType} dimension on the emitted WAM
         * event via {@link SyncPatchType#isCritical()}.
         */
        private final SyncPatchType collectionName;

        /**
         * The unix timestamp in milliseconds at which the download
         * phase began; subtracted from the commit time to compute the
         * {@code mdBootstrapStepDuration} dimension.
         */
        private final long startTs;

        /**
         * The running sum of {@code fileSizeBytes} across every
         * external blob downloaded so far for this tracker.
         */
        private long totalBytes;

        /**
         * Constructs a tracker pinned to {@code collectionName} with a start
         * timestamp of {@link System#currentTimeMillis()} and a zero byte
         * accumulator.
         *
         * <p>Built once per collection bootstrap round; the start timestamp is
         * captured before the first CDN download so the measured window covers
         * the whole download phase.
         *
         * @param collectionName the collection being downloaded
         */
        private BootstrapDownloadTracker(SyncPatchType collectionName) {
            this.collectionName = collectionName;
            this.startTs = System.currentTimeMillis();
        }

        /**
         * Returns the collection this tracker was created for.
         *
         * @return the collection name
         */
        private SyncPatchType collectionName() {
            return collectionName;
        }

        /**
         * Returns the unix timestamp (in milliseconds) captured at
         * construction.
         *
         * @return the start timestamp in milliseconds
         */
        private long startTs() {
            return startTs;
        }

        /**
         * Returns the running sum of external blob sizes accumulated
         * so far.
         *
         * @return the total downloaded bytes
         */
        private long totalBytes() {
            return totalBytes;
        }

        /**
         * Adds a single blob's plaintext size to the running total.
         *
         * <p>Called once per downloaded external blob; non-positive sizes are
         * ignored so a missing {@code fileSizeBytes} field on a blob reference
         * cannot corrupt the running total.
         *
         * @param bytes the {@code fileSizeBytes} value from an external
         *              blob reference
         */
        private void addBytes(long bytes) {
            if (bytes > 0) {
                totalBytes += bytes;
            }
        }
    }

    /**
     * Applies a batch of trusted mutations to the store, grouped by action
     * name and dispatched through the corresponding handler in
     * {@link WebAppStateHandlerRegistry}.
     *
     * <p>Called from the snapshot, patch, and recovery ingest paths after
     * mutations have been decrypted, integrity-checked, and
     * conflict-resolved. It emits the
     * {@link BootstrapAppStateDataStageCode#ABOUT_TO_APPLY_MUTATIONS} and
     * {@link BootstrapAppStateDataStageCode#APPLIED_MUTATIONS} bootstrap stage
     * markers, persists each mutation's {@link MutationApplicationResult},
     * queues orphan results into the per-collection orphan list, and triggers
     * a single {@link #retryOrphanMutations(SyncPatchType)} round at the end
     * so orphans whose target now exists can land.
     *
     * @implNote
     * This implementation aborts the entire collection on a
     * {@link WhatsAppWebAppStateSyncException} that reports {@code isFatal()}
     * or any throwable arising in {@link SyncPatchType#CRITICAL_BLOCK}; any
     * other handler failure marks the affected batch
     * {@link MutationApplicationResult#failed()} and continues, so a single
     * broken handler cannot poison the whole collection. Mutations whose
     * action version exceeds the registered handler's version cap are recorded
     * as {@link MutationApplicationResult#unsupported()}.
     *
     * @param collectionName  the collection these mutations belong to
     * @param remoteMutations the trusted mutations to apply
     * @throws WhatsAppWebAppStateSyncException if a handler raises a
     *         fatal sync error or fails while processing the
     *         {@link SyncPatchType#CRITICAL_BLOCK} collection
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandler", exports = "Xe", adaptation = WhatsAppAdaptation.ADAPTED)
    private void applyMutations(SyncPatchType collectionName, SequencedCollection<DecryptedMutation.Trusted> remoteMutations) {
        logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.ABOUT_TO_APPLY_MUTATIONS);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "applying {0} mutations for collection {1}", remoteMutations.size(), collectionName);
        syncdDailyStats.mutationCount.addAndGet(remoteMutations.size());
        var mutationsToApply = resolveConflicts(remoteMutations, collectionName);

        var mutationsByAction = new HashMap<String, List<DecryptedMutation.Trusted>>();

        for (var mutation : mutationsToApply) {
            var actionName = resolveActionName(mutation);
            if (actionName == null) {
                syncdDailyStats.unsetActionCount.incrementAndGet();
                recordMutationState(collectionName, mutation, null, MutationApplicationResult.unsupported());
                continue;
            }
            mutationsByAction
                    .computeIfAbsent(actionName, _ -> new ArrayList<>())
                    .add(mutation);
        }

        for (var entry : mutationsByAction.entrySet()) {
            var handler = handlerRegistry.findHandler(entry.getKey());
            if (handler.isEmpty()) {
                for (var mutation : entry.getValue()) {
                    recordMutationState(collectionName, mutation, entry.getKey(), MutationApplicationResult.unsupported());
                }
                continue;
            }

            var maxVersion = handler.get().version();
            var versionGated = new ArrayList<DecryptedMutation.Trusted>(entry.getValue().size());
            for (var mutation : entry.getValue()) {
                if (mutation.actionVersion() > maxVersion) {
                    recordMutationState(collectionName, mutation, entry.getKey(), MutationApplicationResult.unsupported());
                    continue;
                }
                versionGated.add(mutation);
            }
            if (versionGated.isEmpty()) {
                continue;
            }

            List<MutationApplicationResult> batchResults = null;
            var handlerFailed = false;
            try {
                batchResults = handler.get().applyMutationBatch(whatsapp, versionGated);
            } catch (WhatsAppWebAppStateSyncException exception) {
                if (exception.isFatal() || collectionName == SyncPatchType.CRITICAL_BLOCK) {
                    throw exception;
                }
                handlerFailed = true;
                if (Log.WARNING) LOGGER.log(Level.WARNING, "handler failed for action " + entry.getKey() + " in " + collectionName, exception);
            } catch (Throwable throwable) {
                if (collectionName == SyncPatchType.CRITICAL_BLOCK) {
                    throw new WhatsAppWebAppStateSyncException.UnexpectedError(throwable);
                }
                handlerFailed = true;
                if (Log.WARNING) LOGGER.log(Level.WARNING, "handler failed for action " + entry.getKey() + " in " + collectionName, throwable);
            }
            for (var i = 0; i < versionGated.size(); i++) {
                var mutation = versionGated.get(i);
                var result = handlerFailed
                        ? MutationApplicationResult.failed()
                        : batchResults.get(i);
                recordMutationState(collectionName, mutation, entry.getKey(), result);
                if (!handlerFailed) {
                    notifyWebAppStateAction(mutation);
                }
                if (!handlerFailed && result.isOrphan()) {
                    store.syncStore().addOrphanMutation(
                            collectionName,
                            buildOrphanEntry(
                                    mutation,
                                    result.modelType() != null ? result.modelType() : entry.getKey(),
                                    result.modelId()
                            )
                    );
                }
            }
        }

        logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.APPLIED_MUTATIONS);

        scheduleAppStateSyncDailyFlush();

        retryOrphanMutations(collectionName);
    }

    /**
     * Re-applies every orphan mutation previously parked for the given
     * collection.
     *
     * <p>Called at the tail of {@link #applyMutations} so any mutation whose
     * target entity was just introduced by the same sync round has a chance to
     * land; mutations that still cannot apply are re-added by
     * {@link #retryOrphanEntries(SyncPatchType, List)} and remain eligible for
     * the next retry round.
     *
     * @param collectionName the collection whose orphan list to retry
     */
    private void retryOrphanMutations(SyncPatchType collectionName) {
        coordinator.runLocked(() -> {
            var orphans = store.syncStore().findOrphanMutations(collectionName);
            if (orphans.isEmpty()) {
                return;
            }

            store.syncStore().removeOrphanMutations(collectionName);
            retryOrphanEntries(collectionName, orphans);
        });
    }

    /**
     * Re-applies the given orphan entries oldest-first, requeueing any that
     * still cannot land.
     *
     * <p>Called by {@link #retryOrphanMutations(SyncPatchType)} and by
     * {@link #retryUnsupportedMutations(SyncPatchType)} to drive individual
     * orphan retries.
     *
     * @implNote
     * This implementation sorts by {@link OrphanMutationEntry#timestamp()}
     * before applying so that later orphans targeting the same index overwrite
     * earlier ones. {@link #resolveActionNameSafe(DecryptedMutation.Trusted)}
     * is used rather than the throwing variant because a partially decoded
     * orphan should be re-parked rather than escalate.
     *
     * @param collectionName the collection these orphans belong to
     * @param orphans        the orphan entries to retry
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCollectionHandler", exports = "applyIndividualMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    private void retryOrphanEntries(SyncPatchType collectionName, List<OrphanMutationEntry> orphans) {
        var sortedOrphans = new ArrayList<>(orphans);
        sortedOrphans.sort(Comparator.comparing(OrphanMutationEntry::timestamp));
        for (var orphan : sortedOrphans) {
            var mutation = new DecryptedMutation.Trusted(
                    orphan.index(),
                    orphan.value(),
                    orphan.operation(),
                    orphan.timestamp(),
                    orphan.actionVersion()
            );
            var actionName = resolveActionNameSafe(mutation);
            if (actionName == null) {
                store.syncStore().addOrphanMutation(collectionName, orphan);
                continue;
            }
            var handler = handlerRegistry.findHandler(actionName);
            if (handler.isEmpty()) {
                store.syncStore().addOrphanMutation(collectionName, orphan);
                continue;
            }

            try {
                var result = handler.get().applyMutation(whatsapp, mutation);
                if (result.isOrphan()) {
                    store.syncStore().addOrphanMutation(collectionName, orphan);
                } else {
                    recordMutationState(collectionName, mutation, actionName, result);
                }
            } catch (Throwable throwable) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "orphan mutation retry failed for action " + actionName + " in " + collectionName, throwable);
            }
        }
    }

    /**
     * Builds an {@link OrphanMutationEntry} for the given trusted mutation,
     * deriving its model id from the mutation index when no explicit override
     * is provided.
     *
     * <p>Used by {@link #applyMutations} when a handler reports
     * {@link MutationApplicationResult#isOrphan()}; the resulting entry is
     * keyed on model type and model id so the targeted orphan-resolution
     * helpers ({@link #checkOrphanMessages(java.util.Collection)},
     * {@link #checkOrphanChats(java.util.Collection)}, and the rest) can find
     * it once the missing entity arrives.
     *
     * @implNote
     * This implementation extracts the model id from index element 1 only when
     * {@code modelIdOverride} is {@code null}; an unparseable index leaves the
     * model id {@code null}, since the orphan can still be retried via
     * {@link #retryOrphanEntries(SyncPatchType, List)} which works without a
     * model id.
     *
     * @param mutation        the trusted mutation to persist as an
     *                        orphan
     * @param modelType       the action name extracted from the
     *                        mutation grouping
     * @param modelIdOverride explicit model id override, or
     *                        {@code null} to extract from the index
     * @return the constructed orphan entry
     */
    private static OrphanMutationEntry buildOrphanEntry(
            DecryptedMutation.Trusted mutation,
            String modelType,
            String modelIdOverride
    ) {
        var modelId = modelIdOverride;
        try {
            var indexArray = JSON.parseArray(mutation.index());
            if (modelId == null && indexArray != null && indexArray.size() >= 2) {
                modelId = indexArray.getString(1);
            }
        } catch (Throwable _) {
        }
        return new OrphanMutationEntryBuilder()
                .index(mutation.index())
                .value(mutation.value().orElse(null))
                .operation(mutation.operation())
                .timestamp(mutation.timestamp())
                .actionVersion(mutation.actionVersion())
                .modelType(modelType)
                .modelId(modelId)
                .build();
    }

    /**
     * Builds an {@link OrphanMutationEntry} without an explicit model id
     * override, always extracting the model id from the mutation index.
     *
     * <p>Convenience overload for call-sites that have no
     * {@link MutationApplicationResult#modelId()} hint and simply want the
     * index-derived value; delegates to
     * {@link #buildOrphanEntry(DecryptedMutation.Trusted, String, String)}.
     *
     * @param mutation  the trusted mutation to persist as an orphan
     * @param modelType the action name extracted from the mutation
     *                  grouping
     * @return the constructed orphan entry
     */
    private static OrphanMutationEntry buildOrphanEntry(DecryptedMutation.Trusted mutation, String modelType) {
        return buildOrphanEntry(mutation, modelType, null);
    }

    /**
     * Parses the action index of a trusted mutation and returns the action
     * name from element 0, throwing on unparseable or empty indices.
     *
     * <p>Used by {@link #applyMutations} during the fatal-on-invalid
     * categorization path; an unparseable index there is a server protocol
     * violation that must abort the round. A successfully parsed index whose
     * action name is unknown or blank returns {@code null}, which the caller
     * treats as {@link MutationApplicationResult#unsupported()} rather than
     * fatal.
     *
     * @param mutation the trusted mutation whose index to parse
     * @return the action name, or {@code null} if the action name at
     *         index 0 is unknown
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError if the
     *         index is unparseable or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "parseIndex", adaptation = WhatsAppAdaptation.ADAPTED)
    private String resolveActionName(DecryptedMutation.Trusted mutation) {
        JSONArray indexArray;
        try {
            indexArray = JSON.parseArray(mutation.index());
        } catch (Throwable throwable) {
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "invalid action index: " + mutation.index(), throwable);
        }
        if (indexArray == null || indexArray.isEmpty()) {
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "invalid action index: " + mutation.index(), null);
        }
        var actionName = indexArray.getString(0);
        if (actionName == null || actionName.isBlank()) {
            return null;
        }
        return actionName;
    }

    /**
     * Resolves the action name for a trusted mutation without raising on parse
     * errors.
     *
     * <p>Used on the non-fatal paths where an unparseable index should simply
     * skip the mutation rather than abort the round: orphan retry
     * ({@link #retryOrphanEntries(SyncPatchType, List)}), conflict resolution
     * ({@link #resolveConflicts(SequencedCollection, SyncPatchType)}),
     * unsupported retry ({@link #retryUnsupportedMutations(SyncPatchType)}),
     * and the upload-success bookkeeping path.
     *
     * @implNote
     * This implementation additionally treats a blank action name as
     * {@code null} as a defensive guard.
     *
     * @param mutation the trusted mutation whose index to parse
     * @return the action name, or {@code null} if the index is invalid
     *         or the action name is blank
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "getMutationNameFromIndex", adaptation = WhatsAppAdaptation.ADAPTED)
    private String resolveActionNameSafe(DecryptedMutation.Trusted mutation) {
        var actionName = SyncdIndexUtils
                .getMutationNameFromIndex(null, mutation.index());
        if (actionName == null || actionName.isBlank()) {
            return null;
        }
        return actionName;
    }

    /**
     * Returns the primary-entity identifier (element 1) carried by a mutation
     * index string.
     *
     * <p>Used by {@link #processUploadSuccess} when seeding sync action
     * entries on upload, and by
     * {@link #recordMutationState(SyncPatchType, DecryptedMutation.Trusted, String, MutationApplicationResult)}
     * as the fallback model id when the handler did not surface one of its
     * own. The index is a JSON array of the form
     * {@snippet :
     *     ["actionName", "modelId", ...]
     * }
     *
     * @param mutationIndex the raw index string
     * @return the model id, or {@code null} if the index is invalid or
     *         shorter than two elements
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "parseIndex", adaptation = WhatsAppAdaptation.ADAPTED)
    private String extractModelId(String mutationIndex) {
        var indexArray = SyncdIndexUtils
                .parseIndex(null, mutationIndex);
        if (indexArray != null && indexArray.size() >= 2) {
            return indexArray.getString(1);
        }
        return null;
    }

    /**
     * Stamps the action-state, model-type, and model-id columns of the
     * {@link SyncActionEntry} that corresponds to a freshly-applied trusted
     * mutation.
     *
     * <p>Persists per-mutation telemetry used by the daily syncd stats
     * reporter ({@link #reportSyncdStats()}); only {@link SyncdOperation#SET}
     * mutations keep an entry to update, REMOVE mutations have already been
     * deleted.
     *
     * @implNote
     * This implementation walks all entries for the collection and matches by
     * {@link SyncActionEntry#actionIndex()} plus
     * {@link SyncActionEntry#actionVersion()}; the model id falls back to
     * {@link #extractModelId(String)} when the handler result does not name
     * one, and the model type falls back to {@code actionName}.
     *
     * @param collectionName the collection the mutation belongs to
     * @param mutation       the trusted mutation that was just applied
     * @param actionName     the resolved action name, used as the fallback model
     *                       type; may be {@code null}
     * @param result         the {@link MutationApplicationResult} returned by the
     *                       action handler
     */
    @WhatsAppWebExport(moduleName = "WAWebGetSyncAction", exports = "getSyncActionsByCollectionsInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGetSyncAction", exports = "getSyncActionInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    private void recordMutationState(
            SyncPatchType collectionName,
            DecryptedMutation.Trusted mutation,
            String actionName,
            MutationApplicationResult result
    ) {
        switch (result.actionState()) {
            case SUCCESS, SKIPPED -> syncdDailyStats.storedMutationCount.incrementAndGet();
            case MALFORMED -> syncdDailyStats.invalidActionCount.incrementAndGet();
            case UNSUPPORTED -> syncdDailyStats.unsupportedActionCount.incrementAndGet();
            default -> {
            }
        }

        if (mutation.operation() != SyncdOperation.SET) {
            return;
        }

        var fallbackModelId = extractModelId(mutation.index());
        for (var entry : store.syncStore().getSyncActionEntries(collectionName)) {
            if (!Objects.equals(entry.actionIndex(), mutation.index())
                    || entry.actionVersion() != mutation.actionVersion()) {
                continue;
            }

            entry.setActionState(result.actionState());
            entry.setModelType(result.modelType() != null ? result.modelType() : actionName);
            entry.setModelId(result.modelId() != null ? result.modelId() : fallbackModelId);
        }
    }

    /**
     * Fans a freshly-applied app-state action out to every registered
     * {@link LinkedWebAppStateActionListener}.
     *
     * <p>Fires once per decoded mutation that carries a {@link com.github.auties00.cobalt.wire.linked.sync.action.SyncAction},
     * as the mutation is applied from an incoming snapshot or patch, passing the decoded action together
     * with the mutation's raw index. A mutation whose value carries no action (a bare remove keyed only by
     * its index) surfaces nothing, since there is no action to deliver. Orphan retries do not re-fire the
     * event, so a mutation parked as an orphan and replayed later still notifies listeners exactly once,
     * at first receipt.
     *
     * @implNote
     * This implementation is the generic action-callback analogue of WhatsApp Web's per-mutation
     * dispatch: each listener is invoked on its own virtual thread so a slow or throwing listener can
     * neither stall the sync pipeline nor block the fan-out to the other listeners.
     *
     * @param mutation the decoded, trusted mutation that was just applied
     */
    private void notifyWebAppStateAction(DecryptedMutation.Trusted mutation) {
        var action = mutation.value().flatMap(sav -> sav.action()).orElse(null);
        if (action == null) {
            return;
        }
        var index = mutation.index();
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof LinkedWebAppStateActionListener typed) {
                Thread.startVirtualThread(() -> typed.onWebAppStateAction(whatsapp, action, index));
            }
        }
    }

    /**
     * Replays every stored {@link SyncActionState#UNSUPPORTED} mutation across
     * all {@link SyncPatchType} collections.
     *
     * <p>The unsupported half of the startup orphan sweep; entries previously
     * parked because no handler was registered for their action are given
     * another chance now that the handler registry may have grown.
     *
     * @implNote
     * This implementation iterates {@link SyncPatchType#values()} and
     * delegates to {@link #retryUnsupportedMutations(SyncPatchType)} per
     * collection, performing a per-collection scan rather than a single bulk
     * index query.
     */
    private void retryUnsupportedMutations() {
        for (var patchType : SyncPatchType.values()) {
            retryUnsupportedMutations(patchType);
        }
    }

    /**
     * Replays the {@link SyncActionState#UNSUPPORTED} entries of a single
     * collection through the current {@link WebAppStateHandlerRegistry}.
     *
     * <p>Invoked from {@link #retryUnsupportedMutations()} as part of the
     * startup orphan sweep; entries whose handler version exceeds the
     * mutation's recorded {@link DecryptedMutation.Trusted#actionVersion()}
     * are skipped.
     *
     * @implNote
     * This implementation reconstructs a {@link DecryptedMutation.Trusted}
     * from the stored {@link SyncActionEntry} fields and forwards it through
     * the regular
     * {@link com.github.auties00.cobalt.sync.handler.WebAppStateActionHandler#applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)}
     * path; any throwable from the handler is logged and swallowed so a single
     * failing entry never aborts the sweep.
     *
     * @param collectionName the collection whose unsupported entries are
     *                       replayed
     */
    @WhatsAppWebExport(moduleName = "WAWebGetSyncAction", exports = "getSyncActionsByActionStatesInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGetSyncAction", exports = "getSyncActionsByCollectionsInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    private void retryUnsupportedMutations(SyncPatchType collectionName) {
        coordinator.runLocked(() -> retryUnsupportedMutationsLocked(collectionName));
    }

    /**
     * Replays the {@link SyncActionState#UNSUPPORTED} entries of a single
     * collection while the {@link #coordinator} monitor held by
     * {@link #retryUnsupportedMutations(SyncPatchType)} serializes the replay
     * against the apply path.
     *
     * @param collectionName the collection whose unsupported entries are
     *                       replayed
     */
    private void retryUnsupportedMutationsLocked(SyncPatchType collectionName) {
        var unsupportedEntries = store.syncStore().getSyncActionEntries(collectionName).stream()
                .filter(entry -> entry.actionState() == SyncActionState.UNSUPPORTED)
                .toList();
        for (var entry : unsupportedEntries) {
            if (entry.actionIndex() == null || entry.actionValue() == null) {
                continue;
            }

            var mutation = new DecryptedMutation.Trusted(
                    entry.actionIndex(),
                    entry.actionValue(),
                    SyncdOperation.SET,
                    entry.actionValue().timestamp().orElse(Instant.EPOCH),
                    entry.actionVersion()
            );
            var actionName = resolveActionNameSafe(mutation);
            if (actionName == null) {
                continue;
            }
            var handler = handlerRegistry.findHandler(actionName);
            if (handler.isEmpty() || mutation.actionVersion() > handler.get().version()) {
                continue;
            }

            try {
                var result = handler.get().applyMutation(whatsapp, mutation);
                recordMutationState(collectionName, mutation, actionName, result);
                if (result.isOrphan()) {
                    store.syncStore().addOrphanMutation(collectionName, buildOrphanEntry(
                            mutation,
                            result.modelType() != null ? result.modelType() : actionName,
                            result.modelId()
                    ));
                }
            } catch (Throwable throwable) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unsupported mutation retry failed for action " + actionName + " in " + collectionName, throwable);
            }
        }
    }

    /**
     * Reconciles incoming server mutations against the local pending-mutation
     * queue and returns the subset that should still be applied.
     *
     * <p>Called on the inbound side of every patch processing pass. The
     * per-handler
     * {@link com.github.auties00.cobalt.sync.handler.WebAppStateActionHandler#resolveConflicts(DecryptedMutation.Trusted, DecryptedMutation.Trusted)}
     * verdict picks between
     * {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL},
     * {@link MutationConflictResolutionState#SKIP_REMOTE}, and
     * {@link MutationConflictResolutionState#SKIP_REMOTE_DROP_LOCAL}. After the
     * per-index pass, a cross-index sweep delegates to
     * {@link com.github.auties00.cobalt.sync.handler.WebAppStateActionHandler#dropMutationDueToCrossIndexConflict(DecryptedMutation.Trusted, java.util.Map)}
     * so handlers can drop remote mutations that conflict at a different index
     * (for example, a remote pin at index A obsoleted by a local unpin at
     * index B).
     *
     * @implNote
     * This implementation resolves the action handler via the local
     * {@link WebAppStateHandlerRegistry} rather than reading a handler
     * reference off the mutation record. The drop-and-merge store cleanup is
     * performed inline rather than returned to the caller; the net effect on
     * the pending-mutation table is identical. Mutations without a resolvable
     * handler default to
     * {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL} so the server
     * view wins.
     *
     * @param remoteMutations the trusted mutations decoded from the incoming
     *                        patch
     * @param collectionName  the {@link SyncPatchType} being processed
     * @return the filtered list of trusted mutations that survive the
     *         per-index and cross-index passes
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdResolveConflict", exports = "resolveConflict", adaptation = WhatsAppAdaptation.ADAPTED)
    private SequencedCollection<DecryptedMutation.Trusted> resolveConflicts(SequencedCollection<DecryptedMutation.Trusted> remoteMutations, SyncPatchType collectionName) {
        var pendingMutations = whatsapp.store().syncStore().findPendingMutations(collectionName);
        var pendingByIndex = new LinkedHashMap<String, DecryptedMutation.Trusted>();
        for (var pendingMutation : pendingMutations) {
            pendingByIndex.put(pendingMutation.mutation().index(), pendingMutation.mutation());
        }

        var results = new ArrayList<DecryptedMutation.Trusted>(remoteMutations.size());
        var pendingToDrop = new HashSet<String>();
        var mergedPendingToAdd = new ArrayList<SyncPendingMutation>();
        for (var remoteMutation : remoteMutations) {
            var localMutation = pendingByIndex.get(remoteMutation.index());
            if (localMutation == null) {
                results.add(remoteMutation);
                continue;
            }

            var actionName = resolveActionNameSafe(remoteMutation);
            var handler = actionName != null ? handlerRegistry.findHandler(actionName).orElse(null) : null;
            var resolution = handler != null && remoteMutation.operation() == SyncdOperation.SET
                    ? handler.resolveConflicts(localMutation, remoteMutation)
                    : ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);

            switch (resolution.state()) {
                case APPLY_REMOTE_DROP_LOCAL -> {
                    results.add(remoteMutation);
                    pendingToDrop.add(remoteMutation.index());
                }
                case SKIP_REMOTE -> {
                }
                case SKIP_REMOTE_DROP_LOCAL -> {
                    pendingToDrop.add(remoteMutation.index());
                    if (resolution.mergedMutation() != null) {
                        results.add(resolution.mergedMutation());
                        mergedPendingToAdd.add(new SyncPendingMutation(resolution.mergedMutation(), 0));
                    }
                }
            }
        }

        var filteredResults = new ArrayList<DecryptedMutation.Trusted>(results.size());
        for (var remoteMutation : results) {
            var actionName = resolveActionNameSafe(remoteMutation);
            var handler = actionName != null ? handlerRegistry.findHandler(actionName).orElse(null) : null;
            if (handler != null && handler.dropMutationDueToCrossIndexConflict(remoteMutation, pendingByIndex)) {
                continue;
            }
            filteredResults.add(remoteMutation);
        }

        if (!pendingToDrop.isEmpty() || !mergedPendingToAdd.isEmpty()) {
            var pendingMutationIdsToDrop = whatsapp.store().syncStore().findPendingMutations(collectionName)
                    .stream()
                    .filter(pm -> pendingToDrop.contains(pm.mutation().index()))
                    .map(SyncPendingMutation::mutationId)
                    .toList();
            if (!pendingMutationIdsToDrop.isEmpty()) {
                whatsapp.store().syncStore().removePendingMutations(collectionName, pendingMutationIdsToDrop);
            }
            if (!mergedPendingToAdd.isEmpty()) {
                whatsapp.store().syncStore().addPendingMutations(collectionName, mergedPendingToAdd);
            }
        }

        return Collections.unmodifiableSequencedCollection(filteredResults);
    }

    /**
     * Folds a sequence of decrypted mutations into the supplied base LT-Hash
     * and collects the corresponding {@link SyncActionEntryUpdate} list.
     *
     * <p>Drives the per-patch LT-Hash recomputation used to validate the
     * server-supplied snapshot MAC; SET mutations remove the prior value-MAC
     * (when one was stored) and add the new one, while REMOVE mutations remove
     * the stored value-MAC.
     *
     * @implNote
     * This implementation tolerates a REMOVE for an index MAC that has no
     * local entry by logging and skipping it, which is legitimate during
     * split-brain recovery or out-of-order patch delivery. The LT-Hash math is
     * delegated to {@link MutationLTHash#subtractThenAdd(byte[], List, List)},
     * and the returned {@link SyncActionEntryUpdate} list is only persisted
     * once {@link #updateCollectionState(SyncPatchType, long, byte[])} confirms
     * the version guard passed.
     *
     * @param patchType the collection these mutations belong to
     * @param baseHash  the starting LT-Hash; {@code null} is treated as
     *                  {@link MutationLTHash#EMPTY_HASH}
     * @param mutations the decoded mutations to fold in
     * @return the new LT-Hash and the entry updates to apply on commit
     */
    @WhatsAppWebExport(moduleName = "WAWebGetSyncAction", exports = "getSyncActionsByIndexMacsInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    private LtHashComputation computeNewLTHash(SyncPatchType patchType, byte[] baseHash, SequencedCollection<DecryptedMutation.Untrusted> mutations) {
        var currentHash = baseHash != null ? baseHash : MutationLTHash.EMPTY_HASH;
        var toAdd = new ArrayList<byte[]>();
        var toRemove = new ArrayList<byte[]>();
        var updates = new ArrayList<SyncActionEntryUpdate>(mutations.size());

        for (var mutation : mutations) {
            var indexMac = mutation.indexMac();
            var valueMac = mutation.valueMac();

            if (mutation.operation() == SyncdOperation.SET) {
                store.syncStore().findSyncActionEntry(patchType, indexMac)
                        .ifPresent(existing -> toRemove.add(existing.valueMac()));
                toAdd.add(valueMac);
                updates.add(new SyncActionEntryUpdate(
                        indexMac,
                        new SyncActionEntryBuilder()
                                .indexMac(indexMac)
                                .valueMac(valueMac)
                                .keyId(mutation.keyId())
                                .actionIndex(mutation.index())
                                .actionValue(mutation.value().orElse(null))
                                .actionVersion(mutation.actionVersion())
                                .build(),
                        false
                ));
            } else {
                var removedEntry = store.syncStore().findSyncActionEntry(patchType, indexMac);
                if (removedEntry.isPresent()) {
                    toRemove.add(removedEntry.get().valueMac());
                    updates.add(new SyncActionEntryUpdate(indexMac, null, true));
                } else {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "remove mutation has no local entry for index mac in {0}, skipping from lt-hash computation", patchType);
                }
            }
        }

        return new LtHashComputation(
                MutationLTHash.subtractThenAdd(currentHash, toAdd, toRemove).ltHash(),
                updates
        );
    }

    /**
     * Commits the {@link SyncActionEntryUpdate} list computed by
     * {@link #computeNewLTHash(SyncPatchType, byte[], SequencedCollection)}
     * into the per-collection sync-action store.
     *
     * <p>Called only after the version guard inside
     * {@link #updateCollectionState(SyncPatchType, long, byte[])} accepts the
     * new collection version, so an entry write never lands ahead of a
     * collection metadata write that the server later supersedes.
     *
     * @implNote
     * This implementation routes each update through either
     * {@link LinkedWhatsAppSyncStore#removeSyncActionEntry(SyncPatchType, byte[])} or
     * {@link LinkedWhatsAppSyncStore#putSyncActionEntry(SyncPatchType, byte[], SyncActionEntry)}
     * based on the {@link SyncActionEntryUpdate#remove()} flag; no batching is
     * performed because the in-memory store maps are unsynchronised
     * per-collection.
     *
     * @param patchType the collection whose entries are being updated
     * @param updates   the entry updates to persist
     */
    private void applySyncActionEntryUpdates(SyncPatchType patchType, List<SyncActionEntryUpdate> updates) {
        for (var update : updates) {
            if (update.remove()) {
                store.syncStore().removeSyncActionEntry(patchType, update.indexMac());
            } else {
                store.syncStore().putSyncActionEntry(patchType, update.indexMac(), update.entry());
            }
        }
    }

    /**
     * Writes a new collection version and LT-Hash atomically, skipping the
     * write when the supplied version is not strictly newer than the currently
     * persisted version.
     *
     * <p>Returns the gate value the caller uses to decide whether to commit
     * the dependent {@link SyncActionEntryUpdate} list; on a {@code false}
     * return the caller must drop the updates as well, so the version, hash,
     * and action-entry rows stay consistent.
     *
     * @implNote
     * This implementation uses
     * {@link LinkedWhatsAppSyncStore#updateWebAppStateVersion(SyncPatchType, long, byte[])}
     * which updates the version map and the LT-Hash map in a single call;
     * there is no bulk variant because the per-collection state machine
     * commits one collection at a time.
     *
     * @param collectionName the collection being updated
     * @param version        the new collection version
     * @param ltHash         the new LT-Hash
     * @return {@code true} if the write was applied, {@code false} when the
     *         version guard rejected it
     */
    @WhatsAppWebExport(moduleName = "WAWebGetCollectionVersion", exports = "updateCollectionVersionAndLtHashInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGetCollectionVersion", exports = "bulkUpdateCollectionVersionInTransaction", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean updateCollectionState(SyncPatchType collectionName, long version, byte[] ltHash) {
        var currentVersion = getCurrentVersion(collectionName);
        if (version > 0 && currentVersion > 0 && version <= currentVersion) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "skipping state update for {0}: version {1} is not newer than current {2}", collectionName, version, currentVersion);
            return false;
        }

        store.syncStore().updateWebAppStateVersion(collectionName, version, ltHash);
        return true;
    }

    /**
     * Transitions a failed collection into the {@link SyncCollectionState}
     * that matches the supplied throwable and schedules the appropriate
     * follow-up (missing-key request, fatal notification, or retry).
     *
     * <p>Dispatches based on the throwable type. A
     * {@link WhatsAppWebAppStateSyncException.MissingKey} parks the collection
     * in {@link SyncCollectionState#BLOCKED} and asks
     * {@link MissingSyncKeyRequestService} to fetch the missing key. A fatal
     * {@link WhatsAppWebAppStateSyncException} parks it in
     * {@link SyncCollectionState#ERROR_FATAL}, waits five seconds, notifies the
     * primary device via
     * {@link #sendAppStateFatalExceptionNotification(List)}, and forwards the
     * exception to
     * {@link LinkedWhatsAppClient#handleFailure(com.github.auties00.cobalt.exception.WhatsAppException)}.
     * Anything else is treated as retryable: the collection is scheduled with
     * the backoff returned by the server (when present) and parked in
     * {@link SyncCollectionState#ERROR_RETRY}; if the backoff window has
     * expired the collection escalates to
     * {@link SyncCollectionState#ERROR_FATAL}.
     *
     * @implNote
     * This implementation routes the fatal path through Cobalt's pluggable
     * {@link WhatsAppLinkedClientErrorHandler}
     * instead of a hardcoded logout, in keeping with the user-configurable
     * recovery model. The server-backoff reset on the retry attempt counter
     * matches the side effect of an {@code ErrorRetry} server reply.
     *
     * @param error          the throwable raised by the sync round
     * @param collectionName the collection that failed; {@code null} is
     *                       silently ignored
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdFatal", exports = "handleFatalError", adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleSyncError(Throwable error, SyncPatchType collectionName) {
        if (collectionName == null) {
            return;
        }

        var metadata = store.syncStore().findWebAppState(collectionName);
        if (error instanceof WhatsAppWebAppStateSyncException.MissingKey missingKeyEx) {
            store.syncStore().markWebAppStateBlocked(collectionName);
            var keyIds = missingKeyEx.keyIds();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "collection {0} blocked, requesting {1} missing sync keys", collectionName, keyIds.size());

            coordinator.runWithMonitorReleased(() -> {
                missingSyncKeyRequestService.requestMissingKeys(keyIds);
                missingSyncKeyTimeoutScheduler.startPeriodicReRequestJob();
            });
        } else if (error instanceof WhatsAppWebAppStateSyncException syncEx && syncEx.isFatal()) {
            store.syncStore().markWebAppStateErrorFatal(collectionName);
            if (Log.ERROR) LOGGER.log(Level.ERROR, "app-state sync fatal error for collection " + collectionName, syncEx);

            var collectionNames = List.of(String.valueOf(collectionName));

            coordinator.runWithMonitorReleased(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                try {
                    sendAppStateFatalExceptionNotification(collectionNames);
                } catch (Throwable notifyError) {
                    if (Log.ERROR) LOGGER.log(Level.ERROR, "syncd: error when sending fatal message to primary", notifyError);
                }
                whatsapp.handleFailure(syncEx);
            });
        } else {
            var firstFailureTimestamp = metadata.lastErrorTimestamp()
                    .map(Instant::toEpochMilli)
                    .orElseGet(System::currentTimeMillis);
            var serverBackoffMs = error instanceof WhatsAppWebAppStateSyncException.RetryableServerError retryable
                    ? retryable.serverBackoffMs()
                    : null;
            if (serverBackoffMs != null) {
                retryScheduler.resetAttemptCounter();
            }
            var result = retryScheduler.scheduleRetry(
                    collectionName,
                    firstFailureTimestamp,
                    serverBackoffMs,
                    () -> syncCollection(collectionName)
            );
            if (result) {
                store.syncStore().markWebAppStateErrorRetry(collectionName);
                if (Log.WARNING) LOGGER.log(Level.WARNING, "collection " + collectionName + " sync failed, scheduled for retry", error);
                logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.ENTERED_RETRY_MODE);
            } else {
                store.syncStore().markWebAppStateErrorFatal(collectionName);
                if (Log.ERROR) LOGGER.log(Level.ERROR, "collection " + collectionName + " exhausted retries, marking fatal", error);
            }
        }
    }

    /**
     * Builds and dispatches an app-state fatal-exception notification peer
     * message to the primary device (device id 0) for the listed collections.
     *
     * <p>Wakes the primary device whenever the companion encounters an
     * unrecoverable syncd error, so it can surface a diagnostic and re-pair
     * the companion if necessary; called from
     * {@link #handleSyncError(Throwable, SyncPatchType)} after the five-second
     * settle delay. Silently logs and returns when the own JID has not been
     * established yet (the companion is not yet paired).
     *
     * @implNote
     * This implementation constructs the primary device JID with the
     * four-argument {@link Jid#of(String, com.github.auties00.cobalt.wire.core.jid.JidServer, int, int)}
     * form so the agent slot is explicitly zero and the caller's agent is not
     * preserved. The peer message is sent through the single
     * {@link LinkedWhatsAppClient#sendPeerMessage} entry point.
     *
     * @param collectionNames the affected collection names; written into the
     *                        protobuf payload verbatim
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdFatalExceptionNotificationApi", exports = "sendAppStateFatalExceptionNotification", adaptation = WhatsAppAdaptation.ADAPTED)
    private void sendAppStateFatalExceptionNotification(List<String> collectionNames) {
        var myJid = whatsapp.store().accountStore().jid().orElse(null);
        if (myJid == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot send fatal exception notification: own jid not available");
            return;
        }

        var notification = new AppStateFatalExceptionNotificationBuilder()
                .collectionNames(collectionNames)
                .timestamp(Instant.now())
                .build();

        var protocolMessage = new ProtocolMessageBuilder()
                .type(ProtocolMessage.Type.APP_STATE_FATAL_EXCEPTION_NOTIFICATION)
                .appStateFatalExceptionNotification(notification)
                .build();

        var messageContainer = new LinkedMessageContainerBuilder()
                .protocolMessage(protocolMessage)
                .build();

        var primaryDeviceJid = Jid.of(myJid.user(), myJid.server(), 0, 0);
        var messageKey = new MessageKeyBuilder()
                .id(MessageIdGenerator.generate(MessageIdVersion.V2, myJid))
                .parentJid(myJid.toUserJid())
                .fromMe(true)
                .senderJid(myJid)
                .build();
        var messageInfo = new ChatMessageInfoBuilder()
                .key(messageKey)
                .message(messageContainer)
                .build();

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending app-state fatal exception notification for {0} collections", collectionNames.size());
        whatsapp.sendPeerMessage(primaryDeviceJid, messageInfo);
    }

    /**
     * Schedules the all-peers-negative check inside the
     * {@link MissingSyncKeyTimeoutScheduler}.
     *
     * <p>Invoked from the key-share receiver whenever a companion device
     * replies to a sync-key request peer message without supplying the
     * requested key; after the grace period elapses with every companion
     * having responded negatively, the missing key is escalated to a fatal
     * sync failure. The timer bookkeeping, cancellation, and fatal escalation
     * all live on the scheduler.
     */
    @Override
    public void scheduleAllDevicesRespondedCheck() {
        missingSyncKeyTimeoutScheduler.scheduleAllDevicesRespondedCheck();
    }

    /**
     * Forces the missing-sync-key timeout deadline to be recomputed against
     * the current contents of the missing-key store.
     *
     * <p>Must be called whenever a missing key has been resolved (typically by
     * the {@code AppStateSyncKeyShare} receiver removing it from the store) so
     * the deadline tracks the new earliest-pending key, or clears entirely
     * when no missing keys remain.
     *
     * @implNote
     * This implementation delegates to
     * {@link MissingSyncKeyTimeoutScheduler#scheduleTimeoutCheck()} which
     * idempotently replaces the in-flight timeout handle.
     */
    @Override
    public void rescheduleMissingSyncKeyTimeout() {
        missingSyncKeyTimeoutScheduler.scheduleTimeoutCheck();
    }

    /**
     * Drives an outbound sync round for every collection currently parked in
     * {@link SyncCollectionState#DIRTY}.
     *
     * <p>Called immediately before a graceful logout so the sentinel mutation
     * that carries key-rotation state reaches every companion device; without
     * this flush the rotated key would only land on the next reconnect.
     *
     * @implNote
     * This implementation iterates {@link SyncPatchType#values()} and skips
     * any collection whose {@link SyncCollectionState} is not
     * {@link SyncCollectionState#DIRTY}; per-collection failures are swallowed
     * and logged because the caller is about to disconnect and cannot react.
     */
    @Override
    public void flushDirtyCollections() {
        for (var patchType : SyncPatchType.values()) {
            var metadata = store.syncStore().findWebAppState(patchType);
            if (metadata.state() == SyncCollectionState.DIRTY) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "flushing dirty collection {0} before disconnect", patchType);
                try {
                    syncCollection(patchType);
                } catch (Exception e) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "flush failed for dirty collection " + patchType + " on disconnect", e);
                }
            }
        }
    }

    /**
     * Starts the four recurring background jobs that keep app-state sync
     * eventually consistent and instrumented.
     *
     * <p>The four jobs catch missed dirty-bit notifications, ship the daily
     * action and key telemetry, and rotate the active app state sync key.
     * Safe to call after a previous {@link #stopPeriodicSyncJob()}; idempotent
     * if already started.
     *
     * @implNote
     * This implementation cancels any in-flight periodic sync handle before
     * scheduling, then dispatches the rotation job to
     * {@link SyncKeyRotationService#startPeriodicRotationJob()} and the stats
     * jobs to {@link #startPeriodicReportSyncdStatsJob()} and
     * {@link #startPeriodicReportSyncdKeyStatsJob()}; the four jobs run on
     * independent {@link ScheduledTask} handles.
     */
    @Override
    public void startPeriodicSyncJob() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting periodic app-state sync jobs");
        stopPeriodicSyncJob();
        scheduleNextPeriodicSync();

        syncKeyRotationService.startPeriodicRotationJob();

        startPeriodicReportSyncdStatsJob();

        startPeriodicReportSyncdKeyStatsJob();
    }

    /**
     * Schedules the next firing of the recurring catch-up sweep against
     * {@link ABProp#SYNCD_PERIODIC_SYNC_DAYS}.
     *
     * <p>Reads the day count from the AB prop on each tick so a server-side
     * cadence change takes effect on the next reschedule without restarting
     * the periodic job. A zero-or-negative value disables the sweep entirely
     * and leaves {@link #periodicSyncJob} {@code null}.
     *
     * @implNote
     * This implementation self-reschedules from the {@code finally} branch of
     * the task lambda; per-tick failures are logged and the reschedule still
     * happens so a single bad sweep does not stop the recurrence.
     */
    private void scheduleNextPeriodicSync() {
        var days = abPropsService.getInt(ABProp.SYNCD_PERIODIC_SYNC_DAYS);
        if (days <= 0) {
            return;
        }

        periodicSyncJob = ScheduledTask.scheduleDelayed(
                Duration.ofDays(days),
                () -> {
                    try {
                        pullPatches(SyncPatchType.values());
                    } catch (Exception e) {
                        if (Log.WARNING) LOGGER.log(Level.WARNING, "periodic sync job failed", e);
                    } finally {
                        scheduleNextPeriodicSync();
                    }
                }
        );
    }

    /**
     * Cancels the in-flight catch-up sweep handle if one is scheduled.
     *
     * <p>Pairs with {@link #startPeriodicSyncJob()}; safe to call when no job
     * is scheduled (no-op) and called from {@link #reset()} during graceful
     * disconnect.
     *
     * @implNote
     * This implementation cancels the {@link ScheduledTask} handle, waking a
     * pending sweep so it never fires and interrupting one already running; the
     * field is then nulled to permit re-arming via
     * {@link #scheduleNextPeriodicSync()}.
     */
    @Override
    public void stopPeriodicSyncJob() {
        var job = periodicSyncJob;
        if (job != null) {
            job.cancel();
            periodicSyncJob = null;
        }
    }

    /**
     * Arms the recurring action-stats job that ships the daily mutation-state
     * histogram.
     *
     * <p>Each tick walks every stored sync-action entry, buckets its state,
     * and emits one mutation-stats WAM event per mutation name.
     *
     * @implNote
     * This implementation cancels any prior handle via
     * {@link #stopPeriodicReportSyncdStatsJob()} before scheduling on the fixed
     * one-day cadence, so a second {@link #startPeriodicSyncJob()} call cannot
     * leave two parallel stats tasks running.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdReportSyncdStatJob", exports = "reportSyncdStatsJob", adaptation = WhatsAppAdaptation.ADAPTED)
    private void startPeriodicReportSyncdStatsJob() {
        stopPeriodicReportSyncdStatsJob();
        periodicReportSyncdStatsJob = ScheduledTask.schedule(Duration.ofDays(1), this::runReportSyncdStats);
    }

    /**
     * Runs one action-stats tick, swallowing any failure so the recurrence
     * survives it.
     *
     * <p>The body of the recurring action-stats job; the one-day cadence is
     * fixed, with no AB prop to widen or narrow the interval. A
     * {@link #reportSyncdStats()} failure is logged and dropped so a single bad
     * tick does not halt the loop, which the scheduler keeps running until
     * {@link #stopPeriodicReportSyncdStatsJob()} cancels it.
     */
    private void runReportSyncdStats() {
        try {
            reportSyncdStats();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "periodic syncd stats reporting job failed", e);
        }
    }

    /**
     * Walks every stored {@link SyncActionEntry} across all collections,
     * buckets per-state counts by mutation name, and commits one mutation
     * stats WAM event per distinct mutation.
     *
     * <p>The body of the recurring action-stats task.
     * {@link SyncActionState#SUCCESS} and {@link SyncActionState#SKIPPED} both
     * increment the applied bucket; {@link SyncActionState#MALFORMED} feeds the
     * invalid bucket; {@link SyncActionState#ORPHAN},
     * {@link SyncActionState#UNSUPPORTED}, and {@link SyncActionState#FAILED}
     * each feed their own bucket. Mutation names that cannot be parsed back
     * from the index are reported under the literal sentinel
     * {@code "no-mutation-name"}.
     *
     * @implNote
     * This implementation iterates the per-collection sync-action views rather
     * than a flat table, and tolerates a {@code null} action state by skipping
     * the entry so the reporting job does not abort partway through.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdReportSyncdStatJob", exports = "reportSyncdStatsJob", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSyncdWamUtils", exports = "generateActionStatCounts", adaptation = WhatsAppAdaptation.ADAPTED)
    void reportSyncdStats() {
        var counts = new LinkedHashMap<String, ActionStatCounts>();
        for (var patchType : SyncPatchType.values()) {
            for (var entry : store.syncStore().getSyncActionEntries(patchType)) {
                var actionName = SyncdIndexUtils.getMutationNameFromIndex(patchType.toString(), entry.actionIndex());
                if (actionName == null || actionName.isBlank()) {
                    actionName = "no-mutation-name";
                }
                var bucket = counts.computeIfAbsent(actionName, name -> new ActionStatCounts());
                var actionState = entry.actionState();
                if (actionState == null) {
                    continue;
                }
                switch (actionState) {
                    case SUCCESS, SKIPPED -> bucket.applied++;
                    case MALFORMED -> bucket.invalid++;
                    case ORPHAN -> bucket.orphan++;
                    case UNSUPPORTED -> bucket.unsupported++;
                    case FAILED -> bucket.failed++;
                }
            }
        }

        for (var entry : counts.entrySet()) {
            var actionName = entry.getKey();
            var stats = entry.getValue();
            wamService.commit(new MdAppStateSyncMutationStatsEventBuilder()
                    .syncdAction(actionName)
                    .applied(convertToBucket(stats.applied))
                    .invalid(convertToBucket(stats.invalid))
                    .orphan(convertToBucket(stats.orphan))
                    .unsupported(convertToBucket(stats.unsupported))
                    .failed(convertToBucket(stats.failed))
                    .build());
        }
    }

    /**
     * Maps a non-negative mutation count to the matching
     * {@link MutationCountBucket} bucket constant.
     *
     * <p>Implements the privacy-preserving bucketing the WAM pipeline requires:
     * {@code 0} maps to {@link MutationCountBucket#ZERO}, {@code 1} to
     * {@link MutationCountBucket#ONE}, then strict less-than thresholds at 10,
     * 100, 500, 1000, and 5000 map to {@link MutationCountBucket#LT10},
     * {@link MutationCountBucket#LT100}, {@link MutationCountBucket#LT500},
     * {@link MutationCountBucket#LT1K}, and {@link MutationCountBucket#LT5K},
     * with anything else landing in {@link MutationCountBucket#GTE5K}.
     *
     * @param count the mutation count to bucket
     * @return the bucket constant for {@code count}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdWamUtils", exports = "convertToBucket", adaptation = WhatsAppAdaptation.DIRECT)
    private static MutationCountBucket convertToBucket(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("cannot convert negative number to a bucket");
        }
        if (count == 0) return MutationCountBucket.ZERO;
        if (count == 1) return MutationCountBucket.ONE;
        if (count < 10) return MutationCountBucket.LT10;
        if (count < 100) return MutationCountBucket.LT100;
        if (count < 500) return MutationCountBucket.LT500;
        if (count < 1_000) return MutationCountBucket.LT1K;
        if (count < 5_000) return MutationCountBucket.LT5K;
        return MutationCountBucket.GTE5K;
    }

    /**
     * Arms the recurring key-stats job that ships the daily
     * per-app-state-sync-key usage histogram.
     *
     * <p>Each tick computes the key-count WAM event via
     * {@link #reportSyncdKeyStats()}.
     *
     * @implNote
     * This implementation cancels any prior handle via
     * {@link #stopPeriodicReportSyncdKeyStatsJob()} before scheduling on the
     * fixed one-day cadence so a second {@link #startPeriodicSyncJob()} call
     * cannot leave two parallel key-stats tasks running.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdReportKeyStatsJob", exports = "reportSyncdKeyStatsJob", adaptation = WhatsAppAdaptation.ADAPTED)
    private void startPeriodicReportSyncdKeyStatsJob() {
        stopPeriodicReportSyncdKeyStatsJob();
        periodicReportSyncdKeyStatsJob = ScheduledTask.schedule(Duration.ofDays(1), this::runReportSyncdKeyStats);
    }

    /**
     * Runs one key-stats tick, swallowing any failure so the recurrence
     * survives it.
     *
     * <p>The body of the recurring key-stats job; always uses a one-day cadence
     * with no gatekeeper-driven stretch to a longer interval. A
     * {@link #reportSyncdKeyStats()} failure is logged and dropped so a single
     * bad tick does not halt the loop, which the scheduler keeps running until
     * {@link #stopPeriodicReportSyncdKeyStatsJob()} cancels it.
     */
    private void runReportSyncdKeyStats() {
        try {
            reportSyncdKeyStats();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "periodic syncd key stats reporting job failed", e);
        }
    }

    /**
     * Cancels the in-flight key-stats task handle if one is scheduled.
     *
     * <p>Pairs with {@link #startPeriodicReportSyncdKeyStatsJob()}; safe to
     * call when no job is scheduled (no-op) and invoked from {@link #reset()}
     * during disconnect.
     *
     * @implNote
     * This implementation cancels the {@link ScheduledTask} handle, waking a
     * pending tick so it never fires and interrupting one already running; the
     * field is then nulled so a subsequent
     * {@link #startPeriodicReportSyncdKeyStatsJob()} can re-arm.
     */
    @Override
    public void stopPeriodicReportSyncdKeyStatsJob() {
        var job = periodicReportSyncdKeyStatsJob;
        if (job != null) {
            job.cancel();
            periodicReportSyncdKeyStatsJob = null;
        }
    }

    /**
     * Computes and commits one key-count WAM event with the bucketed
     * per-app-state-sync-key usage percentiles.
     *
     * <p>The body of the recurring key-stats task; the event lets server-side
     * telemetry track how many syncd keys are in use across the population
     * versus how many sit unused, and how concentrated mutation volume is per
     * key (the p80/p95 fields).
     *
     * @implNote
     * This implementation runs unconditionally with no gatekeeper kill switch.
     * The session-length field is attached only when the session-start
     * timestamp has been recorded ({@link KeyStats#syncdSessionLengthDays()}
     * returns non-null).
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdReportKeyStatsJob", exports = "reportSyncdKeyStatsJob", adaptation = WhatsAppAdaptation.ADAPTED)
    void reportSyncdKeyStats() {
        var stats = getKeyStats();
        var event = new SyncdKeyCountEventBuilder()
                .keysUsedInSnapshotCount(stats.keysUsedInSnapshotCount())
                .p80MuationsPerKey(stats.p80MutationsPerKey())
                .p95MuationsPerKey(stats.p95MutationsPerKey())
                .totalKeyCount(stats.totalKeyCount());
        var sessionLengthDays = stats.syncdSessionLengthDays();
        if (sessionLengthDays != null) {
            event.syncdSessionLengthDays(sessionLengthDays);
        }
        wamService.commit(event.build());
    }

    /**
     * Gathers the inputs to the key-count bucket math and hands them to
     * {@link #getKeyStatsInternal(Collection, Collection, Integer)}.
     *
     * <p>Called from {@link #reportSyncdKeyStats()} on every daily stats tick;
     * combines the known {@link AppStateSyncKey} set, the union of all stored
     * {@link SyncActionEntry} rows across every collection, and the
     * session-start timestamp read by {@link #getSyncdSessionStartTimestamp()}.
     *
     * @implNote
     * This implementation iterates {@link SyncPatchType#values()} to read the
     * per-collection sync-action views in turn. The session-length math uses
     * {@link Math#round(double)} on the raw millisecond delta divided by
     * {@code 1000 * 3600 * 24}.
     *
     * @return the populated {@link KeyStats} record
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdWamUtils", exports = "getKeyStats", adaptation = WhatsAppAdaptation.ADAPTED)
    KeyStats getKeyStats() {
        var keys = whatsapp.store().syncStore().appStateKeys();
        var entries = new ArrayList<SyncActionEntry>();
        for (var patchType : SyncPatchType.values()) {
            entries.addAll(whatsapp.store().syncStore().getSyncActionEntries(patchType));
        }
        var sessionStart = getSyncdSessionStartTimestamp();
        Integer sessionLengthDays = null;
        if (sessionStart != null) {
            var deltaMs = System.currentTimeMillis() - sessionStart.toEpochMilli();
            sessionLengthDays = (int) Math.round(deltaMs / (1000.0 * 3600.0 * 24.0));
        }
        return getKeyStatsInternal(keys, entries, sessionLengthDays);
    }

    /**
     * Resolves the {@code session_start} mutation's timestamp from the
     * {@link SyncPatchType#REGULAR_LOW} sync-action store.
     *
     * <p>Surfaces the moment the current syncd session began for
     * {@link #getKeyStats()}; returns {@code null} when no
     * {@code session_start} mutation has been recorded yet (typically right
     * after a fresh pairing), in which case the WAM event omits the
     * session-length field. The canonical index payload is the JSON array
     * {@snippet :
     *     ["primary_version","session_start"]
     * }
     *
     * @implNote
     * This implementation scans the {@link SyncPatchType#REGULAR_LOW}
     * sync-action entries looking for an
     * {@link SyncActionEntry#actionIndex()} that contains both literal tokens;
     * because Cobalt keys on the HMAC-derived index rather than the plaintext
     * index, it has to walk the collection rather than do a primary-key
     * lookup.
     *
     * @return the recorded session start timestamp, or {@code null} when no
     *         such entry exists
     */
    private Instant getSyncdSessionStartTimestamp() {
        for (var entry : whatsapp.store().syncStore().getSyncActionEntries(SyncPatchType.REGULAR_LOW)) {
            var actionIndex = entry.actionIndex();
            if (actionIndex == null) {
                continue;
            }
            if (!actionIndex.contains("primary_version") || !actionIndex.contains("session_start")) {
                continue;
            }
            var actionValue = entry.actionValue();
            if (actionValue == null) {
                continue;
            }
            return actionValue.timestamp().orElse(null);
        }
        return null;
    }

    /**
     * Builds the per-app-state-sync-key usage histogram and derives the
     * key-count percentiles from the supplied keys, entries, and optional
     * session length.
     *
     * <p>The pure side of the key-count math, separated from
     * {@link #getKeyStats()} so tests can exercise it without touching the
     * store. The total key count is the size of {@code keys} (the total app
     * state sync key population), the keys-used count is the number of
     * distinct key ids that back at least one entry, and the two percentile
     * fields are {@code sortedCounts[floor(N * 0.8) - 1]} and
     * {@code sortedCounts[floor(N * 0.95) - 1]}; an out-of-range percentile
     * index leaves the corresponding field {@code null}.
     *
     * @implNote
     * This implementation tolerates a {@code null}
     * {@link SyncActionEntry#keyId()} by skipping that entry from the
     * histogram. Per-key counts are sorted numerically rather than
     * lexicographically, since the values are mutation counts and a numeric
     * sort is the intent of the percentile lookup.
     *
     * @param keys                   the full app state sync key population
     * @param entries                every stored {@link SyncActionEntry}
     *                               across all collections
     * @param syncdSessionLengthDays the session length in days, or
     *                               {@code null} when no {@code session_start}
     *                               timestamp has been recorded
     * @return the populated {@link KeyStats} record
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdWamUtils", exports = "getKeyStatsInternal", adaptation = WhatsAppAdaptation.DIRECT)
    KeyStats getKeyStatsInternal(Collection<AppStateSyncKey> keys, Collection<SyncActionEntry> entries, Integer syncdSessionLengthDays) {
        var encodedKeyIds = new ArrayList<String>(entries.size());
        for (var entry : entries) {
            var keyId = entry.keyId();
            if (keyId == null) {
                continue;
            }
            encodedKeyIds.add(Base64.getEncoder().encodeToString(keyId));
        }
        var distinctKeyIds = new HashSet<>(encodedKeyIds);
        var perKeyCounts = new HashMap<String, Integer>();
        for (var encoded : encodedKeyIds) {
            perKeyCounts.merge(encoded, 1, Integer::sum);
        }
        var sortedCounts = new ArrayList<>(perKeyCounts.values());
        Collections.sort(sortedCounts);
        var size = sortedCounts.size();
        var p80Index = (int) Math.floor(size * 0.8) - 1;
        var p95Index = (int) Math.floor(size * 0.95) - 1;
        var p80 = (p80Index >= 0 && p80Index < size) ? sortedCounts.get(p80Index) : null;
        var p95 = (p95Index >= 0 && p95Index < size) ? sortedCounts.get(p95Index) : null;
        return new KeyStats(
                keys.size(),
                distinctKeyIds.size(),
                p80,
                p95,
                syncdSessionLengthDays
        );
    }

    /**
     * The aggregated per-app-state-sync-key snapshot consumed by
     * {@link #reportSyncdKeyStats()}.
     *
     * <p>Carries the four populated key-count WAM event fields plus the
     * optional session length; populated by {@link #getKeyStats()} and
     * {@link #getKeyStatsInternal(Collection, Collection, Integer)}.
     *
     * @param totalKeyCount           number of app state sync keys present
     *                                in the local store
     * @param keysUsedInSnapshotCount number of distinct keys that have at
     *                                least one stored
     *                                {@link SyncActionEntry}
     * @param p80MutationsPerKey      mutation count at the 80th percentile
     *                                of the per-key histogram, or
     *                                {@code null} when the percentile index
     *                                is out of range
     * @param p95MutationsPerKey      mutation count at the 95th percentile
     *                                of the per-key histogram, or
     *                                {@code null} when the percentile index
     *                                is out of range
     * @param syncdSessionLengthDays  days since the {@code session_start}
     *                                primary version timestamp, or
     *                                {@code null} when no
     *                                {@code session_start} mutation has
     *                                been recorded
     */
    record KeyStats(
            int totalKeyCount,
            int keysUsedInSnapshotCount,
            Integer p80MutationsPerKey,
            Integer p95MutationsPerKey,
            Integer syncdSessionLengthDays
    ) {
    }

    /**
     * Cancels the in-flight action-stats task handle if one is scheduled.
     *
     * <p>Pairs with {@link #startPeriodicReportSyncdStatsJob()}; safe to call
     * when no job is scheduled (no-op) and invoked from {@link #reset()}
     * during disconnect.
     *
     * @implNote
     * This implementation cancels the {@link ScheduledTask} handle, waking a
     * pending tick so it never fires and interrupting one already running; the
     * field is then nulled so a subsequent
     * {@link #startPeriodicReportSyncdStatsJob()} can re-arm.
     */
    @Override
    public void stopPeriodicReportSyncdStatsJob() {
        var job = periodicReportSyncdStatsJob;
        if (job != null) {
            job.cancel();
            periodicReportSyncdStatsJob = null;
        }
    }

    /**
     * Arms or shifts the debounced flush that ships the accumulated
     * {@link MdAppStateSyncDailyEventBuilder MdAppStateSyncDaily} aggregate.
     *
     * <p>Called from every mutation-processing chokepoint that touches
     * {@link #syncdDailyStats}. Any pending flush is cancelled and replaced with
     * a fresh one {@link #APP_STATE_SYNC_DAILY_FLUSH_DELAY} out, so a burst of
     * sync activity keeps shifting the window forward and a single aggregate is
     * emitted only after the collection has quiesced.
     *
     * @implNote
     * This implementation reproduces WhatsApp Web's {@code ShiftTimer.onOrBefore}
     * behaviour with a rescheduled {@link ScheduledTask}; the previous handle is
     * cancelled before the new one is armed so at most one flush is ever
     * pending, and the cancellation is a no-op when the prior flush already
     * fired.
     */
    private void scheduleAppStateSyncDailyFlush() {
        var previous = appStateSyncDailyFlushTask;
        if (previous != null) {
            previous.cancel();
        }
        appStateSyncDailyFlushTask = ScheduledTask.scheduleDelayed(
                APP_STATE_SYNC_DAILY_FLUSH_DELAY, this::runFlushAppStateSyncDaily);
    }

    /**
     * Runs one daily-aggregate flush tick, swallowing any failure so a bad tick
     * cannot escape onto the scheduler's virtual thread.
     *
     * <p>The body of the debounced flush armed by
     * {@link #scheduleAppStateSyncDailyFlush()}; a {@link #flushAppStateSyncDaily()}
     * failure is logged and dropped.
     */
    private void runFlushAppStateSyncDaily() {
        try {
            flushAppStateSyncDaily();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "app-state sync daily aggregate flush failed", e);
        }
    }

    /**
     * Drains {@link #syncdDailyStats} and commits a single
     * {@link MdAppStateSyncDailyEventBuilder MdAppStateSyncDaily} aggregate when
     * at least one counter is non-zero.
     *
     * <p>Each counter is atomically read and reset to zero; only the counters
     * that are strictly positive are set on the event, matching WhatsApp Web's
     * per-field {@code > 0} gate, so a quiet window that accumulated nothing
     * emits no event at all.
     *
     * @implNote
     * This implementation omits {@code crossIndexConflictCount} and
     * {@code keyRotationRemoveCount}: WhatsApp Web's daily accumulator never
     * feeds the former, and the latter is driven by the key-rotation module
     * which lives outside this service, so both stay absent from the aggregate
     * rather than carrying a fabricated value.
     */
    private void flushAppStateSyncDaily() {
        var mutationCount = syncdDailyStats.mutationCount.getAndSet(0);
        var invalidActionCount = syncdDailyStats.invalidActionCount.getAndSet(0);
        var unsupportedActionCount = syncdDailyStats.unsupportedActionCount.getAndSet(0);
        var storedMutationCount = syncdDailyStats.storedMutationCount.getAndSet(0);
        var unsetActionCount = syncdDailyStats.unsetActionCount.getAndSet(0);
        var missingKeyCount = syncdDailyStats.missingKeyCount.getAndSet(0);
        var uploadConflictCount = syncdDailyStats.uploadConflictCount.getAndSet(0);

        if (mutationCount <= 0
                && invalidActionCount <= 0
                && unsupportedActionCount <= 0
                && storedMutationCount <= 0
                && unsetActionCount <= 0
                && missingKeyCount <= 0
                && uploadConflictCount <= 0) {
            return;
        }

        var builder = new MdAppStateSyncDailyEventBuilder();
        if (mutationCount > 0) {
            builder.mutationCount(mutationCount);
        }
        if (invalidActionCount > 0) {
            builder.invalidActionCount(invalidActionCount);
        }
        if (unsupportedActionCount > 0) {
            builder.unsupportedActionCount(unsupportedActionCount);
        }
        if (storedMutationCount > 0) {
            builder.storedMutationCount(storedMutationCount);
        }
        if (unsetActionCount > 0) {
            builder.unsetActionCount(unsetActionCount);
        }
        if (missingKeyCount > 0) {
            builder.missingKeyCount(missingKeyCount);
        }
        if (uploadConflictCount > 0) {
            builder.uploadConflictCount(uploadConflictCount);
        }
        wamService.commit(builder.build());
    }

    /**
     * Cancels the pending daily-aggregate flush if one is armed.
     *
     * <p>Pairs with {@link #scheduleAppStateSyncDailyFlush()}; safe to call when
     * no flush is pending (no-op) and invoked from {@link #reset()} during
     * disconnect. Any counters accumulated since the last flush are dropped, the
     * same as WhatsApp Web's teardown of its shift timer.
     */
    private void stopAppStateSyncDailyFlush() {
        var task = appStateSyncDailyFlushTask;
        if (task != null) {
            task.cancel();
            appStateSyncDailyFlushTask = null;
        }
    }

    /**
     * The live accumulator behind the daily app-state sync aggregate.
     *
     * <p>Each field is an {@link AtomicLong} fed from a mutation-processing
     * chokepoint and drained by {@link #flushAppStateSyncDaily()} via
     * {@link AtomicLong#getAndSet(long)}. The lock-free counters tolerate the
     * benign race of an increment landing during a drain, which telemetry
     * aggregates do not need to serialize against.
     *
     * @implNote
     * This implementation mirrors WhatsApp Web's {@code WAWebSyncdWamAppState}
     * accumulator object, minus the {@code crossIndexConflictCount} and
     * {@code keyRotationRemoveCount} buckets that WhatsApp Web either never
     * populates or feeds from a module outside this service.
     */
    private static final class SyncdDailyStats {
        /**
         * The number of mutations submitted for application across all
         * collections since the last flush.
         */
        private final AtomicLong mutationCount = new AtomicLong();

        /**
         * The number of mutations recorded as
         * {@link SyncActionState#MALFORMED} since the last flush.
         */
        private final AtomicLong invalidActionCount = new AtomicLong();

        /**
         * The number of mutations recorded as
         * {@link SyncActionState#UNSUPPORTED} since the last flush.
         */
        private final AtomicLong unsupportedActionCount = new AtomicLong();

        /**
         * The number of mutations recorded as {@link SyncActionState#SUCCESS}
         * or {@link SyncActionState#SKIPPED} (and thus persisted) since the
         * last flush.
         */
        private final AtomicLong storedMutationCount = new AtomicLong();

        /**
         * The number of mutations whose action index resolved to no action
         * name since the last flush.
         */
        private final AtomicLong unsetActionCount = new AtomicLong();

        /**
         * The number of distinct app-state sync keys found missing during
         * mutation decryption since the last flush.
         */
        private final AtomicLong missingKeyCount = new AtomicLong();

        /**
         * The number of outgoing patch uploads that raced a competing version
         * bump since the last flush.
         */
        private final AtomicLong uploadConflictCount = new AtomicLong();
    }

    /**
     * The mutable per-mutation-name accumulator used by
     * {@link #reportSyncdStats()} while bucketing
     * {@link SyncActionEntry#actionState()} counts.
     *
     * <p>One instance is created per distinct mutation name encountered during
     * the daily walk, and the five counters land directly in the mutation
     * stats WAM event after going through {@link #convertToBucket(int)}.
     *
     * @implNote
     * This implementation uses package-private mutable {@code int} fields for
     * the fastest possible increment loop; the type is never exposed outside
     * the enclosing class.
     */
    private static final class ActionStatCounts {
        /**
         * The number of mutations seen in either {@link SyncActionState#SUCCESS}
         * or {@link SyncActionState#SKIPPED}.
         */
        int applied;

        /**
         * The number of mutations seen in {@link SyncActionState#MALFORMED}.
         */
        int invalid;

        /**
         * The number of mutations seen in {@link SyncActionState#ORPHAN}.
         */
        int orphan;

        /**
         * The number of mutations seen in {@link SyncActionState#UNSUPPORTED}.
         */
        int unsupported;

        /**
         * The number of mutations seen in {@link SyncActionState#FAILED}.
         */
        int failed;
    }

    /**
     * Tears down every background scheduler owned by this service so the JVM
     * can quiesce.
     *
     * <p>Called from the connection-shutdown path during graceful disconnect
     * or logout; pairs with {@link #startPeriodicSyncJob()} as the inverse
     * lifecycle hook. Stops the periodic catch-up sweep, the key-rotation job,
     * both daily WAM stats jobs, the debounced app-state sync daily aggregate
     * flush, the retry backoff scheduler, and the missing-key timeout scheduler.
     * Idempotent.
     *
     * @implNote
     * This implementation forwards to each scheduler's own shutdown method in
     * sequence; no order matters because each is independent.
     */
    @Override
    public void reset() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resetting app-state sync service");
        stopPeriodicSyncJob();
        stopPeriodicReportSyncdStatsJob();
        stopPeriodicReportSyncdKeyStatsJob();
        stopAppStateSyncDailyFlush();
        syncKeyRotationService.stopPeriodicRotationJob();
        retryScheduler.close();
        missingSyncKeyTimeoutScheduler.shutdown();
    }
}


