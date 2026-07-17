package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.device.sync.MissingDeviceSyncKey;
import com.github.auties00.cobalt.wire.linked.device.sync.PendingDeviceSync;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.wire.linked.sync.mutation.OrphanMutationEntry;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEntry;
import com.github.auties00.cobalt.wire.linked.sync.SyncCollectionMetadata;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.sync.SyncPendingMutation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.SequencedCollection;

/**
 * The app-state synchronization (syncd) and feature-flag state of a WhatsApp client session.
 *
 * <p>This is the sub-store that drives WhatsApp Web/Desktop's encrypted app-state sync: the
 * per-collection state machine (version, LtHash, in-flight/error state, MAC-mismatch flag), the
 * pending and orphan mutation queues, the applied sync-action index, the app-state sync keys and the
 * known-missing keys, the history-sync policy, the per-collection "synced" bootstrap flags, the
 * device-sync retry queue, and the AB-props feature-flag bundle delivered by the server.
 *
 * @apiNote
 * Embedders reach this through {@link LinkedWhatsAppStore#syncStore()}; it is consumed almost entirely by
 * the internal app-state sync pipeline.
 *
 * @see LinkedWhatsAppStore
 */
@WhatsAppWebModule(moduleName = "WAWebGetSyncKey")
@WhatsAppWebModule(moduleName = "WAWebGetSyncAction")
@WhatsAppWebModule(moduleName = "WAWebSyncActionStore")
@WhatsAppWebModule(moduleName = "WAWebGetCollectionVersion")
@WhatsAppWebModule(moduleName = "WAWebGetMissingKey")
@WhatsAppWebModule(moduleName = "WAWebSyncdOrphan")
@WhatsAppWebModule(moduleName = "WAWebSyncdStoreMissingKeys")
@WhatsAppWebModule(moduleName = "WAWebSyncdCollectionsStateMachine")
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface LinkedWhatsAppSyncStore {
    /**
     * Returns whether the companion advertises a full history sync to the primary during pairing.
     *
     * @return {@code true} if a full sync is requested
     */
    boolean isFullHistorySyncRequired();

    /**
     * Sets whether the companion advertises a full history sync.
     *
     * @param fullSyncRequired whether to request a full sync
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setFullHistorySyncRequired(boolean fullSyncRequired);

    /**
     * Returns whether the decoded history payload is trimmed on receipt to the data Cobalt needs.
     *
     * @return {@code true} if the decoded payload is trimmed
     */
    boolean isHistoryDiscarded();

    /**
     * Sets whether the decoded history payload is trimmed on receipt.
     *
     * @param discarded whether to trim the decoded payload
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setHistoryDiscarded(boolean discarded);

    /**
     * Returns whether the newsletter list is bootstrapped after login. Defaults to {@code true}.
     *
     * @return {@code true} if newsletters are bootstrapped
     */
    boolean hasHistoryNewsletters();

    /**
     * Sets whether the newsletter list is bootstrapped after login.
     *
     * @param newsletters whether to bootstrap newsletters
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setHistoryNewsletters(boolean newsletters);

    /**
     * Returns the advertised full-sync day window.
     *
     * @return the day window, or empty to let the primary choose
     */
    OptionalInt historyFullSyncDays();

    /**
     * Sets the advertised full-sync day window.
     *
     * @param days the day window, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setHistoryFullSyncDays(Integer days);

    /**
     * Returns the advertised storage budget in megabytes.
     *
     * @return the storage quota in megabytes, or empty to compute it from available storage
     */
    OptionalInt historyStorageQuotaMb();

    /**
     * Sets the advertised storage budget in megabytes.
     *
     * @param storageQuotaMb the storage quota in megabytes, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setHistoryStorageQuotaMb(Integer storageQuotaMb);

    /**
     * Returns the advertised recent-sync day window.
     *
     * @return the recent-sync day window, or empty for the server default
     */
    OptionalInt historyRecentSyncDays();

    /**
     * Sets the advertised recent-sync day window.
     *
     * @param days the recent-sync day window, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setHistoryRecentSyncDays(Integer days);

    /**
     * Returns the advertised thumbnail-sync day window.
     *
     * @return the thumbnail-sync day window, or empty for the server default
     */
    OptionalInt historyThumbnailSyncDays();

    /**
     * Sets the advertised thumbnail-sync day window.
     *
     * @param days the thumbnail-sync day window, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setHistoryThumbnailSyncDays(Integer days);

    /**
     * Returns the advertised maximum number of messages per chat in the initial sync.
     *
     * @return the per-chat message cap, or empty for the server default
     */
    OptionalInt historyMaxMessagesPerChat();

    /**
     * Sets the advertised maximum number of messages per chat in the initial sync.
     *
     * @param count the per-chat message cap, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setHistoryMaxMessagesPerChat(Integer count);

    /**
     * Returns whether the primary device has granted this companion complete on-demand access to the
     * message history (the desktop "all chat history" setting).
     *
     * @return {@code true} if complete history access was granted
     */
    boolean isCompleteHistoryAccessGranted();

    /**
     * Sets whether the primary device has granted complete on-demand access to the message history.
     *
     * @param granted whether complete history access was granted
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setCompleteHistoryAccessGranted(boolean granted);

    /**
     * Returns whether app-state patch MACs are verified during sync.
     *
     * @return {@code true} if patch MACs are verified
     */
    boolean checkPatchMacs();

    /**
     * Sets whether app-state patch MACs are verified during sync.
     *
     * @param checkPatchMacs whether to verify patch MACs
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setCheckPatchMacs(boolean checkPatchMacs);

    /**
     * Returns whether the initial chat history sync has completed.
     *
     * @return {@code true} if chats have been synced
     */
    boolean syncedChats();

    /**
     * Sets the chat-sync-completed flag.
     *
     * @param syncedChats whether chats have been synced
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setSyncedChats(boolean syncedChats);

    /**
     * Returns whether the initial contact history sync has completed.
     *
     * @return {@code true} if contacts have been synced
     */
    boolean syncedContacts();

    /**
     * Sets the contact-sync-completed flag.
     *
     * @param syncedContacts whether contacts have been synced
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setSyncedContacts(boolean syncedContacts);

    /**
     * Returns whether the initial newsletter subscription sync has completed.
     *
     * @return {@code true} if newsletters have been synced
     */
    boolean syncedNewsletters();

    /**
     * Sets the newsletter-sync-completed flag.
     *
     * @param syncedNewsletters whether newsletters have been synced
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setSyncedNewsletters(boolean syncedNewsletters);

    /**
     * Returns whether the initial status history sync has completed.
     *
     * @return {@code true} if statuses have been synced
     */
    boolean syncedStatus();

    /**
     * Sets the status-sync-completed flag.
     *
     * @param syncedStatus whether statuses have been synced
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setSyncedStatus(boolean syncedStatus);

    /**
     * Returns whether the business-verified-name certificate has been fetched.
     *
     * @return {@code true} if the business certificate has been synced
     */
    boolean syncedBusinessCertificate();

    /**
     * Sets the business-certificate-synced flag.
     *
     * @param syncedBusinessCertificate whether the business certificate has been synced
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setSyncedBusinessCertificate(boolean syncedBusinessCertificate);

    /**
     * Returns whether the primary device supports the syncd snapshot-recovery flow.
     *
     * @return {@code true} if the primary supports syncd recovery
     */
    boolean primaryDeviceSupportsSyncdRecovery();

    /**
     * Sets whether the primary device supports the syncd snapshot-recovery flow.
     *
     * @param supported whether the primary supports syncd recovery
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setPrimaryDeviceSupportsSyncdRecovery(boolean supported);

    /**
     * Returns whether the client opted into the external-web-beta AB-prop bucket.
     *
     * @return {@code true} if external web beta is enabled
     */
    boolean externalWebBeta();

    /**
     * Sets the external-web-beta opt-in flag.
     *
     * @param externalWebBeta whether external web beta is enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setExternalWebBeta(boolean externalWebBeta);

    /**
     * Returns the feature gates the primary device reports it understands.
     *
     * @return an unmodifiable copy of the primary feature list
     */
    List<String> primaryFeatures();

    /**
     * Sets the primary feature list.
     *
     * @param primaryFeatures the feature list, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setPrimaryFeatures(List<String> primaryFeatures);

    /**
     * Returns whether the primary device allows all mutations from this companion.
     *
     * @return {@code true} if the primary allows all mutations
     */
    boolean primaryAllowsAllMutations();

    /**
     * Sets whether the primary device allows all mutations from this companion.
     *
     * @param primaryAllowsAllMutations whether the primary allows all mutations
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setPrimaryAllowsAllMutations(boolean primaryAllowsAllMutations);

    /**
     * Returns all app-state sync keys in insertion order.
     *
     * @return an unmodifiable sequenced collection of sync keys
     */
    SequencedCollection<AppStateSyncKey> appStateKeys();

    /**
     * Looks up an app-state sync key by its raw id.
     *
     * @param id the key id bytes
     * @return the matching key, or empty if none is stored
     */
    Optional<AppStateSyncKey> findWebAppStateKeyById(byte[] id);

    /**
     * Stores app-state sync keys, ignoring entries without key data.
     *
     * @param keys the keys to store
     */
    void addWebAppStateKeys(Collection<AppStateSyncKey> keys);

    /**
     * Expires app-state keys whose timestamp is at or before the given threshold.
     *
     * @param threshold the expiry threshold
     */
    void expireAppStateKeys(Instant threshold);

    /**
     * Expires app-state keys belonging to the given epoch.
     *
     * @param epoch the key epoch to expire
     */
    void expireAppStateKeysByEpoch(int epoch);

    /**
     * Looks up an applied sync-action entry by its index MAC.
     *
     * @param patchType the collection
     * @param indexMac  the action index MAC
     * @return the entry, or empty if not present
     */
    Optional<SyncActionEntry> findSyncActionEntry(SyncPatchType patchType, byte[] indexMac);

    /**
     * Looks up an applied sync-action entry by its action index string.
     *
     * @param patchType   the collection
     * @param actionIndex the action index string
     * @return the entry, or empty if not present
     */
    Optional<SyncActionEntry> findSyncActionEntryByActionIndex(SyncPatchType patchType, String actionIndex);

    /**
     * Stores an applied sync-action entry.
     *
     * @param patchType the collection
     * @param indexMac  the action index MAC
     * @param entry     the entry
     */
    void putSyncActionEntry(SyncPatchType patchType, byte[] indexMac, SyncActionEntry entry);

    /**
     * Removes an applied sync-action entry by its index MAC.
     *
     * @param patchType the collection
     * @param indexMac  the action index MAC
     * @return the removed entry, or empty if not present
     */
    Optional<SyncActionEntry> removeSyncActionEntry(SyncPatchType patchType, byte[] indexMac);

    /**
     * Clears all applied sync-action entries for a collection.
     *
     * @param patchType the collection
     */
    void clearSyncActionEntries(SyncPatchType patchType);

    /**
     * Returns the applied sync-action entries for a collection.
     *
     * @param patchType the collection
     * @return an unmodifiable copy of the entries
     */
    Collection<SyncActionEntry> getSyncActionEntries(SyncPatchType patchType);

    /**
     * Returns the total number of applied sync-action entries across all collections.
     *
     * @return the entry count
     */
    int countSyncActionEntries();

    /**
     * Returns every applied sync-action entry across all collections.
     *
     * @return the entries
     */
    Collection<SyncActionEntry> getAllSyncActionEntries();

    /**
     * Returns the app-state sync keys known to be missing.
     *
     * @return an unmodifiable copy of the missing keys
     */
    Collection<MissingDeviceSyncKey> missingSyncKeys();

    /**
     * Looks up a missing sync key by its id.
     *
     * @param keyId the key id bytes
     * @return the missing key record, or empty if not tracked
     */
    Optional<MissingDeviceSyncKey> findMissingSyncKey(byte[] keyId);

    /**
     * Returns the number of tracked missing sync keys.
     *
     * @return the missing key count
     */
    int missingSyncKeyCount();

    /**
     * Tracks a missing sync key.
     *
     * @param missingKey the missing key record
     */
    void addMissingSyncKey(MissingDeviceSyncKey missingKey);

    /**
     * Tracks a batch of missing sync keys.
     *
     * @param missingKeys the missing key records, never {@code null}
     */
    void addMissingSyncKeys(Collection<MissingDeviceSyncKey> missingKeys);

    /**
     * Stops tracking a missing sync key.
     *
     * @param keyId the key id bytes
     */
    void removeMissingSyncKey(byte[] keyId);

    /**
     * Appends pending mutations to a collection's queue.
     *
     * @param collectionName the collection
     * @param patch          the pending mutations
     */
    void addPendingMutations(SyncPatchType collectionName, Collection<? extends SyncPendingMutation> patch);

    /**
     * Returns the pending mutations queued for a collection.
     *
     * @param collectionName the collection
     * @return an unmodifiable copy of the pending mutations
     */
    SequencedCollection<SyncPendingMutation> findPendingMutations(SyncPatchType collectionName);

    /**
     * Removes all pending mutations for a collection.
     *
     * @param collectionName the collection
     */
    void removePendingMutations(SyncPatchType collectionName);

    /**
     * Removes specific pending mutations by id from a collection.
     *
     * @param collectionName the collection
     * @param mutationIds    the mutation ids to remove
     */
    void removePendingMutations(SyncPatchType collectionName, Collection<String> mutationIds);

    /**
     * Clears the pending mutation queue for a collection.
     *
     * @param collectionName the collection
     */
    void clearPendingMutations(SyncPatchType collectionName);

    /**
     * Buffers an orphan mutation whose parent entity has not yet arrived.
     *
     * @param collectionName the collection
     * @param mutation       the orphan mutation
     */
    void addOrphanMutation(SyncPatchType collectionName, OrphanMutationEntry mutation);

    /**
     * Returns the buffered orphan mutations for a collection.
     *
     * @param collectionName the collection
     * @return an unmodifiable copy of the orphan mutations
     */
    List<OrphanMutationEntry> findOrphanMutations(SyncPatchType collectionName);

    /**
     * Returns the buffered orphan mutations for a collection that target a given model.
     *
     * @param collectionName the collection
     * @param modelId        the model id
     * @return the matching orphan mutations
     */
    List<OrphanMutationEntry> findOrphanMutationsByModel(SyncPatchType collectionName, String modelId);

    /**
     * Removes all buffered orphan mutations for a collection.
     *
     * @param collectionName the collection
     */
    void removeOrphanMutations(SyncPatchType collectionName);

    /**
     * Removes specific buffered orphan mutations from a collection.
     *
     * @param collectionName the collection
     * @param entries        the orphan mutations to remove
     */
    void removeOrphanMutations(SyncPatchType collectionName, Collection<OrphanMutationEntry> entries);

    /**
     * Transitions a collection into the dirty state, creating its record if absent.
     *
     * @param collectionName the collection
     */
    void markWebAppStateDirty(SyncPatchType collectionName);

    /**
     * Transitions a collection into the in-flight state.
     *
     * @param collectionName the collection
     */
    void markWebAppStateInFlight(SyncPatchType collectionName);

    /**
     * Transitions a collection into the up-to-date state and stamps the sync time.
     *
     * @param collectionName the collection
     */
    void markWebAppStateUpToDate(SyncPatchType collectionName);

    /**
     * Transitions a collection into the pending state.
     *
     * @param collectionName the collection
     */
    void markWebAppStatePending(SyncPatchType collectionName);

    /**
     * Transitions a collection into the blocked state.
     *
     * @param collectionName the collection
     */
    void markWebAppStateBlocked(SyncPatchType collectionName);

    /**
     * Transitions a collection into the error-retry state and increments its retry count.
     *
     * @param collectionName the collection
     */
    void markWebAppStateErrorRetry(SyncPatchType collectionName);

    /**
     * Transitions a collection into the fatal-error state.
     *
     * @param collectionName the collection
     */
    void markWebAppStateErrorFatal(SyncPatchType collectionName);

    /**
     * Returns whether a collection is in the fatal MAC-mismatch state.
     *
     * @param collectionName the collection
     * @return {@code true} if the collection has a MAC mismatch flagged
     */
    boolean isCollectionInMacMismatchFatal(SyncPatchType collectionName);

    /**
     * Flags a collection as having a MAC mismatch.
     *
     * @param collectionName the collection
     */
    void markWebAppStateMacMismatch(SyncPatchType collectionName);

    /**
     * Returns the metadata record for a collection, creating a fresh up-to-date record if absent.
     *
     * @param collectionName the collection
     * @return the collection metadata
     */
    SyncCollectionMetadata findWebAppState(SyncPatchType collectionName);

    /**
     * Updates a collection's version and LtHash and stamps the sync time.
     *
     * @param collectionName the collection
     * @param newVersion     the new version
     * @param newLtHash      the new LtHash
     */
    void updateWebAppStateVersion(SyncPatchType collectionName, long newVersion, byte[] newLtHash);

    /**
     * Returns the observed clock skew between local and server time in seconds.
     *
     * @return the clock skew in seconds
     */
    long clockSkewSeconds();

    /**
     * Sets the observed clock skew in seconds.
     *
     * @param clockSkewSeconds the clock skew
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setClockSkewSeconds(long clockSkewSeconds);

    /**
     * Returns the timestamp of the most recent emergency push of group-scoped AB props.
     *
     * @return the emergency push timestamp, or empty if none
     */
    Optional<Instant> groupAbPropsEmergencyPushTimestamp();

    /**
     * Sets the group-AB-props emergency push timestamp.
     *
     * @param timestamp the timestamp, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setGroupAbPropsEmergencyPushTimestamp(Instant timestamp);

    /**
     * Returns the opaque AB-key echoed on the next AB-props refresh.
     *
     * @return the AB-key, or empty if none
     */
    Optional<String> abPropsAbKey();

    /**
     * Sets the AB-key.
     *
     * @param abKey the AB-key, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setAbPropsAbKey(String abKey);

    /**
     * Returns the content hash of the cached AB-props bundle.
     *
     * @return the AB-props hash, or empty if none
     */
    Optional<String> abPropsHash();

    /**
     * Sets the AB-props content hash.
     *
     * @param hash the hash, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setAbPropsHash(String hash);

    /**
     * Returns the server-suggested AB-props refresh interval in seconds.
     *
     * @return the refresh interval, or empty if unset
     */
    OptionalLong abPropsRefresh();

    /**
     * Sets the AB-props refresh interval in seconds.
     *
     * @param refreshSeconds the refresh interval
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setAbPropsRefresh(long refreshSeconds);

    /**
     * Returns the timestamp of the most recent successful AB-props fetch.
     *
     * @return the last sync time, or empty if never fetched
     */
    Optional<Instant> abPropsLastSyncTime();

    /**
     * Sets the AB-props last-sync timestamp.
     *
     * @param lastSyncTime the timestamp, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setAbPropsLastSyncTime(Instant lastSyncTime);

    /**
     * Returns the server-issued refresh id for device-scoped AB props.
     *
     * @return the device-scoped refresh id
     */
    long abPropsRefreshId();

    /**
     * Sets the device-scoped AB-props refresh id.
     *
     * @param refreshId the refresh id
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setAbPropsRefreshId(long refreshId);

    /**
     * Returns the server-issued refresh id for web-scoped AB props.
     *
     * @return the web-scoped refresh id
     */
    long abPropsWebRefreshId();

    /**
     * Sets the web-scoped AB-props refresh id.
     *
     * @param webRefreshId the refresh id
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setAbPropsWebRefreshId(long webRefreshId);

    /**
     * Returns the server-issued refresh id for group-scoped AB props.
     *
     * @return the group-scoped refresh id
     */
    long groupAbPropsRefreshId();

    /**
     * Sets the group-scoped AB-props refresh id.
     *
     * @param groupRefreshId the refresh id
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSyncStore setGroupAbPropsRefreshId(long groupRefreshId);

    /**
     * Returns the device-sync operations awaiting retry.
     *
     * @return an unmodifiable copy of the pending device syncs
     */
    List<PendingDeviceSync> pendingDeviceSyncs();

    /**
     * Queues a device-sync operation for retry.
     *
     * @param sync the pending sync
     */
    void addPendingDeviceSync(PendingDeviceSync sync);

    /**
     * Removes a queued device-sync operation.
     *
     * @param sync the sync to remove
     */
    void removePendingDeviceSync(PendingDeviceSync sync);

    /**
     * Clears all queued device-sync operations.
     */
    void clearPendingDeviceSyncs();
}
