package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppLidMigrationException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingMode;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMute;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.linked.sync.history.HistorySync;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMapping;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayload;
import com.github.auties00.cobalt.wire.linked.jid.migration.PhoneNumberToLIDMapping;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.setting.GlobalSettings;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.Lid11MigrationLifecycleEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.LidMigrationDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.LidMigrationSourceType;
import com.github.auties00.cobalt.wire.wam.type.MigrationStageEnum;
import com.github.auties00.cobalt.wire.wam.type.StageFailureReasonEnum;

import com.github.auties00.cobalt.util.ScheduledTask;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Drives the per-account 1:1 LID migration and exposes the LID/PN conversion
 * helpers that the rest of the client depends on.
 *
 * <p>WhatsApp is migrating 1:1 conversations from phone-number JIDs to
 * privacy-preserving Linked-Identity (LID) JIDs. A paired companion client
 * waits for the {@code lid_one_on_one_migration_enabled} AB prop, receives a
 * {@link LIDMigrationMappingSyncPayload} from the primary device that describes
 * how every known phone number maps to its assigned LID, rewrites every
 * eligible local chat to the new address, deletes chats that no longer resolve,
 * and persists the mapping table so that outgoing and incoming stanzas can be
 * translated between addressing modes. This service owns that pipeline through
 * the {@link LidMigrationState} machine ({@link LidMigrationState#NOT_STARTED}
 * to {@link LidMigrationState#WAITING_PROP} to
 * {@link LidMigrationState#WAITING_MAPPINGS} to {@link LidMigrationState#READY}
 * to {@link LidMigrationState#IN_PROGRESS} to {@link LidMigrationState#COMPLETE}),
 * with {@link LidMigrationState#DISABLED} and {@link LidMigrationState#FAILED}
 * as terminal off-ramps.
 *
 * <p>Beyond the migration itself, this service also exposes the conversion
 * utilities ({@link #toLid(Jid)}, {@link #toPn(Jid)},
 * {@link #getAlternateMsgKey(MessageKey)},
 * {@link #getMeUserLidOrJidForChat(Chat, TranslateMsgKeyType)}) that the rest
 * of the client uses to move freely between PN and LID representations for
 * messages, chats, and the current user's identity.
 *
 * @implNote
 * This implementation collapses what WhatsApp Web spreads across several
 * UserPrefs-backed modules into a single in-memory service with an explicit
 * {@link AtomicReference}-driven state machine, no IndexedDB persistence layer,
 * and no page-refresh trigger. The migration is auto-started synchronously from
 * {@link #processProtocolMessage(LIDMigrationMappingSyncPayload)} instead of
 * going through a window-refreshing job.
 */
@WhatsAppWebModule(moduleName = "WAWebLid1X1MigrationGating")
@WhatsAppWebModule(moduleName = "WAWebLid1X1ThreadAccountMigrations")
@WhatsAppWebModule(moduleName = "WAWebLid1x1MigrationPrimaryCache")
@WhatsAppWebModule(moduleName = "WAWebLid1x1MigrationManager")
@WhatsAppWebModule(moduleName = "WAWebLid1x1MigrationTimeout")
@WhatsAppWebModule(moduleName = "WAWebLid1x1MigrationTimeoutUtils")
@WhatsAppWebModule(moduleName = "WAWebLid1x1MigrationMsgParser")
@WhatsAppWebModule(moduleName = "WAWebLidMigrationUtils")
@WhatsAppWebModule(moduleName = "WAWebApiContact")
public final class LiveLidMigrationService implements LidMigrationService {
    /**
     * The logger for {@link LiveLidMigrationService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveLidMigrationService.class);

    /**
     * The {@code lidOriginType} marker stamped on a chat whose LID was minted
     * as part of a click-to-WhatsApp flow with phone-number hiding.
     *
     * <p>{@link #resolveThread(Chat, Set)} uses this marker to detect chats
     * that may need their origin marker promoted to
     * {@link #LID_ORIGIN_TYPE_GENERAL} once the primary device confirms the LID
     * is the latest one for the underlying contact.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsernameTypes", exports = "LidOriginType",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String LID_ORIGIN_TYPE_PNH_CTWA = "ctwa";

    /**
     * The {@code lidOriginType} marker stamped on a chat whose LID was not
     * minted through phone-number hiding.
     *
     * <p>This is the target value when {@link #resolveThread(Chat, Set)}
     * promotes a {@link #LID_ORIGIN_TYPE_PNH_CTWA} chat after a primary cache
     * match.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsernameTypes", exports = "LidOriginType",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String LID_ORIGIN_TYPE_GENERAL = "general";

    /**
     * The set of {@link ChatMessageInfo.StubType} values that the migration
     * deletability heuristic treats as non-content noise.
     *
     * <p>This set is consulted by {@link #isMigrationSafeStub(ChatMessageInfo)}
     * as the first gate in {@link #canDeleteChat(Chat)}; a chat consisting
     * entirely of these stubs has no user-visible history and is safe to drop
     * when no LID mapping can be resolved.
     *
     * @implNote
     * This implementation models WhatsApp Web's "initial E2E notification" and
     * "disappearing-mode system message" predicates by enumerating the three
     * {@link ChatMessageInfo.StubType} constants that satisfy either predicate,
     * so the check can be a single {@link EnumSet#contains(Object)} call.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "X",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static final Set<ChatMessageInfo.StubType> MIGRATION_SAFE_STUB_TYPES = EnumSet.of(
            ChatMessageInfo.StubType.E2E_ENCRYPTED,
            ChatMessageInfo.StubType.E2E_ENCRYPTED_NOW,
            ChatMessageInfo.StubType.DISAPPEARING_MODE
    );

    /**
     * The set of {@link ChatMessageInfo.StubType} values that the migration
     * deletability heuristic recognises as call-log entries.
     *
     * <p>This set is consulted by {@link #isCallLogMessage(ChatMessageInfo)} in
     * {@link #allMessagesAreSafeStubsOrCallLog(Collection)} so that chats which
     * contain only stubs and call-history entries (missed or silenced calls)
     * are still considered safe to delete during the 1:1 migration cascade.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "ee",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static final Set<ChatMessageInfo.StubType> CALL_LOG_STUB_TYPES = EnumSet.of(
            ChatMessageInfo.StubType.CALL_MISSED_VOICE,
            ChatMessageInfo.StubType.CALL_MISSED_VIDEO,
            ChatMessageInfo.StubType.CALL_MISSED_GROUP_VOICE,
            ChatMessageInfo.StubType.CALL_MISSED_GROUP_VIDEO,
            ChatMessageInfo.StubType.SILENCED_UNKNOWN_CALLER_AUDIO,
            ChatMessageInfo.StubType.SILENCED_UNKNOWN_CALLER_VIDEO
    );

    /**
     * The owning {@link LinkedWhatsAppClient}, used to surface migration failures
     * through the configurable error handler.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The flat {@link LinkedWhatsAppStore} that persists chats, contacts, and the
     * bidirectional LID/PN mapping table.
     */
    private final LinkedWhatsAppStore store;

    /**
     * The {@link ABPropsService} used to read the AB props that gate the
     * migration.
     *
     * <p>The props read are
     * {@code lid_one_on_one_migration_peer_sync_timeout_in_seconds},
     * {@code lid_one_on_one_migration_compatible}, and
     * {@code lid_one_on_one_migration_log_out_on_mismatch}.
     */
    private final ABPropsService abPropsService;

    /**
     * The {@link WamService} used to commit migration-lifecycle telemetry
     * events.
     */
    private final WamService wamService;

    /**
     * The current position of the migration pipeline, accessed atomically.
     *
     * @implNote
     * This implementation uses an {@link AtomicReference} so the
     * compare-and-set transitions stay lock-free across the connection
     * lifecycle. WhatsApp Web persists the equivalent value to IndexedDB; this
     * implementation keeps the value in memory and recomputes it on each
     * session through {@link #reset()}.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "getLidThreadMigrationStatus",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private final AtomicReference<LidMigrationState> state;

    /**
     * Maps each phone-number user part to the LID the primary device assigned
     * to that contact at migration time.
     *
     * <p>This cache is populated by
     * {@link #processSingleMapping(LIDMigrationMapping)} and
     * {@link #changeLid(Jid, Jid, Jid)}, read by {@link #lookupLid(Jid)}
     * (cache-first) and by {@link #resolveThread(Chat, Set)} when picking the
     * migration target for a PN chat.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationPrimaryCache", exports = "lidPnMigrationPrimaryCache",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private final ConcurrentHashMap<String, Jid> primaryPnToAssignedLidCache;

    /**
     * Maps each phone-number user part to the latest LID known to the primary
     * device, which may be newer than the originally assigned LID if the
     * contact has rotated theirs.
     *
     * <p>This cache is consulted by {@link #resolveThread(Chat, Set)} to detect
     * whether a {@link #LID_ORIGIN_TYPE_PNH_CTWA} chat now matches the primary
     * device's view and should be promoted to {@link #LID_ORIGIN_TYPE_GENERAL},
     * and by {@link #learnMappingsInBulk()} to decide whether the latest LID
     * needs a separate store entry.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationPrimaryCache", exports = "lidPnMigrationPrimaryCache",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private final ConcurrentHashMap<String, Jid> primaryPnToLatestLidCache;

    /**
     * Caches, keyed by phone-number user part, the LID that was known at the
     * moment a phone-number chat was created locally.
     *
     * <p>This cache is populated through {@link #registerOriginalLid(Jid, Jid)}
     * from the create-chat path so that {@link #resolveThread(Chat, Set)} has a
     * last-resort fallback when neither the primary cache nor the bidirectional
     * store mapping has a LID for the contact.
     */
    private final ConcurrentHashMap<String, Jid> originalLidCache;

    /**
     * The most recent chat-DB migration timestamp reported by the primary
     * device.
     *
     * <p>This value is read by {@link #getEffectiveSyncTimestamp()} when
     * classifying a PN chat in {@link #resolveThread(Chat, Set)} under the
     * mismatch-logout branch. It may be overridden by a newer value carried in
     * a {@link HistorySync} via
     * {@link #observeChatDbMigrationTimestamp(Instant)}.
     */
    private volatile Instant chatDbMigrationTimestamp;

    /**
     * The wall-clock time at which the mapping-sync protocol message arrived.
     *
     * <p>This value is read by {@link #getEffectiveSyncTimestamp()} as a
     * fallback when the primary device did not report a
     * {@link #chatDbMigrationTimestamp}, so the staleness comparison in
     * {@link #resolveThread(Chat, Set)} still has a reference point.
     */
    private volatile Instant receiveTimestamp;

    /**
     * The pending scheduled future that fails the migration when peer mappings
     * do not arrive within the AB-prop-defined window.
     *
     * <p>This future is armed by {@link #enableMigration()} and cancelled by
     * {@link #processProtocolMessage(LIDMigrationMappingSyncPayload)} or by
     * {@link #reset()}. It is {@code null} when no timeout is currently armed.
     */
    private volatile ScheduledTask mappingTimeoutFuture;

    /**
     * Constructs a new service bound to the given client, AB props service, and
     * WAM telemetry service.
     *
     * <p>The service is typically instantiated once per {@link LinkedWhatsAppClient}
     * during client wiring and then survives across reconnects via
     * {@link #reset()}.
     *
     * @param whatsapp       the owning {@link LinkedWhatsAppClient}
     * @param abPropsService the {@link ABPropsService} used to read the
     *                       migration AB props
     * @param wamService     the {@link WamService} used to commit lifecycle
     *                       telemetry
     */
    public LiveLidMigrationService(LinkedWhatsAppClient whatsapp, ABPropsService abPropsService, WamService wamService) {
        this.whatsapp = whatsapp;
        this.store = whatsapp.store();
        this.abPropsService = abPropsService;
        this.wamService = wamService;
        this.state = new AtomicReference<>(LidMigrationState.NOT_STARTED);
        this.primaryPnToAssignedLidCache = new ConcurrentHashMap<>();
        this.primaryPnToLatestLidCache = new ConcurrentHashMap<>();
        this.originalLidCache = new ConcurrentHashMap<>();
    }

    /**
     * Returns whether the 1:1 LID migration has completed for this account.
     *
     * <p>Consumers use this flag to decide whether outgoing messages should be
     * addressed by LID or by phone number, and as the gate inside
     * {@link #shouldHaveAccountLid(Jid)}.
     *
     * @implNote
     * This implementation returns {@code true} only when the state machine has
     * reached {@link LidMigrationState#COMPLETE}; WhatsApp Web reads a persisted
     * flag that survives page reloads, while Cobalt rebuilds the state on every
     * session.
     *
     * @return {@code true} if the state machine is at
     *         {@link LidMigrationState#COMPLETE}
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1MigrationGating", exports = "Lid1X1MigrationUtils",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public boolean isLidMigrated() {
        return state.get() == LidMigrationState.COMPLETE;
    }

    /**
     * Returns whether the Syncd session has been migrated to LID.
     *
     * <p>This predicate surfaces a non-LID Syncd session to other modules that
     * gate Syncd traffic on this flag, and always reports {@code false} to
     * match WhatsApp Web's constant stub.
     *
     * @return {@code false}, always
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1MigrationGating", exports = "Lid1X1MigrationUtils",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public boolean isSyncdSessionMigrated() {
        return false;
    }

    /**
     * Returns whether the chat-creation path should still produce a
     * PN-addressed chat.
     *
     * <p>Chat creation always favours LID addressing once the new pipeline is
     * live, so this mirrors WhatsApp Web's constant stub and always reports
     * {@code false}.
     *
     * @return {@code false}, always
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1MigrationGating", exports = "Lid1X1MigrationUtils",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public boolean shouldCreatePnChat() {
        return false;
    }

    /**
     * Returns whether the runtime state disagrees with the persisted LID
     * migration flag.
     *
     * <p>In WhatsApp Web this fires when the in-memory state does not match the
     * persisted account-migrated flag. Cobalt has no separate persisted flag
     * and so cannot diverge from itself.
     *
     * @return {@code false}, always
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1MigrationGating", exports = "Lid1X1MigrationUtils",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public boolean hasStateDiscrepancy() {
        return false;
    }

    /**
     * Returns the current position of the state machine.
     *
     * <p>This is a package-private test seam consumed by the migration unit
     * tests to pin transition behaviour; production code reads
     * {@link #isLidMigrated()} instead.
     *
     * @return the current {@link LidMigrationState}, never {@code null}
     */
    LidMigrationState state() {
        return state.get();
    }

    /**
     * Returns whether the primary device has already delivered the peer-mapping
     * sync to this companion.
     *
     * <p>The blocklist fetch path uses this to decide whether a LID-addressed
     * blocklist arriving on an unmigrated device can be deferred until the 1:1
     * LID migration completes (peer mappings already in flight) or must be
     * treated as a hard error (peer mappings never delivered).
     *
     * @return {@code true} when the current state is
     *         {@link LidMigrationState#READY},
     *         {@link LidMigrationState#IN_PROGRESS}, or
     *         {@link LidMigrationState#COMPLETE}
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationTimeoutUtils",
            exports = "PEER_MAPPING_RECEIVED_STATUSES", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public boolean hasReceivedPeerMappings() {
        var current = state.get();
        return current == LidMigrationState.READY
                || current == LidMigrationState.IN_PROGRESS
                || current == LidMigrationState.COMPLETE;
    }

    /**
     * Arms the state machine to react to the AB prop flip by moving
     * {@link LidMigrationState#NOT_STARTED} to
     * {@link LidMigrationState#WAITING_PROP}.
     *
     * <p>This method is called once after the connection is established and
     * before any protocol traffic is processed. It is idempotent; subsequent
     * calls when the state has already advanced are silently ignored so that
     * reconnect handlers can call it unconditionally.
     */
    @Override
    public void initialize() {
        if (state.compareAndSet(LidMigrationState.NOT_STARTED, LidMigrationState.WAITING_PROP)) {
            if (Log.INFO) LOGGER.log(Level.INFO, "LID migration initialized, waiting for AB prop");
        }
    }

    /**
     * Activates the migration when the AB prop reports it is enabled by
     * advancing {@link LidMigrationState#WAITING_PROP} to
     * {@link LidMigrationState#WAITING_MAPPINGS} and arming the peer-mapping
     * arrival timeout.
     *
     * <p>When the {@code lid_one_on_one_migration_enabled} AB prop transitions
     * to {@code true} this transitions the state machine and schedules a
     * deferred failure that fires if the primary device never delivers the
     * mapping sync within
     * {@link ABProp#LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS}
     * seconds. A timeout value of zero disables the scheduled task. When the
     * timeout fires, the failure is split into two distinct exception subtypes:
     * {@link WhatsAppLidMigrationException.StateDiscrepancy} when
     * {@link #hasStateDiscrepancy()} reports drift between the local view and
     * the primary's, and
     * {@link WhatsAppLidMigrationException.PeerMappingsNotReceived} otherwise.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "checkIfMigrationEnabled",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationTimeout", exports = "scheduleLogoutIfNeeded",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void enableMigration() {
        if (state.compareAndSet(LidMigrationState.WAITING_PROP, LidMigrationState.WAITING_MAPPINGS)) {
            if (Log.INFO) LOGGER.log(Level.INFO, "LID migration enabled, waiting for mappings from primary");

            var timeoutSeconds = abPropsService.getInt(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS);
            if (timeoutSeconds == 0) {
                if (Log.INFO) LOGGER.log(Level.INFO, "LID migration peer sync timeout disabled by AB prop");
                return;
            }

            mappingTimeoutFuture = ScheduledTask.scheduleDelayed(
                    Duration.ofSeconds(timeoutSeconds),
                    () -> {
                        if (state.get() == LidMigrationState.WAITING_MAPPINGS) {
                            if (Log.WARNING) LOGGER.log(Level.WARNING,
                                    "LID migration timed out after {0}s waiting for mappings", timeoutSeconds);
                            var hasDiscrepancy = hasStateDiscrepancy();
                            var failureReason = hasDiscrepancy
                                    ? StageFailureReasonEnum.COMPANION_UNSUPPORTED_VERSION
                                    : StageFailureReasonEnum.COMPANION_TIMEOUT_BASED_ON_DEVICE_CAPABILITY;
                            wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                                    .migrationStage(MigrationStageEnum.COMPANION_LOCAL_MIGRATION_FAILED)
                                    .stageFailureReason(failureReason)
                                    .isLocally1x1MigratedFromDb(isLidMigrated())
                                    .build());
                            handleError(hasDiscrepancy
                                    ? new WhatsAppLidMigrationException.StateDiscrepancy()
                                    : new WhatsAppLidMigrationException.PeerMappingsNotReceived());
                        }
                    });
        }
    }

    /**
     * Parks the state machine at {@link LidMigrationState#DISABLED} when the
     * server-sent AB prop indicates the migration is not enabled for this
     * account.
     *
     * <p>This is the companion to {@link #enableMigration()}: when AB-prop
     * polling confirms the migration is not enabled, this terminal off-ramp
     * prevents the timeout future from being armed and keeps the service inert
     * for the lifetime of the session. It is idempotent and only fires from
     * {@link LidMigrationState#WAITING_PROP}.
     */
    @Override
    public void disableMigration() {
        if (state.compareAndSet(LidMigrationState.WAITING_PROP, LidMigrationState.DISABLED)) {
            if (Log.INFO) LOGGER.log(Level.INFO, "LID migration disabled");
        }
    }

    /**
     * Ingests a mapping-sync protocol message received from the primary device,
     * populates the primary caches, advances the state machine to
     * {@link LidMigrationState#READY}, and auto-starts
     * {@link #executeMigration()}.
     *
     * <p>This method is invoked by the protocol-message receiver after the
     * primary device's mapping payload has been decoded. A {@code null} payload
     * is treated as a malformed peer message and routed through
     * {@link LinkedWhatsAppClient#handleFailure(com.github.auties00.cobalt.exception.WhatsAppException)}
     * as a {@link WhatsAppLidMigrationException.FailedToParseMappings}; an empty
     * mapping list is treated as malformed and routed as a
     * {@link WhatsAppLidMigrationException.PeerMappingsMalformed}. Payloads
     * delivered outside {@link LidMigrationState#WAITING_PROP} or
     * {@link LidMigrationState#WAITING_MAPPINGS} are silently dropped.
     *
     * @implNote
     * This implementation captures {@link Instant#now()} as the
     * {@link #receiveTimestamp} fallback before processing, then cancels the
     * pending {@link #mappingTimeoutFuture}, walks the mappings through
     * {@link #processSingleMapping(LIDMigrationMapping)}, sets the state to
     * {@link LidMigrationState#READY}, and finally calls
     * {@link #executeMigration()} synchronously on the current virtual thread.
     * WhatsApp Web instead persists the payload and schedules a page refresh to
     * drive the migration.
     *
     * @param payload the decoded mapping payload from the primary device, or
     *                {@code null} when parsing failed
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "setLidMigrationMappings",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationMsgParser", exports = "parseLidMigrationMappingSyncMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void processProtocolMessage(LIDMigrationMappingSyncPayload payload) {
        if (payload == null) {
            wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                    .migrationStage(MigrationStageEnum.COMPANION_LOCAL_MIGRATION_FAILED)
                    .stageFailureReason(StageFailureReasonEnum.MALFORMED_PEER_MESSAGE)
                    .build());
            handleError(new WhatsAppLidMigrationException.FailedToParseMappings("null payload"));
            return;
        }

        var currentState = state.get();
        if (currentState != LidMigrationState.WAITING_MAPPINGS && currentState != LidMigrationState.WAITING_PROP) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "Ignoring mappings in state: {0}", currentState);
            return;
        }

        try {
            var timeout = mappingTimeoutFuture;
            if (timeout != null) {
                timeout.cancel();
                mappingTimeoutFuture = null;
            }

            this.receiveTimestamp = Instant.now();

            wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                    .migrationStage(MigrationStageEnum.COMPANION_RECEIVED_PEER_MESSAGE)
                    .build());

            var mappings = payload.pnToLidMappings();

            if (mappings.isEmpty()) {
                wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                        .migrationStage(MigrationStageEnum.COMPANION_LOCAL_MIGRATION_FAILED)
                        .stageFailureReason(StageFailureReasonEnum.MALFORMED_PEER_MESSAGE)
                        .build());
                handleError(new WhatsAppLidMigrationException.PeerMappingsMalformed("empty mappings"));
                return;
            }

            this.chatDbMigrationTimestamp = payload.chatDbMigrationTimestamp()
                    .orElse(null);

            if (Log.INFO) LOGGER.log(Level.INFO, "Processing {0} LID mappings from primary", mappings.size());

            for (var mapping : mappings) {
                processSingleMapping(mapping);
            }

            state.set(LidMigrationState.READY);
            if (Log.INFO) LOGGER.log(Level.INFO, "LID migration ready with {0} assigned mappings, {1} latest mappings",
                    primaryPnToAssignedLidCache.size(), primaryPnToLatestLidCache.size());

            if (shouldAutoStartMigration()) {
                executeMigration();
            }

        } catch (Throwable throwable) {
            handleError(new WhatsAppLidMigrationException.FailedToParseMappings("error processing mappings", throwable));
        }
    }

    /**
     * Absorbs a candidate chat-DB migration timestamp, keeping the newest value
     * seen across all sources.
     *
     * <p>This method is called from the history-sync handler and from any other
     * ingestion path that learns of a primary-device migration timestamp; the
     * recorded value feeds {@link #getEffectiveSyncTimestamp()}, which the
     * staleness branch of {@link #resolveThread(Chat, Set)} uses to decide
     * whether the primary's mappings are obsolete compared to a local chat's
     * last activity. A {@code null} input is a tolerated no-op so callers can
     * pipe through optional fields directly.
     *
     * @param timestamp the observed timestamp, or {@code null} to leave the
     *                  recorded value untouched
     */
    @Override
    public void observeChatDbMigrationTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return;
        }

        if (chatDbMigrationTimestamp == null || timestamp.isAfter(chatDbMigrationTimestamp)) {
            chatDbMigrationTimestamp = timestamp;
        }
    }

    /**
     * Harvests LID mappings, conversation pairings, and an optional chat-DB
     * timestamp from a {@link HistorySync} payload.
     *
     * <p>This method is invoked by the history-sync ingestion path so that
     * mappings already present in the primary device's database are reflected
     * locally even before the primary delivers a dedicated mapping-sync
     * protocol message. Two sources are inspected: the flat
     * {@link HistorySync#phoneNumberToLidMappings()} table and the
     * per-conversation PN/LID pair on each {@link Chat}. A
     * {@link GlobalSettings#chatDbLidMigrationTimestamp()} carried in the same
     * payload is absorbed through
     * {@link #observeChatDbMigrationTimestamp(Instant)}.
     *
     * @implNote
     * This implementation deliberately writes only to the bidirectional store
     * mapping and to the contact LID; it does not touch
     * {@link #primaryPnToAssignedLidCache} or {@link #primaryPnToLatestLidCache},
     * because the primary caches are reserved for the mapping-sync protocol
     * message. The {@link #chatDbMigrationTimestamp} field is updated only when
     * the history sync carries a newer value than the one already recorded.
     *
     * @param historySync the decoded {@link HistorySync}, or {@code null} for a
     *                    tolerated no-op
     */
    @Override
    public void processHistorySync(HistorySync historySync) {
        if (historySync == null) {
            return;
        }

        var mappingsProcessed = 0;

        var topLevelMappings = historySync.phoneNumberToLidMappings();
        if (topLevelMappings != null && !topLevelMappings.isEmpty()) {
            for (var mapping : topLevelMappings) {
                if (processPhoneNumberToLidMapping(mapping)) {
                    mappingsProcessed++;
                }
            }
        }

        var conversations = historySync.chats();
        if (conversations != null) {
            for (var conversation : conversations) {
                if (processConversationLidData(conversation)) {
                    mappingsProcessed++;
                }
            }
        }

        var chatDbLidMigrationTimestamp = historySync.globalSettings()
                .flatMap(GlobalSettings::chatDbLidMigrationTimestamp);
        if (chatDbLidMigrationTimestamp.isPresent()) {
            if (chatDbMigrationTimestamp == null || chatDbLidMigrationTimestamp.get().isAfter(chatDbMigrationTimestamp)) {
                this.chatDbMigrationTimestamp = chatDbLidMigrationTimestamp.get();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "Updated chatDbMigrationTimestamp from GlobalSettings: {0}", chatDbLidMigrationTimestamp.get());
            }
        }

        if (mappingsProcessed > 0) {
            if (Log.INFO) LOGGER.log(Level.INFO,
                    "Processed {0} LID mappings from history sync (type={1})",
                    mappingsProcessed, historySync.syncType());
        }
    }

    /**
     * Registers a single {@link PhoneNumberToLIDMapping} entry from a
     * history-sync payload into the bidirectional store mapping and onto the
     * matching contact, if any.
     *
     * <p>This method is called per entry by
     * {@link #processHistorySync(HistorySync)}; an entry missing either side of
     * the pair is silently skipped.
     *
     * @implNote
     * This implementation does not touch {@link #primaryPnToAssignedLidCache} or
     * {@link #primaryPnToLatestLidCache}: history-sync mappings are
     * authoritative only for the general store, and the primary caches remain
     * reserved for {@link #processSingleMapping(LIDMigrationMapping)}.
     *
     * @param mapping the mapping entry to process, or {@code null}
     * @return {@code true} when a valid pair was registered
     */
    private boolean processPhoneNumberToLidMapping(PhoneNumberToLIDMapping mapping) {
        if (mapping == null) {
            return false;
        }

        var pnJid = mapping.pnJid().orElse(null);
        var lidJid = mapping.lidJid().orElse(null);

        if (pnJid == null || lidJid == null) {
            return false;
        }

        store.contactStore().registerLidMapping(pnJid, lidJid);
        store.contactStore().findContactByJid(pnJid).ifPresent(contact -> contact.setLid(lidJid));

        return true;
    }

    /**
     * Reconciles the per-conversation LID/PN pair carried in a history-sync
     * entry with the chat, the store, and any known contact.
     *
     * <p>This method is package-private so the history-sync test suite can
     * drive the conversation branch directly; it is not called from outside the
     * migration package in production. Only 1:1 chats are considered; group,
     * newsletter, and broadcast servers are skipped.
     *
     * @implNote
     * This implementation derives the PN from {@link Chat#phoneNumberJid()}
     * when the chat is keyed by LID, and derives the LID from {@link Chat#lid()}
     * when the chat is keyed by PN. Once a complete pair is recovered, the
     * bidirectional mapping is registered, the contact LID is updated, and the
     * chat is updated with the missing side so its addressing-mode companion is
     * always populated.
     *
     * @param conversation the conversation to process, or {@code null}
     * @return {@code true} when a complete pair was registered
     */
    boolean processConversationLidData(Chat conversation) {
        if (conversation == null) {
            return false;
        }

        var chatJid = conversation.jid();

        if (!chatJid.hasUserServer() && !chatJid.hasLidServer()) {
            return false;
        }

        final Jid phoneJid;
        final Jid lidJid;

        if (chatJid.hasLidServer()) {
            phoneJid = conversation.phoneNumberJid().orElse(null);
            lidJid = chatJid;
        } else if (chatJid.hasUserServer()) {
            phoneJid = chatJid;
            lidJid = conversation.lid().orElse(null);
        } else {
            phoneJid = null;
            lidJid = null;
        }

        if (phoneJid == null || lidJid == null) {
            return false;
        }

        store.contactStore().registerLidMapping(phoneJid, lidJid);
        store.contactStore().findContactByJid(phoneJid).ifPresent(contact -> contact.setLid(lidJid));
        conversation.setLid(lidJid);
        if (!phoneJid.equals(chatJid)) {
            conversation.setPhoneNumberJid(phoneJid);
        }

        return true;
    }

    /**
     * Folds one mapping entry from the primary device into the primary caches
     * and mirrors the assigned LID onto any matching local contact.
     *
     * <p>This method is called per entry inside the mapping loop of
     * {@link #processProtocolMessage(LIDMigrationMappingSyncPayload)}. A
     * {@code null} entry is a tolerated no-op so the surrounding loop can keep
     * going.
     *
     * @implNote
     * This implementation populates {@link #primaryPnToAssignedLidCache}
     * unconditionally, {@link #primaryPnToLatestLidCache} only when the mapping
     * declares a {@link LIDMigrationMapping#latestLid()}, and the bidirectional
     * store mapping only when a contact for the PN already exists. The
     * latest-LID cache feeds the click-to-WhatsApp origin promotion in
     * {@link #resolveThread(Chat, Set)}; the eager store registration here is
     * later complemented by {@link #learnMappingsInBulk()}, which sweeps every
     * entry into the store after the migration completes.
     *
     * @param mapping the mapping to fold, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationPrimaryCache", exports = "lidPnMigrationPrimaryCache",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void processSingleMapping(LIDMigrationMapping mapping) {
        if (mapping == null) {
            return;
        }

        var jid = mapping.pn();
        var user = jid.user();

        var assignedLid = mapping.assignedLid();
        primaryPnToAssignedLidCache.put(user, assignedLid);

        mapping.latestLid().ifPresent(latest ->
                primaryPnToLatestLidCache.put(user, latest)
        );

        store.contactStore().findContactByJid(jid).ifPresent(contact -> {
            contact.setLid(assignedLid);
            store.contactStore().registerLidMapping(jid, assignedLid);
        });
    }

    /**
     * Sweeps every chat in the store through the migration cascade, advances
     * the state machine to {@link LidMigrationState#COMPLETE}, and finally
     * flushes the primary caches into the bidirectional mapping table.
     *
     * <p>This method is auto-invoked from
     * {@link #processProtocolMessage(LIDMigrationMappingSyncPayload)} once the
     * primary device delivers its mapping sync, but exposed publicly so callers
     * can re-drive the sweep if needed. It blocks until
     * {@link LinkedWhatsAppStore#waitForOfflineDeliveryEnd()} completes so that no
     * message arriving from the Signal offline delivery window can race the
     * chat-rewriting loop. It aborts via
     * {@link LinkedWhatsAppClient#handleFailure(com.github.auties00.cobalt.exception.WhatsAppException)}
     * if the {@link ABProp#LID_ONE_ON_ONE_MIGRATION_COMPATIBLE} kill switch is
     * off.
     *
     * @implNote
     * This implementation pre-computes the set of user-level JIDs already keyed
     * by LID so that {@link #resolveThread(Chat, Set)} can detect split-thread
     * collisions without a per-chat scan. Each chat is classified into a
     * {@link LidMigrationResolution}, the resolutions are applied via
     * {@link #executeResolutions(List)}, the state is set to
     * {@link LidMigrationState#COMPLETE} before the bulk learn runs, and
     * {@link #learnMappingsInBulk()} promotes the in-memory primary caches to
     * the store. Lifecycle WAM events are committed at every stage. Any
     * {@link WhatsAppLidMigrationException} thrown by
     * {@link #resolveThread(Chat, Set)} surfaces a
     * {@link MigrationStageEnum#COMPANION_LOCAL_MIGRATION_FAILED} event and
     * routes through
     * {@link LinkedWhatsAppClient#handleFailure(com.github.auties00.cobalt.exception.WhatsAppException)};
     * any other {@link Throwable} is wrapped in
     * {@link WhatsAppLidMigrationException.OneOnOneThreadMigrationInternalError}.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "migrate1x1Chats",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void executeMigration() {
        if (!state.compareAndSet(LidMigrationState.READY, LidMigrationState.IN_PROGRESS)) {
            var currentState = state.get();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "Cannot start migration in state: {0}", currentState);
            return;
        }

        wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                .migrationStage(MigrationStageEnum.COMPANION_LOCAL_MIGRATION_STARTED)
                .mappingCount(primaryPnToAssignedLidCache.size())
                .build());

        if (!abPropsService.getBool(ABProp.LID_ONE_ON_ONE_MIGRATION_COMPATIBLE)) {
            wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                    .migrationStage(MigrationStageEnum.COMPANION_LOCAL_MIGRATION_FAILED)
                    .stageFailureReason(StageFailureReasonEnum.COMPANION_UNSUPPORTED_VERSION)
                    .build());
            handleError(new WhatsAppLidMigrationException.IncompatibleClient());
            return;
        }

        if (Log.INFO) LOGGER.log(Level.INFO, "Starting LID migration execution, waiting for offline delivery");
        store.connectionStore().waitForOfflineDeliveryEnd();
        if (Log.INFO) LOGGER.log(Level.INFO, "Offline delivery complete, proceeding with migration");

        try {
            var resolutions = new ArrayList<LidMigrationResolution>();
            var chatsToProcess = new ArrayList<>(store.chatStore().chats());

            var existingLidThreads = new HashSet<Jid>();
            for (var chat : chatsToProcess) {
                if (chat.jid().hasLidServer()) {
                    existingLidThreads.add(chat.jid().toUserJid());
                }
            }

            var companionHasADifferentMappingCount = 0;
            var chatNotInMappingCount = 0;
            var migratedThreadCount = 0;

            for (var chat : chatsToProcess) {
                var resolution = resolveThread(chat, existingLidThreads);
                resolutions.add(resolution);

                var chatJid = chat.jid();
                if (chatJid.hasUserServer()) {
                    var user = chatJid.user();
                    var latestLocalLid = user != null ? store.contactStore().findLidByPhone(chatJid).orElse(null) : null;
                    if (latestLocalLid == null) {
                        chatNotInMappingCount++;
                    }
                    var primaryLid = user != null ? primaryPnToAssignedLidCache.get(user) : null;
                    var latestLocalLidUser = latestLocalLid != null ? latestLocalLid.toUserJid() : null;
                    var primaryLidUser = primaryLid != null ? primaryLid.toUserJid() : null;
                    if (!Objects.equals(latestLocalLidUser, primaryLidUser)) {
                        companionHasADifferentMappingCount++;
                    }
                }

                switch (resolution) {
                    case LidMigrationResolution.Migrate _ -> migratedThreadCount++;
                    case LidMigrationResolution.Keep keep -> {
                        if (keep.reason() == LidMigrationResolution.KeepReason.ALREADY_LID) {
                            migratedThreadCount++;
                        }
                    }
                    case LidMigrationResolution.Delete _ -> {}
                }
            }

            executeResolutions(resolutions);

            state.set(LidMigrationState.COMPLETE);
            if (Log.INFO) LOGGER.log(Level.INFO, "LID migration completed");

            learnMappingsInBulk();

            var latestMappingCount = primaryPnToLatestLidCache.size();
            wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                    .migrationStage(MigrationStageEnum.COMPANION_LOCAL_MIGRATION_ENDED)
                    .mappingCount(primaryPnToAssignedLidCache.size())
                    .migratedThreadCount(migratedThreadCount)
                    .companionHasADifferentMappingCount(companionHasADifferentMappingCount)
                    .chatNotInMappingCount(chatNotInMappingCount)
                    .latestMappingCount(latestMappingCount)
                    .build());

            emitLidMigrationDailyCensus(LidMigrationSourceType.PEER);

        } catch (WhatsAppLidMigrationException e) {
            wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                    .migrationStage(MigrationStageEnum.COMPANION_LOCAL_MIGRATION_FAILED)
                    .stageFailureReason(StageFailureReasonEnum.INITIATED_LOGOUT_BASED_ON_MAPPING)
                    .build());
            handleError(e);
        } catch (Throwable throwable) {
            wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                    .migrationStage(MigrationStageEnum.COMPANION_LOCAL_MIGRATION_FAILED)
                    .stageFailureReason(StageFailureReasonEnum.INTERNAL_ERROR)
                    .build());
            handleError(new WhatsAppLidMigrationException.OneOnOneThreadMigrationInternalError(throwable));
        }
    }

    /**
     * Classifies a single chat thread into a {@link LidMigrationResolution}
     * that describes whether to migrate, keep, or delete the chat.
     *
     * <p>The decision cascade is, in order: an already-LID chat (with an
     * opportunistic click-to-WhatsApp origin promotion) becomes a
     * {@link LidMigrationResolution.Keep}; a group, newsletter, broadcast, or
     * bot server becomes a typed keep; a chat marked as a duplicate-will-merge
     * is kept; otherwise the primary-assigned cache, then the local LID, then
     * the {@link #originalLidCache} are consulted to produce a
     * {@link LidMigrationResolution.Migrate}; if no LID can be resolved the chat
     * is deleted when {@link #canDeleteChat(Chat)} agrees, otherwise the
     * migration fails with
     * {@link WhatsAppLidMigrationException.NoLidAvailable}.
     *
     * @implNote
     * This implementation consults {@link #primaryPnToAssignedLidCache} for the
     * primary's view, not the merged {@link #primaryPnToLatestLidCache}. The
     * {@link ABProp#LID_ONE_ON_ONE_MIGRATION_LOG_OUT_ON_MISMATCH} branch uses a
     * non-strict comparison so a local chat whose timestamp is at or after
     * {@link #getEffectiveSyncTimestamp()} is considered fresher than the
     * primary's mapping and forces a
     * {@link WhatsAppLidMigrationException.PrimaryMappingsObsolete}.
     *
     * @param chat               the chat to classify
     * @param existingLidThreads the user-level JIDs of every chat already keyed
     *                           by LID, used to detect split-thread collisions
     * @return the {@link LidMigrationResolution} to apply
     * @throws WhatsAppLidMigrationException.PrimaryMappingsObsolete if the local
     *         chat is fresher than the primary mapping table and the mismatch
     *         AB prop is on
     * @throws WhatsAppLidMigrationException.NoLidAvailable if a non-deletable
     *         chat has no LID mapping anywhere
     * @throws WhatsAppLidMigrationException.SplitThreadMismatch if the local LID
     *         would collide with an existing LID thread
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "getResolvedThreadAccountLid",
            adaptation = WhatsAppAdaptation.ADAPTED)
    LidMigrationResolution resolveThread(Chat chat, Set<Jid> existingLidThreads) {
        var jid = chat.jid();

        if (jid.hasLidServer()) {
            if (LID_ORIGIN_TYPE_PNH_CTWA.equals(chat.lidOriginType().orElse(null))) {
                var matchesPrimary = primaryPnToLatestLidCache.values().stream()
                        .anyMatch(latestLid -> latestLid.toUserJid().equals(jid.toUserJid()));
                if (matchesPrimary) {
                    chat.setLidOriginType(LID_ORIGIN_TYPE_GENERAL);
                }
            }
            return new LidMigrationResolution.Keep(jid, LidMigrationResolution.KeepReason.ALREADY_LID);
        }

        if (jid.hasGroupOrCommunityServer()) {
            return new LidMigrationResolution.Keep(jid, LidMigrationResolution.KeepReason.GROUP_OR_COMMUNITY);
        }

        if (jid.hasNewsletterServer()) {
            return new LidMigrationResolution.Keep(jid, LidMigrationResolution.KeepReason.NEWSLETTER);
        }

        if (jid.hasBroadcastServer()) {
            if (jid.isStatusBroadcastAccount()) {
                return new LidMigrationResolution.Keep(jid, LidMigrationResolution.KeepReason.STATUS_BROADCAST);
            }
            return new LidMigrationResolution.Keep(jid, LidMigrationResolution.KeepReason.BROADCAST);
        }

        if (jid.hasBotServer()) {
            return new LidMigrationResolution.Keep(jid, LidMigrationResolution.KeepReason.BOT);
        }

        if (chat.phoneNumberhDuplicateLidThread()) {
            return new LidMigrationResolution.Keep(jid, LidMigrationResolution.KeepReason.DUPLICATE_WILL_MERGE);
        }

        var chatLid = chat.lid().orElse(null);
        var user = jid.user();
        var primaryLid = user != null
                ? primaryPnToAssignedLidCache.get(user)
                : null;
        var localLid = chatLid != null
                ? chatLid
                : (user != null ? store.contactStore().findLidByPhone(jid).orElse(null) : null);

        if (primaryLid != null) {
            if (localLid == null || localLid.toUserJid().equals(primaryLid.toUserJid())) {
                return new LidMigrationResolution.Migrate(jid, primaryLid);
            }

            if (abPropsService.getBool(ABProp.LID_ONE_ON_ONE_MIGRATION_LOG_OUT_ON_MISMATCH)) {
                var chatTimestamp = chat.conversationTimestamp();
                var effectiveSyncTimestamp = getEffectiveSyncTimestamp();
                if (chatTimestamp.isPresent() && !chatTimestamp.get().isBefore(effectiveSyncTimestamp)) {
                    throw new WhatsAppLidMigrationException.PrimaryMappingsObsolete();
                }
            }

            return new LidMigrationResolution.Migrate(jid, primaryLid);
        }

        if (localLid != null) {
            if (existingLidThreads.contains(localLid.toUserJid())) {
                throw new WhatsAppLidMigrationException.SplitThreadMismatch();
            }
            return new LidMigrationResolution.Migrate(jid, localLid);
        }

        var cachedOriginalLid = user != null ? originalLidCache.get(user) : null;
        if (cachedOriginalLid != null) {
            return new LidMigrationResolution.Migrate(jid, cachedOriginalLid.toUserJid());
        }

        if (!canDeleteChat(chat)) {
            throw new WhatsAppLidMigrationException.NoLidAvailable();
        }

        return new LidMigrationResolution.Delete(jid, LidMigrationResolution.DeleteReason.NO_LID_MAPPING);
    }

    /**
     * Classifies the given chat after computing the collision set from the
     * current store snapshot.
     *
     * <p>This convenience entry-point is for callers (notably the migration
     * test suite) that classify a single chat in isolation rather than inside
     * the {@link #executeMigration()} sweep, which precomputes the collision
     * set once per pass for efficiency.
     *
     * @param chat the chat to classify
     * @return the {@link LidMigrationResolution} chosen by
     *         {@link #resolveThread(Chat, Set)}
     */
    LidMigrationResolution resolveThread(Chat chat) {
        var existingLidThreads = new HashSet<Jid>();
        for (var stored : store.chatStore().chats()) {
            if (stored.jid().hasLidServer()) {
                existingLidThreads.add(stored.jid().toUserJid());
            }
        }
        return resolveThread(chat, existingLidThreads);
    }

    /**
     * Returns the effective timestamp the migration uses to compare the
     * freshness of local chats against the primary's mapping table.
     *
     * <p>This value is consulted by {@link #resolveThread(Chat, Set)} under the
     * {@link ABProp#LID_ONE_ON_ONE_MIGRATION_LOG_OUT_ON_MISMATCH} branch to
     * decide whether a local chat is fresher than the primary's view and should
     * fail the migration with
     * {@link WhatsAppLidMigrationException.PrimaryMappingsObsolete}.
     *
     * @implNote
     * This implementation prefers {@link #chatDbMigrationTimestamp} (the
     * primary-reported value, potentially refreshed by a history sync), falls
     * back to {@link #receiveTimestamp} (the wall-clock arrival of the mapping
     * sync), and finally returns {@link Instant#EPOCH} so the comparison in the
     * caller is always defined.
     *
     * @return the effective sync timestamp, never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationPrimaryCache", exports = "lidPnMigrationPrimaryCache",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Instant getEffectiveSyncTimestamp() {
        if (chatDbMigrationTimestamp != null) {
            return chatDbMigrationTimestamp;
        }
        if (receiveTimestamp != null) {
            return receiveTimestamp;
        }
        return Instant.EPOCH;
    }

    /**
     * Returns whether the given chat is safe to delete when the migration
     * cannot find a LID mapping for it.
     *
     * <p>This method is consulted by {@link #resolveThread(Chat, Set)} as the
     * last gate before throwing
     * {@link WhatsAppLidMigrationException.NoLidAvailable}: if the chat has no
     * user-meaningful history it is dropped, otherwise the migration aborts.
     *
     * @implNote
     * This implementation walks the chat's messages once and applies three
     * orthogonal rules. The broadcast exemption short-circuits when every
     * message is a safe stub or a broadcast and the
     * {@link LinkedWhatsAppStore#pairingTimestamp()} is at or before the oldest
     * message timestamp (interpreted as "the broadcast existed before this
     * device joined"). Any chat carrying ephemeral settings, lock, archive, or
     * mute state is preserved unless the ephemeral-account-setting exemption
     * applies. Otherwise the chat is deletable when every message is a
     * migration-safe stub, when every message is a safe stub or a call-log
     * entry, or when the broadcast exemption fired.
     *
     * @param chat the chat to evaluate
     * @return {@code true} when the chat may be deleted
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "K",
            adaptation = WhatsAppAdaptation.ADAPTED)
    boolean canDeleteChat(Chat chat) {
        List<ChatMessageInfo> messages;
        try (var stream = chat.messages()) {
            messages = stream.toList();
        }

        var broadcastExempt = false;
        if (allMessagesAreSafeStubsOrBroadcast(messages)) {
            var oldestMessageTs = getOldestMessageTimestamp(messages);
            if (oldestMessageTs != null && isPairingTimestampAtOrBefore(oldestMessageTs)) {
                broadcastExempt = true;
            }
        }

        if (hasEphemeralSettings(chat) && !isEphemeralExempt(chat, messages)) {
            return false;
        }

        if (chat.locked()) {
            return false;
        }

        if (chat.archived()) {
            return false;
        }

        if (chat.mute().map(ChatMute::isMuted).orElse(false)) {
            return false;
        }

        return allMessagesAreSafeStubs(messages)
                || allMessagesAreSafeStubsOrCallLog(messages)
                || broadcastExempt;
    }

    /**
     * Returns whether the given chat has any disappearing-message settings
     * configured.
     *
     * <p>This method is used by {@link #canDeleteChat(Chat)} as the cheap
     * precondition for the ephemeral block; chats without ephemeral state never
     * fall into the ephemeral exemption path.
     *
     * @param chat the chat to inspect
     * @return {@code true} when the chat carries an ephemeral duration or an
     *         ephemeral-setting timestamp
     */
    private boolean hasEphemeralSettings(Chat chat) {
        return chat.ephemeralExpiration().isPresent() || chat.ephemeralSettingTimestamp().isPresent();
    }

    /**
     * Returns whether the given chat is exempt from the ephemeral-blocks-delete
     * rule.
     *
     * <p>This method allows {@link #canDeleteChat(Chat)} to keep a chat
     * deletable when its disappearing-mode state was set by the account-default
     * mechanism (and a corresponding system message is therefore present)
     * rather than by an explicit per-chat configuration the user would notice
     * losing.
     *
     * @implNote
     * This implementation requires the
     * {@link ChatDisappearingMode.Trigger#ACCOUNT_SETTING} trigger combined with
     * at least one {@link ChatMessageInfo.StubType#DISAPPEARING_MODE} system
     * message in the chat.
     *
     * @param chat     the chat to inspect
     * @param messages the chat's messages, already materialised by
     *                 {@link #canDeleteChat(Chat)}
     * @return {@code true} when the chat is exempt from the ephemeral block
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "re",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isEphemeralExempt(Chat chat, Collection<ChatMessageInfo> messages) {
        var trigger = chat.disappearingMode()
                .flatMap(ChatDisappearingMode::trigger)
                .orElse(null);
        if (trigger != ChatDisappearingMode.Trigger.ACCOUNT_SETTING) {
            return false;
        }

        return messages.stream().anyMatch(msg -> {
            var stubType = msg.messageStubType().orElse(null);
            return stubType == ChatMessageInfo.StubType.DISAPPEARING_MODE;
        });
    }

    /**
     * Returns whether every message in the collection is either a
     * migration-safe stub or a broadcast, with at least one broadcast present.
     *
     * <p>This method gates the broadcast exemption branch of
     * {@link #canDeleteChat(Chat)}; combined with the pairing-time comparison,
     * it lets the migration drop chats whose only non-stub content is broadcast
     * traffic the device received before pairing.
     *
     * @param messages the messages to inspect
     * @return {@code true} when the broadcast-and-stubs rule applies
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "te",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean allMessagesAreSafeStubsOrBroadcast(Collection<ChatMessageInfo> messages) {
        var hasBroadcast = false;
        for (var msg : messages) {
            if (isMigrationSafeStub(msg)) {
                continue;
            }
            if (msg.broadcast()) {
                hasBroadcast = true;
                continue;
            }
            return false;
        }
        return hasBroadcast;
    }

    /**
     * Returns the earliest {@link ChatMessageInfo#timestamp()} found among the
     * given messages.
     *
     * <p>This method is used by {@link #canDeleteChat(Chat)} to anchor the
     * broadcast-exemption pairing-time comparison; the migration needs the
     * oldest message in the chat, not the latest.
     *
     * @param messages the messages to inspect
     * @return the oldest timestamp, or {@code null} when no message carries one
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "J",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Instant getOldestMessageTimestamp(Collection<ChatMessageInfo> messages) {
        Instant oldest = null;
        for (var msg : messages) {
            var ts = msg.timestamp().orElse(null);
            if (ts != null && (oldest == null || ts.isBefore(oldest))) {
                oldest = ts;
            }
        }
        return oldest;
    }

    /**
     * Returns whether this device's pairing timestamp is known and is at or
     * before the supplied message timestamp.
     *
     * <p>This method implements the "broadcast existed before pairing" half of
     * the broadcast exemption in {@link #canDeleteChat(Chat)}; a pairing
     * timestamp that precedes the oldest message means the device could not have
     * generated the content and the chat is safe to drop.
     *
     * @param messageTimestamp the timestamp to compare against the pairing time
     * @return {@code true} when {@link LinkedWhatsAppStore#pairingTimestamp()} is
     *         present and not after {@code messageTimestamp}
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "H",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isPairingTimestampAtOrBefore(Instant messageTimestamp) {
        var pairingTs = store.accountStore().pairingTimestamp().orElse(null);
        if (pairingTs == null) {
            return false;
        }

        return !messageTimestamp.isBefore(pairingTs);
    }

    /**
     * Returns whether every message in the collection is a migration-safe
     * system stub.
     *
     * <p>This is the simplest of the three deletability rules in
     * {@link #canDeleteChat(Chat)}; it is satisfied by chats that contain only
     * the initial E2E notification and disappearing-mode system messages.
     *
     * @param messages the messages to inspect
     * @return {@code true} when every message is a
     *         {@link #MIGRATION_SAFE_STUB_TYPES} stub
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "X",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean allMessagesAreSafeStubs(Collection<ChatMessageInfo> messages) {
        return messages.stream().allMatch(this::isMigrationSafeStub);
    }

    /**
     * Returns whether every message in the collection is either a
     * migration-safe stub or a call-log entry, with at least one call-log entry
     * present.
     *
     * <p>This companion to {@link #allMessagesAreSafeStubs(Collection)} lets
     * {@link #canDeleteChat(Chat)} drop chats whose only non-stub content is
     * missed-call or silenced-call entries.
     *
     * @param messages the messages to inspect
     * @return {@code true} when the stubs-or-call-log rule applies
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "ee",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean allMessagesAreSafeStubsOrCallLog(Collection<ChatMessageInfo> messages) {
        var hasCallLog = false;
        for (var msg : messages) {
            if (isMigrationSafeStub(msg)) {
                continue;
            }
            if (isCallLogMessage(msg)) {
                hasCallLog = true;
                continue;
            }
            return false;
        }
        return hasCallLog;
    }

    /**
     * Returns whether the given message is one of the system stubs the
     * migration treats as safe to ignore.
     *
     * <p>This method is used by every deletability rule in
     * {@link #canDeleteChat(Chat)} to skip over the initial E2E notification,
     * the E2E rotation notification, and the disappearing-mode system message;
     * these carry no user-authored content and therefore do not block deletion.
     *
     * @implNote
     * This implementation guards on the message container being empty before
     * consulting {@link #MIGRATION_SAFE_STUB_TYPES}; a message that has a stub
     * type but also carries body content is not a stub but a real message that
     * happens to mention a stub event, and must block deletion.
     *
     * @param msg the message to inspect
     * @return {@code true} when the message is a migration-safe stub
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "X",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isMigrationSafeStub(ChatMessageInfo msg) {
        if (!msg.message().isEmpty()) {
            return false;
        }

        var stubType = msg.messageStubType().orElse(null);
        return stubType != null && MIGRATION_SAFE_STUB_TYPES.contains(stubType);
    }

    /**
     * Returns whether the given message is a call-log entry (missed, silenced,
     * group or 1:1).
     *
     * <p>This method is consulted by
     * {@link #allMessagesAreSafeStubsOrCallLog(Collection)} inside
     * {@link #canDeleteChat(Chat)}; chats consisting solely of stubs plus
     * call-history entries are still safe to delete during the migration.
     *
     * @param msg the message to inspect
     * @return {@code true} when the message's stub type appears in
     *         {@link #CALL_LOG_STUB_TYPES}
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "ee",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isCallLogMessage(ChatMessageInfo msg) {
        var stubType = msg.messageStubType().orElse(null);
        return stubType != null && CALL_LOG_STUB_TYPES.contains(stubType);
    }


    /**
     * Applies the pre-computed resolutions to the store, swallowing
     * per-resolution errors so a single failure does not abort the sweep.
     *
     * <p>This method is called by {@link #executeMigration()} after every chat
     * has been classified; processing order matches the input order so that
     * deletions in one chat cannot perturb classification of another.
     *
     * @implNote
     * This implementation catches {@link Throwable} per resolution and logs the
     * failure; the surrounding sweep keeps going so a single malformed chat
     * cannot poison the entire migration.
     *
     * @param resolutions the resolutions to apply, in input order
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "migrate1x1Chats",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void executeResolutions(List<LidMigrationResolution> resolutions) {
        for (var resolution : resolutions) {
            try {
                switch (resolution) {
                    case LidMigrationResolution.Migrate migrate -> executeMigrate(migrate);
                    case LidMigrationResolution.Delete delete -> executeDelete(delete);
                    case LidMigrationResolution.Keep _ -> {}
                }
            } catch (Throwable throwable) {
                if (Log.ERROR) LOGGER.log(Level.ERROR,
                        "error executing resolution for " + new LogRedactable.User(String.valueOf(resolution.originalJid())), throwable);
            }
        }
    }

    /**
     * Rewrites a single chat to use LID addressing and mirrors the mapping onto
     * the store and contact records.
     *
     * <p>This method is dispatched by {@link #executeResolutions(List)} for
     * every {@link LidMigrationResolution.Migrate} produced by
     * {@link #resolveThread(Chat, Set)}. A chat that has been removed between
     * classification and execution is logged and silently skipped.
     *
     * @implNote
     * This implementation keeps both the LID and the original PN on the
     * {@link Chat} (so future stanzas can reconstruct either addressing mode)
     * and registers the LID/PN pair in the bidirectional store mapping so
     * subsequent {@link #toLid(Jid)} and {@link #toPn(Jid)} lookups find it
     * without touching the primary cache.
     *
     * @param migrate the migration action to apply
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "migrate1x1Chats",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void executeMigrate(LidMigrationResolution.Migrate migrate) {
        var originalJid = migrate.originalJid();
        var targetLid = migrate.targetLid();

        var chat = store.chatStore().findChatByJid(originalJid).orElse(null);
        if (chat == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "chat not found for migration: {0}", originalJid);
            return;
        }

        chat.setLid(targetLid);
        chat.setPhoneNumberJid(originalJid);

        store.contactStore().registerLidMapping(originalJid, targetLid);
        store.contactStore().findContactByJid(originalJid).ifPresent(contact -> contact.setLid(targetLid));

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "migrated chat {0} -> {1}", originalJid, targetLid);
    }

    /**
     * Removes a chat that {@link #resolveThread(Chat, Set)} classified as safe
     * to delete.
     *
     * <p>This method is dispatched by {@link #executeResolutions(List)} for
     * every {@link LidMigrationResolution.Delete}. A chat already absent from
     * the store is a tolerated no-op.
     *
     * @param delete the delete action to apply
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "migrate1x1Chats",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void executeDelete(LidMigrationResolution.Delete delete) {
        var originalJid = delete.originalJid();

        var removed = store.chatStore().removeChat(originalJid);
        if (removed.isPresent()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted chat {0}: {1}", originalJid, delete.reason());
        }
    }

    /**
     * Applies a LID-change notification for an existing contact by updating the
     * primary caches, the bidirectional store mapping, the contact, and any
     * chat keyed by the phone number.
     *
     * <p>This method is called when the server, the primary device, or a
     * contact roster sync reports that a contact's LID has rotated. The new LID
     * is treated as both the assigned and the latest LID for the contact, so
     * subsequent {@link #lookupLid(Jid)} calls and any pending
     * {@link #resolveThread(Chat, Set)} classifications see the rotated value
     * immediately. A {@code null} {@code phoneJid} or {@code newLid} is a
     * tolerated no-op; {@code oldLid} is accepted only for logging.
     *
     * @param phoneJid the phone-number JID whose LID is rotating
     * @param newLid   the new LID
     * @param oldLid   the previous LID, or {@code null} when unknown
     */
    @Override
    public void changeLid(Jid phoneJid, Jid newLid, Jid oldLid) {
        if (phoneJid == null || newLid == null) {
            return;
        }

        if (phoneJid.user() != null) {
            primaryPnToAssignedLidCache.put(phoneJid.user(), newLid);
            primaryPnToLatestLidCache.put(phoneJid.user(), newLid);
        }

        store.contactStore().registerLidMapping(phoneJid, newLid);
        store.contactStore().findContactByJid(phoneJid).ifPresent(contact -> contact.setLid(newLid));
        store.chatStore().findChatByJid(phoneJid).ifPresent(chat -> {
            chat.setLid(newLid);
            chat.setPhoneNumberJid(phoneJid);
        });

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "lid changed for {0}: {1} -> {2}", phoneJid, oldLid, newLid);
    }

    /**
     * Records the LID known at chat-creation time so it can serve as a
     * last-resort fallback during migration.
     *
     * <p>This method is called by the chat-creation path when the local client
     * already knows a LID for a phone-number chat before the migration has
     * begun. The cached value is consulted only by
     * {@link #resolveThread(Chat, Set)} when neither the primary cache nor the
     * bidirectional store mapping has a LID for the contact. A {@code null}
     * argument is a tolerated no-op.
     *
     * @param phoneJid the phone-number JID of the chat
     * @param lid      the LID known at chat-creation time
     */
    @Override
    public void registerOriginalLid(Jid phoneJid, Jid lid) {
        if (phoneJid == null || lid == null || phoneJid.user() == null) {
            return;
        }

        originalLidCache.put(phoneJid.user(), lid);
    }

    /**
     * Sweeps the primary caches into the store's bidirectional LID/PN mapping
     * table at the end of the migration.
     *
     * <p>This method is called once by {@link #executeMigration()} after the
     * state machine has reached {@link LidMigrationState#COMPLETE}; the
     * resulting store entries are the source of truth for subsequent
     * {@link #lookupLid(Jid)} and {@link #toLid(Jid)} lookups once the primary
     * caches drift out of relevance.
     *
     * @implNote
     * This implementation follows WhatsApp Web's two-phase learning. An entry
     * whose assigned LID already matches the store's current LID is skipped. An
     * entry whose latest LID matches the pre-existing local LID is treated as
     * an old mapping: only the assigned LID is registered, so the rotation is
     * undone. An entry whose latest LID differs is treated as a latest mapping:
     * both the assigned and the latest LID are registered so subsequent lookups
     * return the rotated value. The two buckets are written in the
     * old-then-latest order because the store reads the most recently
     * registered entry and the latest LID must win when both are written.
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationPrimaryCache", exports = "lidPnMigrationPrimaryCache",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void learnMappingsInBulk() {
        var oldMappings = new ArrayList<Map.Entry<Jid, Jid>>();
        var latestMappings = new ArrayList<Map.Entry<Jid, Jid>>();

        for (var entry : primaryPnToAssignedLidCache.entrySet()) {
            var phoneJid = Jid.of(entry.getKey());
            var assignedLid = entry.getValue();

            var currentLid = store.contactStore().findLidByPhone(phoneJid).orElse(null);
            if (assignedLid.toUserJid().equals(currentLid != null ? currentLid.toUserJid() : null)) {
                continue;
            }

            var latestLid = primaryPnToLatestLidCache.get(entry.getKey());
            if (latestLid != null && latestLid.toUserJid().equals(currentLid != null ? currentLid.toUserJid() : null)) {
                oldMappings.add(Map.entry(phoneJid, assignedLid));
            } else {
                latestMappings.add(Map.entry(phoneJid, assignedLid));
                if (latestLid != null) {
                    latestMappings.add(Map.entry(phoneJid, latestLid));
                }
            }
        }

        for (var mapping : oldMappings) {
            store.contactStore().registerLidMapping(mapping.getKey(), mapping.getValue());
        }
        for (var mapping : latestMappings) {
            store.contactStore().registerLidMapping(mapping.getKey(), mapping.getValue());
        }

        if (Log.INFO) LOGGER.log(Level.INFO,
                "bulk-registered lid mappings: {0} old, {1} latest",
                oldMappings.size(), latestMappings.size());
    }

    /**
     * Walks the chat store once and commits the daily LID-migration census that
     * WhatsApp Web's background stats task reports for the account.
     *
     * <p>The census classifies every stored chat by its LID-mapping state and
     * reports the aggregate counts: 1:1 user chats that still lack a LID mapping
     * ({@code numberOfPnChatsWithoutMapping}), user chats without an account LID
     * ({@code numberOfUserChatsWithoutAccountLid}, only when
     * {@link #isLidMigrated()} is {@code true}), regular phone-number chats
     * ({@code numberOfRegularPnChats}), groups partitioned by whether their
     * {@link GroupMetadata#isLidAddressingMode()}
     * is set ({@code numberOfLidGroups} vs {@code numberOfPnGroups}), and the
     * click-to-WhatsApp phone-number-hiding threads partitioned by whether
     * {@link #toPn(Jid)} can resolve their phone number
     * ({@code numberOfPnhCtwaThreadsKnownMapping} vs
     * {@code numberOfPnhCtwaThreadsMissingMapping}). The
     * {@code completedMigrations} field carries the comma-joined list of
     * completed sub-migrations, mirroring WhatsApp Web's marker order.
     *
     * @implNote
     * WhatsApp Web emits this event once per day from a background stats task
     * and derives {@code lidMigrationSource} from where the mappings were
     * learned. Cobalt has no daily scheduler in this service, so it emits the
     * census at the point the 1:1 thread migration completes, tagged with the
     * source of the mappings that drove it. The {@code completedMigrations}
     * string reproduces WhatsApp Web's always-complete baseline markers
     * ({@code con}, {@code id}, {@code ss}, {@code sk}, {@code st_lid}) plus the
     * {@code ch_jid} marker once {@link #isLidMigrated()} is {@code true}; the
     * sub-migration markers WhatsApp Web additionally tracks for favourites,
     * cart, labels, phone-number-hiding promotion, blocklist, inactive groups,
     * and forced-history chats are omitted because Cobalt does not model those
     * sub-migrations as independent flags. The five census fields WhatsApp Web's
     * web census leaves unset ({@code numberOfChatsWithClientAssignedLid},
     * {@code numberOfDeprecatedChats}, {@code numberOfLidBroadcastLists},
     * {@code numberOfPnBroadcastLists}, {@code numberOfSplitThreads}) are left
     * absent to mirror the reference payload exactly.
     *
     * @param lidMigrationSource whether the mappings that drove the migration
     *                           were learned from a peer message or from history
     *                           sync
     */
    @WhatsAppWebExport(moduleName = "WAWebTasksDailyStatsTask", exports = "getLidMigrationStatus",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitLidMigrationDailyCensus(LidMigrationSourceType lidMigrationSource) {
        var pnChatsWithoutMapping = 0L;
        var userChatsWithoutAccountLid = 0L;
        var regularPnChats = 0L;
        var pnhCtwaThreadsKnownMapping = 0L;
        var pnhCtwaThreadsMissingMapping = 0L;
        var pnGroups = 0L;
        var lidGroups = 0L;

        for (var chat : store.chatStore().chats()) {
            var jid = chat.jid();

            if (jid.hasGroupOrCommunityServer()) {
                var lidAddressed = store.chatStore().findChatMetadata(jid)
                        .map(metadata -> metadata.isLidAddressingMode())
                        .orElse(false);
                if (lidAddressed) {
                    lidGroups++;
                } else {
                    pnGroups++;
                }
                continue;
            }

            if (jid.hasLidServer() && LID_ORIGIN_TYPE_PNH_CTWA.equals(chat.lidOriginType().orElse(null))) {
                if (toPn(jid) == null) {
                    pnhCtwaThreadsMissingMapping++;
                } else {
                    pnhCtwaThreadsKnownMapping++;
                }
            }

            if (!LidMigrationService.isRegularUser(jid)) {
                continue;
            }

            if (toLid(jid) == null) {
                pnChatsWithoutMapping++;
            }

            if (chat.lid().isEmpty()) {
                userChatsWithoutAccountLid++;
            }

            if (jid.hasUserServer()) {
                regularPnChats++;
            }
        }

        var completedMigrations = new ArrayList<String>();
        completedMigrations.add("con");
        completedMigrations.add("id");
        completedMigrations.add("ss");
        completedMigrations.add("sk");
        if (isLidMigrated()) {
            completedMigrations.add("ch_jid");
        }
        completedMigrations.add("st_lid");

        var builder = new LidMigrationDailyEventBuilder()
                .completedMigrations(String.join(",", completedMigrations))
                .lidMigrationSource(lidMigrationSource)
                .numberOfPnChatsWithoutMapping(pnChatsWithoutMapping)
                .numberOfRegularPnChats(regularPnChats)
                .numberOfPnhCtwaThreadsKnownMapping(pnhCtwaThreadsKnownMapping)
                .numberOfPnhCtwaThreadsMissingMapping(pnhCtwaThreadsMissingMapping)
                .numberOfPnGroups(pnGroups)
                .numberOfLidGroups(lidGroups);
        if (isLidMigrated()) {
            builder.numberOfUserChatsWithoutAccountLid(userChatsWithoutAccountLid);
        }
        wamService.commit(builder.build());

        if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                "committed lid migration daily census: {0} pn groups, {1} lid groups, {2} regular pn chats",
                pnGroups, lidGroups, regularPnChats);
    }

    /**
     * Returns whether the migration sweep should run immediately after a
     * primary-device mapping has been ingested.
     *
     * @implNote
     * This implementation always returns {@code true}; there is no tab-priority
     * queue to honour and no page reload to coordinate. WhatsApp Web defers the
     * sweep via a window-refreshing job, whereas Cobalt runs it on the same
     * virtual thread that ingested the protocol message.
     *
     * @return {@code true}, always
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationManager", exports = "ThreadMigrationManager",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebLid1X1ThreadAccountMigrations", exports = "shouldMigrateNow",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean shouldAutoStartMigration() {
        return true;
    }

    /**
     * Reports a migration failure by setting {@link LidMigrationState#FAILED}
     * and surfacing the exception through
     * {@link LinkedWhatsAppClient#handleFailure(com.github.auties00.cobalt.exception.WhatsAppException)}.
     *
     * <p>This is the single funnel for every error path in the migration; it
     * keeps the state machine and the configurable error handler in sync
     * regardless of where the failure originated (mapping parse, mapping
     * timeout, executor cascade, resolution loop).
     *
     * @param error the migration exception to surface
     */
    private void handleError(WhatsAppLidMigrationException error) {
        state.set(LidMigrationState.FAILED);
        if (Log.ERROR) LOGGER.log(Level.ERROR, "lid migration failed", error);
        whatsapp.handleFailure(error);
    }

    /**
     * Rewinds the state machine to {@link LidMigrationState#NOT_STARTED} for a
     * new session, preserving primary caches and terminal states.
     *
     * <p>This method is called from the client's reconnect handler so the next
     * session can re-run {@link #initialize()} and friends without losing the
     * {@link #primaryPnToAssignedLidCache} contents that {@link #lookupLid(Jid)}
     * still depends on. Terminal states ({@link LidMigrationState#COMPLETE},
     * {@link LidMigrationState#DISABLED}, {@link LidMigrationState#FAILED}) are
     * deliberately preserved so a session bounce cannot reopen a migration that
     * has already concluded.
     *
     * @implNote
     * This implementation cancels {@link #mappingTimeoutFuture} so the new
     * session's {@link #enableMigration()} can arm a fresh timeout, then sets
     * the state to {@link LidMigrationState#NOT_STARTED} only when the current
     * state is non-terminal.
     */
    @Override
    public void reset() {
        var timeout = mappingTimeoutFuture;
        if (timeout != null) {
            timeout.cancel();
            mappingTimeoutFuture = null;
        }

        var currentState = state.get();
        if (!currentState.isTerminal()) {
            state.set(LidMigrationState.NOT_STARTED);
        }
    }

    /**
     * Returns the LID associated with the given phone-number JID, preferring
     * the in-memory primary cache before consulting the store.
     *
     * <p>This is the standard PN to LID resolver for callers that want a
     * present-or-empty answer without forcing a conversion; it stays useful for
     * general-store mappings learned through history sync. A {@code null} input
     * or a JID without a user part yields an empty {@link Optional}.
     *
     * @param phoneJid the phone-number JID to resolve
     * @return the LID for the JID, or {@link Optional#empty()} when none is
     *         known
     */
    @WhatsAppWebExport(moduleName = "WAWebLid1x1MigrationPrimaryCache", exports = "lidPnMigrationPrimaryCache",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public Optional<Jid> lookupLid(Jid phoneJid) {
        if (phoneJid == null || phoneJid.user() == null) {
            return Optional.empty();
        }

        var cached = primaryPnToAssignedLidCache.get(phoneJid.user());
        if (cached != null) {
            return Optional.of(cached);
        }

        return store.contactStore().findLidByPhone(phoneJid);
    }

    /**
     * Returns whether outgoing messages to the given recipient should be
     * addressed using LID rather than phone number.
     *
     * <p>This method is called by the outgoing-message path to decide whether
     * to swap the recipient JID before dispatch. The decision intentionally
     * activates from {@link LidMigrationState#IN_PROGRESS} onwards, not only at
     * {@link LidMigrationState#COMPLETE}, so messages sent while the migration
     * is sweeping chats still land on the new addressing mode.
     *
     * @implNote
     * This implementation accepts LID-server JIDs unconditionally, rejects
     * group, community, newsletter, and broadcast servers outright, and
     * otherwise gates on the state machine plus a positive
     * {@link #lookupLid(Jid)} result so unmapped 1:1 recipients keep their PN
     * addressing.
     *
     * @param recipientJid the recipient JID, or {@code null}
     * @return {@code true} when LID addressing should be used
     */
    @Override
    public boolean shouldUseLidAddressing(Jid recipientJid) {
        if (recipientJid == null) {
            return false;
        }

        if (recipientJid.hasLidServer()) {
            return true;
        }

        if (recipientJid.hasGroupOrCommunityServer() ||
            recipientJid.hasNewsletterServer() ||
            recipientJid.hasBroadcastServer()) {
            return false;
        }

        var currentState = state.get();
        if (currentState != LidMigrationState.COMPLETE && currentState != LidMigrationState.IN_PROGRESS) {
            return false;
        }

        return lookupLid(recipientJid).isPresent();
    }

    /**
     * Returns whether the given JID is eligible to carry an {@code account_lid}
     * attribute on outgoing stanzas.
     *
     * <p>Only regular users (no PSA announcements account, no bot, no group
     * server) may carry an {@code account_lid} attribute, and only after the
     * 1:1 migration has fully completed. Stanza builders consult this when
     * deciding whether to attach the attribute.
     *
     * @param jid the JID to evaluate, or {@code null}
     * @return {@code true} when the JID should carry an account LID
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "shouldHaveAccountLid",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public boolean shouldHaveAccountLid(Jid jid) {
        if (jid == null) {
            return false;
        }

        return isLidMigrated() && LidMigrationService.isRegularUser(jid);
    }

    /**
     * Returns the phone-number JID corresponding to the given JID, or the input
     * itself when it is already a PN.
     *
     * <p>This is the standard LID to PN converter consumed by the stanza
     * builders and by {@link #getAlternateMsgKey(MessageKey)}. It returns
     * {@code null} when the JID is a LID with no known mapping, signalling to
     * the caller that the conversion is impossible in the current state.
     *
     * @param jid the JID to convert, or {@code null}
     * @return the PN form, or {@code null} when the JID is a LID with no mapping
     *         (or the input is {@code null})
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "toPn",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public Jid toPn(Jid jid) {
        if (jid == null) {
            return null;
        }

        if (!jid.hasLidServer()) {
            return jid;
        }

        return store.contactStore().findPhoneByLid(jid).orElse(null);
    }

    /**
     * Returns the LID JID corresponding to the given JID, or the input itself
     * when it is already a LID.
     *
     * <p>This is the standard PN to LID converter. It strips device and agent
     * data from the input before the lookup so the same call works for a
     * participant-keyed JID and a user-bare JID alike.
     *
     * @param jid the JID to convert, or {@code null}
     * @return the LID form, or {@code null} when the JID is a PN with no mapping
     *         (or the input is {@code null})
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "toLid",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public Jid toLid(Jid jid) {
        if (jid == null) {
            return null;
        }

        if (jid.hasLidServer()) {
            return jid;
        }

        var userJid = jid.toUserJid();
        return store.contactStore().findLidByPhone(userJid).orElse(null);
    }

    /**
     * Returns the user-level LID corresponding to the given JID, with device
     * and agent data stripped.
     *
     * <p>This is the standard converter for callers that need a participant JID
     * at user granularity (no device suffix). It is combined with
     * {@link #getMeUserLidOrJidForChat(Chat, TranslateMsgKeyType)} to compose
     * the participant field on outgoing message keys.
     *
     * @param jid the JID to convert, or {@code null}
     * @return the user LID, or {@code null} when no mapping is known
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "toUserLid",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public Jid toUserLid(Jid jid) {
        if (jid == null) {
            return null;
        }

        var userJid = jid.toUserJid();

        if (userJid.hasLidServer()) {
            return userJid;
        }

        return store.contactStore().findLidByPhone(userJid).orElse(null);
    }

    /**
     * Returns the user-level LID for the given JID or throws when none can be
     * resolved.
     *
     * <p>This is the non-nullable companion to {@link #toUserLid(Jid)} for call
     * sites where a missing mapping is a programming error (the JID is expected
     * to be LID-resolvable at this point in the flow).
     *
     * @param jid the JID to convert
     * @return the user LID, never {@code null}
     * @throws IllegalStateException when no LID mapping exists for the JID
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "toUserLidOrThrow",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public Jid toUserLidOrThrow(Jid jid) {
        var result = toUserLid(jid);
        if (result == null) {
            throw new IllegalStateException("No LID for user");
        }
        return result;
    }

    /**
     * Returns the phone-number JID for the given JID or throws when none can be
     * resolved.
     *
     * <p>This is the non-nullable companion to {@link #toPn(Jid)} for call sites
     * where the LID must resolve to a PN (for example, when composing a PN-keyed
     * message-key copy for a chat that has not yet migrated).
     *
     * @param jid the JID to convert
     * @return the phone-number JID, never {@code null}
     * @throws IllegalStateException when no PN mapping exists for the JID
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "toPnOrThrow",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public Jid toPnOrThrow(Jid jid) {
        var result = toPn(jid);
        if (result == null) {
            throw new IllegalStateException("No PN for user");
        }
        return result;
    }

    /**
     * Returns a {@link Function} that converts JIDs to the requested addressing
     * mode.
     *
     * <p>This lets callers pick the converter once and apply it across a
     * collection of JIDs without branching on the mode at every element.
     *
     * @param isLid {@code true} to obtain {@link #toLid(Jid)}, {@code false} to
     *              obtain {@link #toPn(Jid)}
     * @return a {@link Function} reference to the chosen converter
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "toAddressingModeFactory",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public Function<Jid, Jid> toAddressingModeFactory(boolean isLid) {
        return isLid ? this::toLid : this::toPn;
    }

    /**
     * Normalises two JIDs to the same addressing mode by converting one side
     * when they are user wids on different server families.
     *
     * <p>This is used by callers that compare two user JIDs and need both on the
     * same server family before the comparison is meaningful (for example,
     * equality checks between a stored participant and an inbound stanza's
     * sender). The input pair is returned unchanged when neither side has a
     * known alternate.
     *
     * @implNote
     * This implementation tries to convert the first side first (favouring the
     * second side's addressing mode); if no alternate is known for the first
     * side, it falls back to converting the second side. Non-user JIDs and JIDs
     * that already share an addressing mode are passed through as-is.
     *
     * @param first  the first JID, or {@code null}
     * @param second the second JID, or {@code null}
     * @return a two-element array with the (possibly converted) pair, preserving
     *         the input order
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "toCommonAddressingMode",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public Jid[] toCommonAddressingMode(Jid first, Jid second) {
        if (first != null && second != null
                && isUserWid(first) && isUserWid(second)
                && first.hasLidServer() != second.hasLidServer()) {

            var alternateFirst = getAlternateUserWid(first.toUserJid());
            if (alternateFirst != null) {
                return new Jid[]{alternateFirst, second};
            }

            var alternateSecond = getAlternateUserWid(second.toUserJid());
            if (alternateSecond != null) {
                return new Jid[]{first, alternateSecond};
            }
        }
        return new Jid[]{first, second};
    }

    /**
     * Returns the addressing-mode mirror of the given {@link MessageKey},
     * swapping the participant or remote JID into the opposite mode.
     *
     * <p>This is used to reconcile two stored copies of the same message (one
     * PN-keyed, one LID-keyed) so receipts, edits, and reactions land against
     * both. For group, status, and broadcast remotes the participant JID is
     * swapped; for 1:1 user remotes the remote JID is swapped. It returns
     * {@code null} when no alternate is resolvable.
     *
     * @param msgKey the message key, or {@code null}
     * @return the alternate {@link MessageKey}, or {@code null} when no
     *         alternate can be built
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "getAlternateMsgKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public MessageKey getAlternateMsgKey(MessageKey msgKey) {
        if (msgKey == null) {
            return null;
        }

        var remote = msgKey.parentJid().orElse(null);
        if (remote == null) {
            return null;
        }

        if (remote.hasGroupOrCommunityServer() || remote.hasBroadcastServer()) {
            return getAlternateMsgKeyForGroup(msgKey);
        }

        if (isUserWid(remote)) {
            return getAlternateMsgKeyForUser(msgKey);
        }

        return null;
    }

    /**
     * Returns the alternate message key for a group, status, or broadcast
     * remote, with the participant JID swapped into the opposite addressing
     * mode.
     *
     * <p>This is the branch of {@link #getAlternateMsgKey(MessageKey)} that
     * targets non-user remotes. The remote itself is preserved because group,
     * status, and broadcast JIDs do not have an addressing-mode alternate.
     *
     * @param msgKey the key whose {@link MessageKey#parentJid()} is a group,
     *               status, or broadcast JID
     * @return the alternate key, or {@code null} when the participant has no
     *         addressing-mode alternate
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "S",
            adaptation = WhatsAppAdaptation.DIRECT)
    private MessageKey getAlternateMsgKeyForGroup(MessageKey msgKey) {
        var participant = getRawParticipant(msgKey);
        if (participant == null) {
            return null;
        }

        var alternateParticipant = getAlternateUserWid(participant.toUserJid());
        if (alternateParticipant == null) {
            return null;
        }

        var remote = msgKey.parentJid().orElse(null);
        var id = msgKey.id().orElse(null);
        return new MessageKeyBuilder()
                .fromMe(msgKey.fromMe())
                .parentJid(remote)
                .id(id)
                .senderJid(alternateParticipant)
                .build();
    }

    /**
     * Returns the alternate message key for a 1:1 user remote, with the remote
     * JID swapped into the opposite addressing mode.
     *
     * <p>This is the branch of {@link #getAlternateMsgKey(MessageKey)} that
     * targets user remotes. The participant is preserved as-is because in a 1:1
     * conversation the participant carries no addressing-mode meaning of its
     * own.
     *
     * @param msgKey the key whose {@link MessageKey#parentJid()} is a user JID
     * @return the alternate key, or {@code null} when the remote has no
     *         addressing-mode alternate
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "R",
            adaptation = WhatsAppAdaptation.DIRECT)
    private MessageKey getAlternateMsgKeyForUser(MessageKey msgKey) {
        var remote = msgKey.parentJid().orElse(null);
        if (remote == null) {
            return null;
        }

        var alternateRemote = getAlternateUserWid(remote.toUserJid());
        if (alternateRemote == null) {
            return null;
        }

        var id = msgKey.id().orElse(null);
        var participant = getRawParticipant(msgKey);
        return new MessageKeyBuilder()
                .fromMe(msgKey.fromMe())
                .parentJid(alternateRemote)
                .id(id)
                .senderJid(participant)
                .build();
    }

    /**
     * Returns the raw participant JID of a message key, treating a participant
     * equal to the remote as absent.
     *
     * <p>A key whose participant equals its remote is interpreted as having no
     * participant at all (the field was defaulted by the message-key builder
     * rather than explicitly set).
     *
     * @param msgKey the message key
     * @return the participant JID, or {@code null} when it was not set or equals
     *         the remote
     */
    private static Jid getRawParticipant(MessageKey msgKey) {
        var sender = msgKey.senderJid().orElse(null);
        var parent = msgKey.parentJid().orElse(null);
        if (sender != null && sender.equals(parent)) {
            return null;
        }
        return sender;
    }

    /**
     * Returns the current user's identity, in the addressing mode appropriate
     * for the given chat and translate type, that should appear as the
     * participant of an outgoing message key.
     *
     * <p>The five inputs that drive the decision are: whether the chat is on the
     * LID server, whether it is a group, whether the group is a Community
     * Announcement Group (default subgroup), whether the group's metadata
     * reports LID addressing mode, and the {@link TranslateMsgKeyType}.
     * {@link TranslateMsgKeyType#ADDON} selects LID whenever the chat is LID, a
     * Community Announcement Group, or on LID addressing;
     * {@link TranslateMsgKeyType#MESSAGE} and
     * {@link TranslateMsgKeyType#EDIT_MESSAGE} carve out a
     * Community-Announcement-Group branch that selects PN unless the group is on
     * LID addressing.
     *
     * @param chat          the chat composing the outgoing message
     * @param translateType the message-key category that picks the addressing
     *                      mode
     * @return the current user's JID in the chosen addressing mode
     * @throws IllegalStateException when the store has no JID configured for the
     *         chosen addressing mode (no self-LID on the LID branch or no
     *         self-PN on the PN branch)
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "getMeUserLidOrJidForChat",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public Jid getMeUserLidOrJidForChat(Chat chat, TranslateMsgKeyType translateType) {
        var chatJid = chat.jid();
        var isLid = chatJid.hasLidServer();

        var chatMetadata = store.chatStore().findChatMetadata(chatJid).orElse(null);
        var isGroup = chatJid.hasGroupOrCommunityServer();
        var isCAG = isGroup && chatMetadata instanceof GroupMetadata gm
                && gm.isDefaultSubgroup();

        var isLidAddressingMode = isGroup
                && chatMetadata != null
                && chatMetadata.isLidAddressingMode();

        return switch (translateType) {
            case ADDON -> {
                if (isLid || isCAG || isLidAddressingMode) {
                    yield getMeLidUserOrThrow();
                } else {
                    yield getMePnUserOrThrow();
                }
            }
            case MESSAGE, EDIT_MESSAGE -> {
                if (isCAG) {
                    if (isLidAddressingMode) {
                        yield getMeLidUserOrThrow();
                    } else {
                        yield getMePnUserOrThrow();
                    }
                } else {
                    if (isLid || isLidAddressingMode) {
                        yield getMeLidUserOrThrow();
                    } else {
                        yield getMePnUserOrThrow();
                    }
                }
            }
        };
    }

    /**
     * Returns both addressing-mode JIDs for the given JID, with the input itself
     * first followed by its alternate when known.
     *
     * <p>This lets callers iterate over both addressing modes in a single loop
     * when they need to apply the same change (notify, update, remove) to both
     * copies of a contact-tied entity. A single-element list is returned when no
     * alternate is known and an empty list when the input is {@code null}.
     *
     * @param jid the JID whose addressing-mode pair is requested, or
     *            {@code null}
     * @return a list of one or two JIDs, with the input first
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "getPnAndLidToUpdate",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public List<Jid> getPnAndLidToUpdate(Jid jid) {
        if (jid == null) {
            return List.of();
        }

        if (jid.hasLidServer()) {
            var pn = toPn(jid);
            if (pn != null) {
                return List.of(jid, pn);
            }
        } else {
            var lid = toLid(jid);
            if (lid != null) {
                return List.of(jid, lid);
            }
        }

        return List.of(jid);
    }

    /**
     * Returns whether the given chat uses LID addressing mode.
     *
     * <p>A 1:1 LID-server chat always uses LID addressing; a group uses LID
     * addressing when its {@link GroupMetadata#isLidAddressingMode()} is
     * {@code true}. Other server families return {@code false}.
     *
     * @param chat the chat to inspect, or {@code null}
     * @return {@code true} when the chat uses LID addressing
     */
    @WhatsAppWebExport(moduleName = "WAWebLidMigrationUtils", exports = "chatIsLid",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public boolean chatIsLid(Chat chat) {
        if (chat == null) {
            return false;
        }

        var chatJid = chat.jid();

        if (chatJid.hasLidServer()) {
            return true;
        }

        if (chatJid.hasGroupOrCommunityServer()) {
            var chatMetadata = store.chatStore().findChatMetadata(chatJid).orElse(null);
            return chatMetadata != null && chatMetadata.isLidAddressingMode();
        }

        return false;
    }

    /**
     * Returns the alternate-addressing-mode user JID for the given user JID,
     * with a fast path for the current user.
     *
     * <p>This underpins {@link #toCommonAddressingMode(Jid, Jid)} and
     * {@link #getAlternateMsgKey(MessageKey)}. The fast path recognises the
     * current user from {@link LinkedWhatsAppAccountStore#jid()} and
     * {@link LinkedWhatsAppAccountStore#lid()} so a "me" flip never touches the mapping
     * table; any other JID falls back to
     * {@link LinkedWhatsAppContactStore#findPhoneByLid(Jid)} or
     * {@link LinkedWhatsAppContactStore#findLidByPhone(Jid)}.
     *
     * @implNote
     * This implementation collapses WhatsApp Web's separate alternate-user,
     * phone-number, and current-LID exports into one private helper because
     * Cobalt does not need the separate device-WID rewrite WhatsApp Web routes
     * alongside them.
     *
     * @param userJid the user JID, already stripped of device and agent data
     * @return the alternate JID, or {@code null} when no mapping is known
     */
    @WhatsAppWebExport(moduleName = "WAWebApiContact", exports = "getAlternateUserWid",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebApiContact", exports = "getPhoneNumber",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebApiContact", exports = "getCurrentLid",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Jid getAlternateUserWid(Jid userJid) {
        if (userJid == null) {
            return null;
        }

        if (userJid.hasLidServer()) {
            var meLid = store.accountStore().lid().map(Jid::toUserJid).orElse(null);
            var mePn = store.accountStore().jid().map(Jid::toUserJid).orElse(null);
            if (mePn != null && meLid != null && userJid.equals(meLid)) {
                return mePn;
            }
            return store.contactStore().findPhoneByLid(userJid).orElse(null);
        } else {
            var mePn = store.accountStore().jid().map(Jid::toUserJid).orElse(null);
            var meLid = store.accountStore().lid().map(Jid::toUserJid).orElse(null);
            if (meLid != null && mePn != null && userJid.equals(mePn)) {
                return meLid;
            }
            return store.contactStore().findLidByPhone(userJid).orElse(null);
        }
    }

    /**
     * Returns the current user's LID at user-level granularity.
     *
     * <p>This helper is consumed by
     * {@link #getMeUserLidOrJidForChat(Chat, TranslateMsgKeyType)} for the
     * branches that resolve to the current user's LID.
     *
     * @return the current user's LID, never {@code null}
     * @throws IllegalStateException when no LID is configured for the current
     *         user
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsMeUser", exports = "getMeLidUserOrThrow",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Jid getMeLidUserOrThrow() {
        return store.accountStore().lid()
                .map(Jid::toUserJid)
                .orElseThrow(() -> new IllegalStateException("No LID for current user"));
    }

    /**
     * Returns the current user's phone-number JID at user-level granularity.
     *
     * <p>This helper is consumed by
     * {@link #getMeUserLidOrJidForChat(Chat, TranslateMsgKeyType)} for the
     * branches that resolve to the current user's PN.
     *
     * @return the current user's PN JID, never {@code null}
     * @throws IllegalStateException when no PN is configured for the current
     *         user
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsMeUser", exports = "getMePnUserOrThrow_DO_NOT_USE",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Jid getMePnUserOrThrow() {
        return store.accountStore().jid()
                .map(Jid::toUserJid)
                .orElseThrow(() -> new IllegalStateException("No PN for current user"));
    }

    /**
     * Returns whether the given JID lives on a user-like server family.
     *
     * <p>This is used by {@link #toCommonAddressingMode(Jid, Jid)} and
     * {@link #getAlternateMsgKey(MessageKey)} to gate the mixed-mode logic. It
     * recognises {@code s.whatsapp.net}, {@code lid}, {@code bot},
     * {@code hosted}, and {@code hosted.lid}.
     *
     * @param jid the JID to inspect
     * @return {@code true} when the JID is a user-like wid
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isUser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isUserWid(Jid jid) {
        return jid.hasUserServer()
                || jid.hasLidServer()
                || jid.hasBotServer()
                || jid.hasHostedServer()
                || jid.hasHostedLidServer();
    }
}
