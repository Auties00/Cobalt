package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.device.sync.MissingDeviceSyncKey;
import com.github.auties00.cobalt.wire.linked.device.sync.PendingDeviceSync;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyData;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyId;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.sync.mutation.OrphanMutationEntries;
import com.github.auties00.cobalt.wire.linked.sync.mutation.OrphanMutationEntry;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEntry;
import com.github.auties00.cobalt.wire.linked.sync.SyncCollectionMetadata;
import com.github.auties00.cobalt.wire.linked.sync.SyncCollectionMetadataBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncCollectionState;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.MutationLTHash;
import com.github.auties00.cobalt.sync.key.SyncKeyUtils;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNullElseGet;

/**
 * The protobuf-backed {@link LinkedWhatsAppSyncStore} holding this session's app-state sync and feature-flag state.
 *
 * <p>This is a nested {@code MESSAGE} sub-store of {@link ProtobufWhatsAppStore}; it owns the syncd
 * collection state machine, the app-state sync keys and missing-key tracker, the applied
 * sync-action index, the history policy and bootstrap flags, the device-sync retry queue and the
 * AB-props feature-flag bundle (persisted), plus the transient pending/orphan mutation queues.
 *
 * @implNote
 * This implementation resets every collection's runtime state-machine fields (state, retry count,
 * error timestamp, MAC-mismatch flag) to up-to-date in its constructor, since an in-flight or
 * errored sync round never survives a restart. The orphan-mutation queue, pending-mutation queue
 * and sync-action index are transient and start empty.
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class ProtobufLinkedWhatsAppSyncStore implements LinkedWhatsAppSyncStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppSyncStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppSyncStore.class);

    /**
     * Whether app-state patch MACs are verified during sync.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    private boolean checkPatchMacs;

    /**
     * Whether the initial chat history sync has completed.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    private boolean syncedChats;

    /**
     * Whether the initial contact history sync has completed.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    private boolean syncedContacts;

    /**
     * Whether the initial newsletter subscription sync has completed.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    private boolean syncedNewsletters;

    /**
     * Whether the initial status history sync has completed.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    private boolean syncedStatus;

    /**
     * Whether the business-verified-name certificate has been fetched.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    private boolean syncedBusinessCertificate;

    /**
     * The map of app-state sync keys keyed by their hex-encoded id.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final LinkedHashMap<String, AppStateSyncKey> appStateKeysMap;

    /**
     * The map of app-state sync keys known to be missing, keyed by their hex-encoded id.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<String, MissingDeviceSyncKey> missingSyncKeysMap;

    /**
     * Whether the primary device supports the syncd snapshot-recovery flow.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    private boolean primaryDeviceSupportsSyncdRecovery;

    /**
     * Whether the client opted into the external-web-beta AB-prop bucket.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    private boolean externalWebBeta;

    /**
     * The feature gates the primary device reports it understands.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    private List<String> primaryFeatures;

    /**
     * The observed clock skew between local and server time in seconds.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.INT64)
    private long clockSkewSeconds;

    /**
     * The timestamp of the most recent emergency push of group-scoped AB props.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    private Instant groupAbPropsEmergencyPushTimestamp;

    /**
     * The opaque AB-key echoed on the next AB-props refresh.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    private String abPropsAbKey;

    /**
     * The content hash of the cached AB-props bundle.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.STRING)
    private String abPropsHash;

    /**
     * The server-suggested AB-props refresh interval in seconds.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.INT64)
    private long abPropsRefresh;

    /**
     * The timestamp of the most recent successful AB-props fetch.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    private Instant abPropsLastSyncTime;

    /**
     * The server-issued refresh id for device-scoped AB props.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.INT64)
    private long abPropsRefreshId;

    /**
     * The server-issued refresh id for web-scoped AB props.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.INT64)
    private long abPropsWebRefreshId;

    /**
     * The server-issued refresh id for group-scoped AB props.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.INT64)
    private long groupAbPropsRefreshId;

    /**
     * The per-collection sync metadata (version, LtHash, state machine), keyed by collection.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<SyncPatchType, SyncCollectionMetadata> webAppStateCollections;

    /**
     * The device-sync operations awaiting retry.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    private final List<PendingDeviceSync> pendingDeviceSyncs;

    /**
     * The per-collection pending mutation queues awaiting server confirmation; not persisted.
     */
    private final ConcurrentMap<SyncPatchType, SequencedCollection<SyncPendingMutation>> webAppStatePendingMutations;

    /**
     * The per-collection index of applied sync actions, keyed by hex-encoded index MAC; not persisted.
     */
    private final ConcurrentMap<SyncPatchType, ConcurrentMap<String, SyncActionEntry>> syncActionEntries;

    /**
     * The per-collection buffered orphan mutations; not persisted (discarded on load).
     */
    private final ConcurrentMap<SyncPatchType, OrphanMutationEntries> orphanMutationEntriesMap;

    /**
     * Whether the primary device allows all mutations from this companion.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.BOOL)
    boolean primaryAllowsAllMutations;

    /**
     * Whether the companion advertises a full history sync during pairing.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    boolean fullHistorySyncRequired;

    /**
     * Whether the decoded history payload is trimmed on receipt.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.BOOL)
    boolean historyDiscarded;

    /**
     * Whether the newsletter list is bootstrapped after login; defaults to {@code true} when unset.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BOOL)
    Boolean historyNewsletters;

    /**
     * The advertised full-sync day window, or {@code null}.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.UINT32)
    Integer historyFullSyncDays;

    /**
     * The advertised storage budget in megabytes, or {@code null}.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.UINT32)
    Integer historyStorageQuotaMb;

    /**
     * The advertised recent-sync day window, or {@code null}.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.UINT32)
    Integer historyRecentSyncDays;

    /**
     * The advertised thumbnail-sync day window, or {@code null}.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.UINT32)
    Integer historyThumbnailSyncDays;

    /**
     * The advertised maximum messages per chat in the initial sync, or {@code null}.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.UINT32)
    Integer historyMaxMessagesPerChat;

    /**
     * Whether the primary device has granted the companion complete on-demand access to the message
     * history (the desktop "all chat history" setting).
     */
    @ProtobufProperty(index = 33, type = ProtobufType.BOOL)
    boolean completeHistoryAccessGranted;

    /**
     * Constructs a sync sub-store and resets every collection's runtime state-machine fields.
     *
     * @param checkPatchMacs                     whether to verify patch MACs
     * @param syncedChats                        whether chats have synced
     * @param syncedContacts                     whether contacts have synced
     * @param syncedNewsletters                  whether newsletters have synced
     * @param syncedStatus                       whether statuses have synced
     * @param syncedBusinessCertificate          whether the business certificate has synced
     * @param appStateKeysMap                    the app-state key map, never {@code null}
     * @param missingSyncKeysMap                 the missing-key map, or {@code null} for an empty map
     * @param primaryDeviceSupportsSyncdRecovery whether the primary supports syncd recovery
     * @param externalWebBeta                    whether external web beta is enabled
     * @param primaryFeatures                    the primary feature list, or {@code null} for an empty list
     * @param clockSkewSeconds                   the clock skew in seconds
     * @param groupAbPropsEmergencyPushTimestamp the group-AB-props emergency push timestamp, or {@code null}
     * @param abPropsAbKey                       the AB-key, or {@code null}
     * @param abPropsHash                        the AB-props hash, or {@code null}
     * @param abPropsRefresh                     the AB-props refresh interval in seconds
     * @param abPropsLastSyncTime                the AB-props last-sync time, or {@code null}
     * @param abPropsRefreshId                   the device-scoped refresh id
     * @param abPropsWebRefreshId                the web-scoped refresh id
     * @param groupAbPropsRefreshId              the group-scoped refresh id
     * @param webAppStateCollections             the per-collection metadata map, or {@code null} for an empty map
     * @param pendingDeviceSyncs                 the pending device syncs, or {@code null} for an empty list
     * @param primaryAllowsAllMutations          whether the primary allows all mutations
     * @param fullHistorySyncRequired            whether the companion requests a full sync
     * @param historyDiscarded                   whether the decoded payload is trimmed on receipt
     * @param historyNewsletters                 whether newsletters are bootstrapped, or {@code null} for the default
     * @param historyFullSyncDays                the full-sync day window, or {@code null}
     * @param historyStorageQuotaMb              the storage quota in megabytes, or {@code null}
     * @param historyRecentSyncDays              the recent-sync day window, or {@code null}
     * @param historyThumbnailSyncDays           the thumbnail-sync day window, or {@code null}
     * @param historyMaxMessagesPerChat          the per-chat message cap, or {@code null}
     * @param completeHistoryAccessGranted       whether the primary granted complete history access
     */
    ProtobufLinkedWhatsAppSyncStore(boolean checkPatchMacs, boolean syncedChats, boolean syncedContacts, boolean syncedNewsletters, boolean syncedStatus, boolean syncedBusinessCertificate, LinkedHashMap<String, AppStateSyncKey> appStateKeysMap, ConcurrentMap<String, MissingDeviceSyncKey> missingSyncKeysMap, boolean primaryDeviceSupportsSyncdRecovery, boolean externalWebBeta, List<String> primaryFeatures, long clockSkewSeconds, Instant groupAbPropsEmergencyPushTimestamp, String abPropsAbKey, String abPropsHash, long abPropsRefresh, Instant abPropsLastSyncTime, long abPropsRefreshId, long abPropsWebRefreshId, long groupAbPropsRefreshId, ConcurrentMap<SyncPatchType, SyncCollectionMetadata> webAppStateCollections, List<PendingDeviceSync> pendingDeviceSyncs, boolean primaryAllowsAllMutations, boolean fullHistorySyncRequired, boolean historyDiscarded, Boolean historyNewsletters, Integer historyFullSyncDays, Integer historyStorageQuotaMb, Integer historyRecentSyncDays, Integer historyThumbnailSyncDays, Integer historyMaxMessagesPerChat, boolean completeHistoryAccessGranted) {
        this.checkPatchMacs = checkPatchMacs;
        this.syncedChats = syncedChats;
        this.syncedContacts = syncedContacts;
        this.syncedNewsletters = syncedNewsletters;
        this.syncedStatus = syncedStatus;
        this.syncedBusinessCertificate = syncedBusinessCertificate;
        this.appStateKeysMap = Objects.requireNonNull(appStateKeysMap, "appStateKeysMap cannot be null");
        this.missingSyncKeysMap = requireNonNullElseGet(missingSyncKeysMap, ConcurrentHashMap::new);
        this.primaryDeviceSupportsSyncdRecovery = primaryDeviceSupportsSyncdRecovery;
        this.externalWebBeta = externalWebBeta;
        this.primaryFeatures = requireNonNullElseGet(primaryFeatures, ArrayList::new);
        this.clockSkewSeconds = clockSkewSeconds;
        this.groupAbPropsEmergencyPushTimestamp = groupAbPropsEmergencyPushTimestamp;
        this.abPropsAbKey = abPropsAbKey;
        this.abPropsHash = abPropsHash;
        this.abPropsRefresh = abPropsRefresh;
        this.abPropsLastSyncTime = abPropsLastSyncTime;
        this.abPropsRefreshId = abPropsRefreshId;
        this.abPropsWebRefreshId = abPropsWebRefreshId;
        this.groupAbPropsRefreshId = groupAbPropsRefreshId;
        this.webAppStateCollections = requireNonNullElseGet(webAppStateCollections, ConcurrentHashMap::new);
        for (var metadata : this.webAppStateCollections.values()) {
            metadata.setState(SyncCollectionState.UP_TO_DATE);
            metadata.setRetryCount(0);
            metadata.setLastErrorTimestamp(null);
            metadata.setMacMismatch(false);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "sync store loaded: reset {0} collection state machines to up_to_date",
                    this.webAppStateCollections.size());
        }
        this.pendingDeviceSyncs = pendingDeviceSyncs == null
                ? new CopyOnWriteArrayList<>()
                : new CopyOnWriteArrayList<>(pendingDeviceSyncs);
        this.primaryAllowsAllMutations = primaryAllowsAllMutations;
        this.fullHistorySyncRequired = fullHistorySyncRequired;
        this.historyDiscarded = historyDiscarded;
        this.historyNewsletters = historyNewsletters;
        this.historyFullSyncDays = historyFullSyncDays;
        this.historyStorageQuotaMb = historyStorageQuotaMb;
        this.historyRecentSyncDays = historyRecentSyncDays;
        this.historyThumbnailSyncDays = historyThumbnailSyncDays;
        this.historyMaxMessagesPerChat = historyMaxMessagesPerChat;
        this.completeHistoryAccessGranted = completeHistoryAccessGranted;
        this.webAppStatePendingMutations = new ConcurrentHashMap<>();
        this.syncActionEntries = new ConcurrentHashMap<>();
        this.orphanMutationEntriesMap = new ConcurrentHashMap<>();
    }

    /**
     * Returns the live app-state key map backing this store.
     *
     * @return the live app-state key map
     */
    LinkedHashMap<String, AppStateSyncKey> appStateKeysMap() {
        return appStateKeysMap;
    }

    /**
     * Returns the live missing-key map backing this store.
     *
     * @return the live missing-key map
     */
    ConcurrentMap<String, MissingDeviceSyncKey> missingSyncKeysMap() {
        return missingSyncKeysMap;
    }

    /**
     * Returns the live per-collection metadata map backing this store.
     *
     * @return the live per-collection metadata map
     */
    ConcurrentMap<SyncPatchType, SyncCollectionMetadata> webAppStateCollections() {
        return webAppStateCollections;
    }

    @Override
    public boolean isFullHistorySyncRequired() {
        return fullHistorySyncRequired;
    }

    @Override
    public LinkedWhatsAppSyncStore setFullHistorySyncRequired(boolean fullSyncRequired) {
        this.fullHistorySyncRequired = fullSyncRequired;
        return this;
    }

    @Override
    public boolean isHistoryDiscarded() {
        return historyDiscarded;
    }

    @Override
    public LinkedWhatsAppSyncStore setHistoryDiscarded(boolean discarded) {
        this.historyDiscarded = discarded;
        return this;
    }

    @Override
    public boolean hasHistoryNewsletters() {
        return historyNewsletters == null || historyNewsletters;
    }

    @Override
    public LinkedWhatsAppSyncStore setHistoryNewsletters(boolean newsletters) {
        this.historyNewsletters = newsletters;
        return this;
    }

    @Override
    public OptionalInt historyFullSyncDays() {
        return historyFullSyncDays == null ? OptionalInt.empty() : OptionalInt.of(historyFullSyncDays);
    }

    @Override
    public LinkedWhatsAppSyncStore setHistoryFullSyncDays(Integer days) {
        this.historyFullSyncDays = days;
        return this;
    }

    @Override
    public OptionalInt historyStorageQuotaMb() {
        return historyStorageQuotaMb == null ? OptionalInt.empty() : OptionalInt.of(historyStorageQuotaMb);
    }

    @Override
    public LinkedWhatsAppSyncStore setHistoryStorageQuotaMb(Integer storageQuotaMb) {
        this.historyStorageQuotaMb = storageQuotaMb;
        return this;
    }

    @Override
    public OptionalInt historyRecentSyncDays() {
        return historyRecentSyncDays == null ? OptionalInt.empty() : OptionalInt.of(historyRecentSyncDays);
    }

    @Override
    public LinkedWhatsAppSyncStore setHistoryRecentSyncDays(Integer days) {
        this.historyRecentSyncDays = days;
        return this;
    }

    @Override
    public OptionalInt historyThumbnailSyncDays() {
        return historyThumbnailSyncDays == null ? OptionalInt.empty() : OptionalInt.of(historyThumbnailSyncDays);
    }

    @Override
    public LinkedWhatsAppSyncStore setHistoryThumbnailSyncDays(Integer days) {
        this.historyThumbnailSyncDays = days;
        return this;
    }

    @Override
    public OptionalInt historyMaxMessagesPerChat() {
        return historyMaxMessagesPerChat == null ? OptionalInt.empty() : OptionalInt.of(historyMaxMessagesPerChat);
    }

    @Override
    public LinkedWhatsAppSyncStore setHistoryMaxMessagesPerChat(Integer count) {
        this.historyMaxMessagesPerChat = count;
        return this;
    }

    @Override
    public boolean isCompleteHistoryAccessGranted() {
        return completeHistoryAccessGranted;
    }

    @Override
    public LinkedWhatsAppSyncStore setCompleteHistoryAccessGranted(boolean granted) {
        this.completeHistoryAccessGranted = granted;
        return this;
    }

    @Override
    public boolean checkPatchMacs() {
        return checkPatchMacs;
    }

    @Override
    public LinkedWhatsAppSyncStore setCheckPatchMacs(boolean checkPatchMacs) {
        this.checkPatchMacs = checkPatchMacs;
        return this;
    }

    @Override
    public boolean syncedChats() {
        return syncedChats;
    }

    @Override
    public LinkedWhatsAppSyncStore setSyncedChats(boolean syncedChats) {
        this.syncedChats = syncedChats;
        return this;
    }

    @Override
    public boolean syncedContacts() {
        return syncedContacts;
    }

    @Override
    public LinkedWhatsAppSyncStore setSyncedContacts(boolean syncedContacts) {
        this.syncedContacts = syncedContacts;
        return this;
    }

    @Override
    public boolean syncedNewsletters() {
        return syncedNewsletters;
    }

    @Override
    public LinkedWhatsAppSyncStore setSyncedNewsletters(boolean syncedNewsletters) {
        this.syncedNewsletters = syncedNewsletters;
        return this;
    }

    @Override
    public boolean syncedStatus() {
        return syncedStatus;
    }

    @Override
    public LinkedWhatsAppSyncStore setSyncedStatus(boolean syncedStatus) {
        this.syncedStatus = syncedStatus;
        return this;
    }

    @Override
    public boolean syncedBusinessCertificate() {
        return syncedBusinessCertificate;
    }

    @Override
    public LinkedWhatsAppSyncStore setSyncedBusinessCertificate(boolean syncedBusinessCertificate) {
        this.syncedBusinessCertificate = syncedBusinessCertificate;
        return this;
    }

    @Override
    public boolean primaryDeviceSupportsSyncdRecovery() {
        return primaryDeviceSupportsSyncdRecovery;
    }

    @Override
    public LinkedWhatsAppSyncStore setPrimaryDeviceSupportsSyncdRecovery(boolean supported) {
        this.primaryDeviceSupportsSyncdRecovery = supported;
        return this;
    }

    @Override
    public boolean externalWebBeta() {
        return externalWebBeta;
    }

    @Override
    public LinkedWhatsAppSyncStore setExternalWebBeta(boolean externalWebBeta) {
        this.externalWebBeta = externalWebBeta;
        return this;
    }

    @Override
    public List<String> primaryFeatures() {
        return List.copyOf(primaryFeatures);
    }

    @Override
    public LinkedWhatsAppSyncStore setPrimaryFeatures(List<String> primaryFeatures) {
        this.primaryFeatures = new ArrayList<>(Objects.requireNonNull(primaryFeatures, "primaryFeatures cannot be null"));
        return this;
    }

    @Override
    public boolean primaryAllowsAllMutations() {
        return primaryAllowsAllMutations;
    }

    @Override
    public LinkedWhatsAppSyncStore setPrimaryAllowsAllMutations(boolean primaryAllowsAllMutations) {
        this.primaryAllowsAllMutations = primaryAllowsAllMutations;
        return this;
    }

    @Override
    public SequencedCollection<AppStateSyncKey> appStateKeys() {
        return List.copyOf(appStateKeysMap.sequencedValues());
    }

    @Override
    public Optional<AppStateSyncKey> findWebAppStateKeyById(byte[] id) {
        return Optional.ofNullable(appStateKeysMap.get(HexFormat.of().formatHex(id)));
    }

    @Override
    public void addWebAppStateKeys(Collection<AppStateSyncKey> keys) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "adding {0} app-state sync keys", keys.size());
        for (var key : keys) {
            var hasKeyData = key.keyData()
                    .flatMap(AppStateSyncKeyData::keyData)
                    .map(data -> data.length > 0)
                    .orElse(false);
            if (!hasKeyData) {
                continue;
            }
            key.keyId()
                    .flatMap(AppStateSyncKeyId::keyId)
                    .ifPresent(keyId -> appStateKeysMap.put(HexFormat.of().formatHex(keyId), key));
        }
    }

    @Override
    public void expireAppStateKeys(Instant threshold) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "expiring app-state keys older than {0}", threshold);
        for (var entry : appStateKeysMap.entrySet()) {
            var key = entry.getValue();
            var timestamp = key.keyData()
                    .flatMap(AppStateSyncKeyData::timestamp)
                    .orElse(null);
            if (timestamp != null && !timestamp.isAfter(threshold)) {
                key.keyData().ifPresent(data -> data.setTimestamp(Instant.EPOCH));
            }
        }
    }

    @Override
    public void expireAppStateKeysByEpoch(int epoch) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "expiring app-state keys for epoch {0}", epoch);
        for (var key : appStateKeysMap.values()) {
            if (SyncKeyUtils.getKeyEpoch(key) != epoch) {
                continue;
            }
            key.keyData().ifPresent(data -> data.setTimestamp(Instant.EPOCH));
        }
    }

    @Override
    public Optional<SyncActionEntry> findSyncActionEntry(SyncPatchType patchType, byte[] indexMac) {
        var inner = syncActionEntries.get(patchType);
        if (inner == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inner.get(HexFormat.of().formatHex(indexMac)));
    }

    @Override
    public Optional<SyncActionEntry> findSyncActionEntryByActionIndex(SyncPatchType patchType, String actionIndex) {
        var inner = syncActionEntries.get(patchType);
        if (inner == null) {
            return Optional.empty();
        }
        return inner.values()
                .stream()
                .filter(entry -> actionIndex.equals(entry.actionIndex()))
                .findFirst();
    }

    @Override
    public void putSyncActionEntry(SyncPatchType patchType, byte[] indexMac, SyncActionEntry entry) {
        syncActionEntries.computeIfAbsent(patchType, _ -> new ConcurrentHashMap<>())
                .put(HexFormat.of().formatHex(indexMac), entry);
    }

    @Override
    public Optional<SyncActionEntry> removeSyncActionEntry(SyncPatchType patchType, byte[] indexMac) {
        var inner = syncActionEntries.get(patchType);
        if (inner == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inner.remove(HexFormat.of().formatHex(indexMac)));
    }

    @Override
    public void clearSyncActionEntries(SyncPatchType patchType) {
        syncActionEntries.remove(patchType);
    }

    @Override
    public Collection<SyncActionEntry> getSyncActionEntries(SyncPatchType patchType) {
        var inner = syncActionEntries.get(patchType);
        if (inner == null) {
            return List.of();
        }
        return List.copyOf(inner.values());
    }

    @Override
    public int countSyncActionEntries() {
        var total = 0;
        for (var inner : syncActionEntries.values()) {
            total += inner.size();
        }
        return total;
    }

    @Override
    public Collection<SyncActionEntry> getAllSyncActionEntries() {
        return syncActionEntries.values()
                .stream()
                .flatMap(inner -> inner.values().stream())
                .toList();
    }

    @Override
    public Collection<MissingDeviceSyncKey> missingSyncKeys() {
        return List.copyOf(missingSyncKeysMap.values());
    }

    @Override
    public Optional<MissingDeviceSyncKey> findMissingSyncKey(byte[] keyId) {
        return Optional.ofNullable(missingSyncKeysMap.get(HexFormat.of().formatHex(keyId)));
    }

    @Override
    public int missingSyncKeyCount() {
        return missingSyncKeysMap.size();
    }

    @Override
    public void addMissingSyncKey(MissingDeviceSyncKey missingKey) {
        if (Log.WARNING) LOGGER.log(Level.WARNING, "app-state sync key missing: {0}", missingKey.keyId());
        missingSyncKeysMap.put(HexFormat.of().formatHex(missingKey.keyId()), missingKey);
    }

    @Override
    public void addMissingSyncKeys(Collection<MissingDeviceSyncKey> missingKeys) {
        Objects.requireNonNull(missingKeys, "missingKeys cannot be null");
        if (Log.WARNING) LOGGER.log(Level.WARNING, "{0} app-state sync keys missing", missingKeys.size());
        for (var missingKey : missingKeys) {
            this.missingSyncKeysMap.put(HexFormat.of().formatHex(missingKey.keyId()), missingKey);
        }
    }

    @Override
    public void removeMissingSyncKey(byte[] keyId) {
        missingSyncKeysMap.remove(HexFormat.of().formatHex(keyId));
    }

    @Override
    public void addPendingMutations(SyncPatchType collectionName, Collection<? extends SyncPendingMutation> patch) {
        webAppStatePendingMutations
                .computeIfAbsent(collectionName, _ -> new ConcurrentLinkedDeque<>())
                .addAll(patch);
    }

    @Override
    public SequencedCollection<SyncPendingMutation> findPendingMutations(SyncPatchType collectionName) {
        var collectionPending = webAppStatePendingMutations.get(collectionName);
        return collectionPending == null ? List.of() : List.copyOf(collectionPending);
    }

    @Override
    public void removePendingMutations(SyncPatchType collectionName) {
        webAppStatePendingMutations.remove(collectionName);
    }

    @Override
    public void removePendingMutations(SyncPatchType collectionName, Collection<String> mutationIds) {
        if (mutationIds == null || mutationIds.isEmpty()) {
            return;
        }

        var mutationIdsSet = mutationIds instanceof Set<String> ids
                ? ids
                : new HashSet<>(mutationIds);
        webAppStatePendingMutations.computeIfPresent(collectionName, (_, pendingMutations) -> {
            pendingMutations.removeIf(pendingMutation -> mutationIdsSet.contains(pendingMutation.mutationId()));
            return pendingMutations.isEmpty() ? null : pendingMutations;
        });
    }

    @Override
    public void clearPendingMutations(SyncPatchType collectionName) {
        webAppStatePendingMutations.remove(collectionName);
    }

    @Override
    public void addOrphanMutation(SyncPatchType collectionName, OrphanMutationEntry mutation) {
        orphanMutationEntriesMap.computeIfAbsent(collectionName, _ -> new OrphanMutationEntries())
                .add(mutation);
    }

    @Override
    public List<OrphanMutationEntry> findOrphanMutations(SyncPatchType collectionName) {
        var entries = orphanMutationEntriesMap.get(collectionName);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return List.copyOf(entries.entries());
    }

    @Override
    public List<OrphanMutationEntry> findOrphanMutationsByModel(SyncPatchType collectionName, String modelId) {
        var entries = orphanMutationEntriesMap.get(collectionName);
        if (entries == null || entries.isEmpty() || modelId == null) {
            return List.of();
        }
        return entries.entries()
                .stream()
                .filter(e -> modelId.equals(e.modelId()))
                .toList();
    }

    @Override
    public void removeOrphanMutations(SyncPatchType collectionName) {
        orphanMutationEntriesMap.remove(collectionName);
    }

    @Override
    public void removeOrphanMutations(SyncPatchType collectionName, Collection<OrphanMutationEntry> entries) {
        var data = orphanMutationEntriesMap.get(collectionName);
        if (data != null) {
            data.removeAll(entries);
        }
    }

    @Override
    public void markWebAppStateDirty(SyncPatchType collectionName) {
        webAppStateCollections.compute(collectionName, (_, current) -> {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "sync collection {0} state {1} -> dirty", collectionName,
                        current == null ? null : current.state());
            }
            if (current == null) {
                return new SyncCollectionMetadataBuilder()
                        .name(collectionName)
                        .version(0)
                        .ltHash(MutationLTHash.copy(MutationLTHash.EMPTY_HASH))
                        .lastSyncTimestamp(null)
                        .state(SyncCollectionState.DIRTY)
                        .retryCount(0)
                        .lastErrorTimestamp(null)
                        .macMismatch(false)
                        .bootstrapped(false)
                        .build();
            }
            return new SyncCollectionMetadataBuilder()
                    .name(current.name())
                    .version(current.version())
                    .ltHash(current.ltHash())
                    .lastSyncTimestamp(current.lastSyncTimestamp().orElse(null))
                    .state(SyncCollectionState.DIRTY)
                    .retryCount(current.retryCount())
                    .lastErrorTimestamp(current.lastErrorTimestamp().orElse(null))
                    .macMismatch(current.macMismatch())
                    .bootstrapped(current.bootstrapped())
                    .build();
        });
    }

    @Override
    public void markWebAppStateInFlight(SyncPatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) -> {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sync collection {0} state {1} -> in_flight", collectionName, current.state());
            return new SyncCollectionMetadataBuilder()
                    .name(current.name())
                    .version(current.version())
                    .ltHash(current.ltHash())
                    .lastSyncTimestamp(current.lastSyncTimestamp().orElse(null))
                    .state(SyncCollectionState.IN_FLIGHT)
                    .retryCount(current.retryCount())
                    .lastErrorTimestamp(current.lastErrorTimestamp().orElse(null))
                    .macMismatch(current.macMismatch())
                    .bootstrapped(current.bootstrapped())
                    .build();
        });
    }

    @Override
    public void markWebAppStateUpToDate(SyncPatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) -> {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sync collection {0} state {1} -> up_to_date", collectionName, current.state());
            return new SyncCollectionMetadataBuilder()
                    .name(current.name())
                    .version(current.version())
                    .ltHash(current.ltHash())
                    .lastSyncTimestamp(Instant.now())
                    .state(SyncCollectionState.UP_TO_DATE)
                    .retryCount(0)
                    .lastErrorTimestamp(null)
                    .macMismatch(current.macMismatch())
                    .bootstrapped(current.bootstrapped())
                    .build();
        });
    }

    @Override
    public void markWebAppStatePending(SyncPatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) -> {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sync collection {0} state {1} -> pending", collectionName, current.state());
            return new SyncCollectionMetadataBuilder()
                    .name(current.name())
                    .version(current.version())
                    .ltHash(current.ltHash())
                    .lastSyncTimestamp(current.lastSyncTimestamp().orElse(null))
                    .state(SyncCollectionState.PENDING)
                    .retryCount(current.retryCount())
                    .lastErrorTimestamp(current.lastErrorTimestamp().orElse(null))
                    .macMismatch(current.macMismatch())
                    .bootstrapped(current.bootstrapped())
                    .build();
        });
    }

    @Override
    public void markWebAppStateBlocked(SyncPatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) -> {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sync collection {0} state {1} -> blocked", collectionName, current.state());
            return new SyncCollectionMetadataBuilder()
                    .name(current.name())
                    .version(current.version())
                    .ltHash(current.ltHash())
                    .lastSyncTimestamp(current.lastSyncTimestamp().orElse(null))
                    .state(SyncCollectionState.BLOCKED)
                    .retryCount(current.retryCount())
                    .lastErrorTimestamp(current.lastErrorTimestamp().orElse(null))
                    .macMismatch(current.macMismatch())
                    .bootstrapped(current.bootstrapped())
                    .build();
        });
    }

    @Override
    public void markWebAppStateErrorRetry(SyncPatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) -> {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "sync collection {0} state {1} -> error_retry, attempt {2}",
                        collectionName, current.state(), current.retryCount() + 1);
            }
            return new SyncCollectionMetadataBuilder()
                    .name(current.name())
                    .version(current.version())
                    .ltHash(current.ltHash())
                    .lastSyncTimestamp(current.lastSyncTimestamp().orElse(null))
                    .state(SyncCollectionState.ERROR_RETRY)
                    .retryCount(current.retryCount() + 1)
                    .lastErrorTimestamp(current.lastErrorTimestamp().orElseGet(Instant::now))
                    .macMismatch(current.macMismatch())
                    .bootstrapped(current.bootstrapped())
                    .build();
        });
    }

    @Override
    public void markWebAppStateErrorFatal(SyncPatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) -> {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sync collection {0} state {1} -> error_fatal", collectionName, current.state());
            return new SyncCollectionMetadataBuilder()
                    .name(current.name())
                    .version(current.version())
                    .ltHash(current.ltHash())
                    .lastSyncTimestamp(current.lastSyncTimestamp().orElse(null))
                    .state(SyncCollectionState.ERROR_FATAL)
                    .retryCount(current.retryCount())
                    .lastErrorTimestamp(null)
                    .macMismatch(current.macMismatch())
                    .bootstrapped(current.bootstrapped())
                    .build();
        });
    }

    @Override
    public boolean isCollectionInMacMismatchFatal(SyncPatchType collectionName) {
        var current = webAppStateCollections.get(collectionName);
        return current != null && current.macMismatch();
    }

    @Override
    public void markWebAppStateMacMismatch(SyncPatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) -> {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sync collection {0} mac mismatch detected", collectionName);
            return new SyncCollectionMetadataBuilder()
                    .name(current.name())
                    .version(current.version())
                    .ltHash(current.ltHash())
                    .lastSyncTimestamp(current.lastSyncTimestamp().orElse(null))
                    .state(current.state())
                    .retryCount(current.retryCount())
                    .lastErrorTimestamp(current.lastErrorTimestamp().orElse(null))
                    .macMismatch(true)
                    .bootstrapped(current.bootstrapped())
                    .build();
        });
    }

    @Override
    public SyncCollectionMetadata findWebAppState(SyncPatchType collectionName) {
        return webAppStateCollections.computeIfAbsent(collectionName, key -> {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "creating new sync collection metadata for {0}", key);
            return new SyncCollectionMetadataBuilder()
                    .name(key)
                    .version(0)
                    .ltHash(MutationLTHash.copy(MutationLTHash.EMPTY_HASH))
                    .lastSyncTimestamp(null)
                    .state(SyncCollectionState.UP_TO_DATE)
                    .retryCount(0)
                    .lastErrorTimestamp(null)
                    .macMismatch(false)
                    .bootstrapped(false)
                    .build();
        });
    }

    @Override
    public void updateWebAppStateVersion(SyncPatchType collectionName, long newVersion, byte[] newLtHash) {
        var copiedHash = MutationLTHash.copy(newLtHash);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sync collection {0} advanced to version {1}", collectionName, newVersion);
        webAppStateCollections.compute(collectionName, (_, current) ->
                new SyncCollectionMetadataBuilder()
                        .name(collectionName)
                        .version(newVersion)
                        .ltHash(copiedHash)
                        .lastSyncTimestamp(Instant.now())
                        .state(current != null ? current.state() : SyncCollectionState.UP_TO_DATE)
                        .retryCount(0)
                        .lastErrorTimestamp(null)
                        .macMismatch(current != null && current.macMismatch())
                        .bootstrapped(true)
                        .build()
        );
    }

    @Override
    public long clockSkewSeconds() {
        return clockSkewSeconds;
    }

    @Override
    public LinkedWhatsAppSyncStore setClockSkewSeconds(long clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
        return this;
    }

    @Override
    public Optional<Instant> groupAbPropsEmergencyPushTimestamp() {
        return Optional.ofNullable(groupAbPropsEmergencyPushTimestamp);
    }

    @Override
    public LinkedWhatsAppSyncStore setGroupAbPropsEmergencyPushTimestamp(Instant timestamp) {
        this.groupAbPropsEmergencyPushTimestamp = timestamp;
        return this;
    }

    @Override
    public Optional<String> abPropsAbKey() {
        return Optional.ofNullable(abPropsAbKey);
    }

    @Override
    public LinkedWhatsAppSyncStore setAbPropsAbKey(String abKey) {
        this.abPropsAbKey = abKey;
        return this;
    }

    @Override
    public Optional<String> abPropsHash() {
        return Optional.ofNullable(abPropsHash);
    }

    @Override
    public LinkedWhatsAppSyncStore setAbPropsHash(String hash) {
        this.abPropsHash = hash;
        return this;
    }

    @Override
    public OptionalLong abPropsRefresh() {
        return abPropsRefresh != 0L ? OptionalLong.of(abPropsRefresh) : OptionalLong.empty();
    }

    @Override
    public LinkedWhatsAppSyncStore setAbPropsRefresh(long refreshSeconds) {
        this.abPropsRefresh = refreshSeconds;
        return this;
    }

    @Override
    public Optional<Instant> abPropsLastSyncTime() {
        return Optional.ofNullable(abPropsLastSyncTime);
    }

    @Override
    public LinkedWhatsAppSyncStore setAbPropsLastSyncTime(Instant lastSyncTime) {
        this.abPropsLastSyncTime = lastSyncTime;
        return this;
    }

    @Override
    public long abPropsRefreshId() {
        return abPropsRefreshId;
    }

    @Override
    public LinkedWhatsAppSyncStore setAbPropsRefreshId(long refreshId) {
        this.abPropsRefreshId = refreshId;
        return this;
    }

    @Override
    public long abPropsWebRefreshId() {
        return abPropsWebRefreshId;
    }

    @Override
    public LinkedWhatsAppSyncStore setAbPropsWebRefreshId(long webRefreshId) {
        this.abPropsWebRefreshId = webRefreshId;
        return this;
    }

    @Override
    public long groupAbPropsRefreshId() {
        return groupAbPropsRefreshId;
    }

    @Override
    public LinkedWhatsAppSyncStore setGroupAbPropsRefreshId(long groupRefreshId) {
        this.groupAbPropsRefreshId = groupRefreshId;
        return this;
    }

    @Override
    public List<PendingDeviceSync> pendingDeviceSyncs() {
        return List.copyOf(pendingDeviceSyncs);
    }

    @Override
    public void addPendingDeviceSync(PendingDeviceSync sync) {
        pendingDeviceSyncs.add(sync);
    }

    @Override
    public void removePendingDeviceSync(PendingDeviceSync sync) {
        pendingDeviceSyncs.remove(sync);
    }

    @Override
    public void clearPendingDeviceSyncs() {
        pendingDeviceSyncs.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtobufLinkedWhatsAppSyncStore that)) {
            return false;
        }
        return checkPatchMacs == that.checkPatchMacs
               && syncedChats == that.syncedChats
               && syncedContacts == that.syncedContacts
               && syncedNewsletters == that.syncedNewsletters
               && syncedStatus == that.syncedStatus
               && syncedBusinessCertificate == that.syncedBusinessCertificate
               && primaryDeviceSupportsSyncdRecovery == that.primaryDeviceSupportsSyncdRecovery
               && externalWebBeta == that.externalWebBeta
               && clockSkewSeconds == that.clockSkewSeconds
               && abPropsRefresh == that.abPropsRefresh
               && abPropsRefreshId == that.abPropsRefreshId
               && abPropsWebRefreshId == that.abPropsWebRefreshId
               && groupAbPropsRefreshId == that.groupAbPropsRefreshId
               && Objects.equals(appStateKeysMap, that.appStateKeysMap)
               && Objects.equals(missingSyncKeysMap, that.missingSyncKeysMap)
               && Objects.equals(primaryFeatures, that.primaryFeatures)
               && Objects.equals(groupAbPropsEmergencyPushTimestamp, that.groupAbPropsEmergencyPushTimestamp)
               && Objects.equals(abPropsAbKey, that.abPropsAbKey)
               && Objects.equals(abPropsHash, that.abPropsHash)
               && Objects.equals(abPropsLastSyncTime, that.abPropsLastSyncTime)
               && Objects.equals(webAppStateCollections, that.webAppStateCollections)
               && Objects.equals(pendingDeviceSyncs, that.pendingDeviceSyncs)
               && primaryAllowsAllMutations == that.primaryAllowsAllMutations
               && fullHistorySyncRequired == that.fullHistorySyncRequired
               && historyDiscarded == that.historyDiscarded
               && completeHistoryAccessGranted == that.completeHistoryAccessGranted
               && Objects.equals(historyNewsletters, that.historyNewsletters)
               && Objects.equals(historyFullSyncDays, that.historyFullSyncDays)
               && Objects.equals(historyStorageQuotaMb, that.historyStorageQuotaMb)
               && Objects.equals(historyRecentSyncDays, that.historyRecentSyncDays)
               && Objects.equals(historyThumbnailSyncDays, that.historyThumbnailSyncDays)
               && Objects.equals(historyMaxMessagesPerChat, that.historyMaxMessagesPerChat)
               && Objects.equals(webAppStatePendingMutations, that.webAppStatePendingMutations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkPatchMacs, syncedChats, syncedContacts, syncedNewsletters,
                syncedStatus, syncedBusinessCertificate, appStateKeysMap, missingSyncKeysMap,
                primaryDeviceSupportsSyncdRecovery, externalWebBeta, primaryFeatures, clockSkewSeconds,
                groupAbPropsEmergencyPushTimestamp, abPropsAbKey, abPropsHash, abPropsRefresh, abPropsLastSyncTime,
                abPropsRefreshId, abPropsWebRefreshId, groupAbPropsRefreshId, webAppStateCollections,
                pendingDeviceSyncs, primaryAllowsAllMutations, fullHistorySyncRequired, historyDiscarded,
                historyNewsletters, historyFullSyncDays, historyStorageQuotaMb, historyRecentSyncDays,
                historyThumbnailSyncDays, historyMaxMessagesPerChat, completeHistoryAccessGranted,
                webAppStatePendingMutations);
    }
}
