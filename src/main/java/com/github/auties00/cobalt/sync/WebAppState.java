package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.exception.*;
import com.github.auties00.cobalt.io.sync.LTHash;
import com.github.auties00.cobalt.io.sync.MutationDecoder;
import com.github.auties00.cobalt.model.core.sync.CollectionState;
import com.github.auties00.cobalt.model.core.sync.PendingMutation;
import com.github.auties00.cobalt.model.core.sync.DecryptedMutation;
import com.github.auties00.cobalt.model.core.sync.SyncResponse;
import com.github.auties00.cobalt.model.proto.sync.*;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.util.SecureBytes;
import it.auties.protobuf.stream.ProtobufInputStream;

import java.io.Closeable;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.auties00.cobalt.api.WhatsappErrorHandler.Location.WEB_APP_STATE;

/**
 * Main coordinator for WhatsApp Web App State synchronization.
 *
 * <p>This class manages bidirectional synchronization of application state
 * across multiple devices using end-to-end encryption and LT-Hash verification.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Pushing local mutations to the server</li>
 *   <li>Pulling remote mutations from the server</li>
 *   <li>Encrypting and decrypting mutations</li>
 *   <li>Verifying integrity with LT-Hash</li>
 *   <li>Applying mutations via action handlers</li>
 *   <li>Managing collection states</li>
 * </ul>
 */
public final class WebAppState implements Closeable {
    private final Whatsapp whatsapp;
    private final WhatsappStore store;
    private final WebAppStateRequestBuilder requestBuilder;
    private final WebAppStateResponseParser responseParser;
    private final WebAppStateIntegrityVerifier integrityVerifier;
    private final WebAppStateActionHandlerRegistry handlerRegistry;
    private final WebAppStateRetryScheduler retryScheduler;

    /**
     * Creates a new WebAppStateManager instance.
     *
     * @param whatsapp the Whatsapp instance to use for store access and node sending
     */
    public WebAppState(Whatsapp whatsapp) {
        this.whatsapp = whatsapp;
        this.store = whatsapp.store();
        this.requestBuilder = new WebAppStateRequestBuilder(whatsapp);
        this.responseParser = new WebAppStateResponseParser();
        this.handlerRegistry = new WebAppStateActionHandlerRegistry(store);
        this.integrityVerifier = new WebAppStateIntegrityVerifier(store);
        this.retryScheduler = new WebAppStateRetryScheduler();
    }

    /**
     * Pushes local patches to the server.
     * Called from Whatsapp.pushWebAppState().
     *
     * @param patchType the collection type to sync
     * @param patches the patches to push
     */
    public void pushPatches(PatchType patchType, SequencedCollection<PendingMutation> patches) {
        // 1. Mark collection as dirty
        store.markDirty(patchType);

        // 2. Store patches as pending mutations
        whatsapp.store().addPendingMutations(patchType, patches);

        // 3. Trigger sync
        syncCollection(patchType);
    }

    /**
     * Pulls patches from the server.
     * Called from Whatsapp.pullWebAppState().
     *
     * @param patchTypes the collection types to sync
     */
    public void pullPatches(PatchType... patchTypes) {
        var pullCriticalBlock = false;
        var pullCriticalUnblockLow = false;
        var pullNonCritical = new HashSet<PatchType>();

        for (var patchType : patchTypes) {
            switch (patchType) {
                case CRITICAL_BLOCK -> pullCriticalBlock = true;
                case CRITICAL_UNBLOCK_LOW -> pullCriticalUnblockLow = true;
                default -> pullNonCritical.add(patchType);
            }
        }

        if (pullCriticalBlock) {
            syncCollection(PatchType.CRITICAL_BLOCK);
        }

        if (pullCriticalUnblockLow) {
            syncCollection(PatchType.CRITICAL_UNBLOCK_LOW);
        }

        if (!pullNonCritical.isEmpty()) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (var type : pullNonCritical) {
                    executor.execute(() -> syncCollection(type));
                }
            }
        }
    }

    private void syncCollection(PatchType patchType) {
        var remoteMutations = new ArrayList<DecryptedMutation.Trusted>();
        while(store.getMetadata(patchType).state() != CollectionState.UP_TO_DATE) {
            // 1. Get the sync response
            var syncResponse = sendSyncRequest(patchType);
            if(syncResponse.isEmpty()) {
                break;
            }

            // 2. Process the result
            var results = handleSyncResponse(syncResponse.get());
            remoteMutations.addAll(results);
        }
        if(!remoteMutations.isEmpty()) {
            applyMutations(patchType, remoteMutations);
        }
    }

    private Optional<SyncResponse> sendSyncRequest(PatchType patchType) {
        try {
            // 1. Get pending mutations
            var pending = whatsapp.store()
                    .getPendingMutations(patchType);

            // 2. Build request
            var request = requestBuilder.buildSyncRequest(patchType, pending);

            // 3. Mark as in-flight
            store.markInFlight(patchType);

            // 4. Send a request and get a response (synchronous)
            var response = whatsapp.sendNode(request);

            // 5. Handle response
            var result = responseParser.parseSyncResponse(response);
            return Optional.of(result);
        }catch(Throwable throwable) {
            handleSyncError(throwable, null);
            return Optional.empty();
        }
    }

    private SequencedCollection<DecryptedMutation.Trusted> handleSyncResponse(SyncResponse syncResponse) {
        try {
            // 1. Get all mutations from patches or snapshot
            var mutations = getOrDownloadMutations(syncResponse);
            if (mutations.isEmpty()) {
                // No updates - mark as up-to-date
                store.markUpToDate(syncResponse.collectionName());
                return List.of();
            }

            // 2. Decrypt mutations
            var untrusted = decryptMutations(mutations);

            // 3. Compute new LT-Hash
            var newHash = computeNewLTHash(syncResponse.collectionName(), untrusted);

            // 4. Verify integrity (if snapshot/patch MAC provided)
            integrityVerifier.verifyIntegrity(syncResponse, newHash);

            // 5. Update collection version and hash
            updateCollectionState(syncResponse.collectionName(), syncResponse.version(), newHash);

            // 6. Check if more data available
            if (syncResponse.hasMore()) {
                store.markPending(syncResponse.collectionName());
            } else {
                store.markUpToDate(syncResponse.collectionName());
            }

            // Return result
            return untrusted.stream()
                    .map(entry -> new DecryptedMutation.Trusted(entry.index(), entry.value(), entry.operation(), entry.timestamp()))
                    .toList();
        } catch (Exception e) {
            handleSyncError(e, syncResponse.collectionName());
            return List.of();
        }
    }

    private SequencedCollection<MutationSync> getOrDownloadMutations(SyncResponse response) {
        var result = new ArrayList<MutationSync>();
        if(response.snapshot() != null) {
            for (var record : response.snapshot().records()) {
                var sync = new MutationSync(record.operation(), record);
                result.add(sync);
            }
        }

        for (var patch : response.patches()) {
            if (patch.mutations() != null) {
                result.addAll(patch.mutations());
            }

            if (patch.hasExternalMutations()) {
                var downloadedData = downloadExternalMutation(patch.externalMutations());
                var externalMutations = decodeExternalMutation(downloadedData);
                if (externalMutations.mutations() != null) {
                    result.addAll(externalMutations.mutations());
                }
            }
        }

        return Collections.unmodifiableList(result);
    }

    private InputStream downloadExternalMutation(ExternalBlobReference externalRef) {
        try {
            return whatsapp.store()
                    .mediaConnection()
                    .orElseThrow(() -> new InternalError("Media connection not available"))
                    .download(externalRef);
        }catch (Throwable throwable) {
            throw new WebAppStateSyncGenericRetryableException("Failed to download external mutations", throwable);
        }
    }

    private MutationsSync decodeExternalMutation(InputStream downloadedData) {
        try(var protobufStream = ProtobufInputStream.fromStream(downloadedData)) {
            return MutationsSyncSpec.decode(protobufStream);
        }catch (Throwable throwable) {
            throw new WebAppStateSyncGenericRetryableException("Failed to decode external mutations", throwable);
        }
    }

    private SequencedCollection<DecryptedMutation.Untrusted> decryptMutations(SequencedCollection<MutationSync> mutations) {
        var decrypted = new ArrayList<DecryptedMutation.Untrusted>(mutations.size());

        for (var mutation : mutations) {
            var record = mutation.record();
            if (record == null || record.index() == null || record.value() == null) {
                continue;
            }

            // Get encryption key id
            var keyId = record.keyId();
            if (keyId == null || keyId.id() == null) {
                continue;
            }

            // Get encryption key
            var syncKey = whatsapp.store()
                    .findWebAppStateKeyById(keyId.id())
                    .orElseThrow(() -> new WebAppStateSyncMissingKeyException(keyId.id()));
            var keyData = syncKey.keyData();
            if (keyData == null || keyData.keyData() == null) {
                continue;
            }

            // Derive keys and decrypt
            try (var keys = WebAppStateSyncKeys.ofSyncKey(keyData.keyData())) {
                var decryptedMutation = MutationDecoder.decryptMutation(
                        record.value().blob(),
                        record.index().blob(),
                        keys,
                        mutation.operation()
                );
                decrypted.add(decryptedMutation);
            }catch (Exception e) {
                throw new WebAppStateSyncGenericRetryableException("Failed to decrypt mutation", e);
            }
        }

        return Collections.unmodifiableSequencedCollection(decrypted);
    }

    private void applyMutations(PatchType collectionName, SequencedCollection<DecryptedMutation.Trusted> remoteMutations) {
        // Step 1: Resolve conflicts with pending local mutations
        var mutationsToApply = resolveConflicts(remoteMutations, collectionName);

        // Step 2: Group mutations by action type
        var mutationsByAction = new HashMap<String, List<DecryptedMutation.Trusted>>();

        for (var mutation : mutationsToApply) {
            // Determine action name from action or setting
            var action = mutation.value().action();
            var setting = mutation.value().setting();

            String actionName = null;
            if (action.isPresent() && action.get().indexName() != null) {
                actionName = action.get().indexName();
            } else if (setting.isPresent() && setting.get().indexName() != null) {
                actionName = setting.get().indexName();
            }

            if (actionName != null) {
                mutationsByAction
                        .computeIfAbsent(actionName, _ -> new ArrayList<>())
                        .add(mutation);
            }
        }

        // Step 3: Apply each action group via its handler
        for (var entry : mutationsByAction.entrySet()) {
            var handler = handlerRegistry.findHandler(entry.getKey());
            if (handler.isEmpty()) {
                continue;
            }

            var mutations = entry.getValue();
            for (var mutation : mutations) {
                try {
                    handler.get().applyMutation(store, mutation);
                }catch (Throwable throwable) {
                    whatsapp.handleFailure(WEB_APP_STATE, throwable);
                }
            }
        }
    }

    private SequencedCollection<DecryptedMutation.Trusted> resolveConflicts(SequencedCollection<DecryptedMutation.Trusted> remoteMutations, PatchType collectionName) {
        // Create index for quick lookup of pending mutations
        var pendingByIndex = whatsapp.store()
                .getPendingMutations(collectionName)
                .stream()
                .map(PendingMutation::mutation)
                .collect(Collectors.toUnmodifiableMap(DecryptedMutation.Trusted::index, Function.identity()));

        var results = new ArrayList<DecryptedMutation.Trusted>(remoteMutations.size());
        for (var remoteMutation : remoteMutations) {
            // Get the index of the remote mutation
            var remoteIndex = remoteMutation.index();

            // Check if we have a pending local mutation with the same index
            var localMutation = pendingByIndex.get(remoteIndex);
            if(localMutation == null || remoteMutation.timestamp() >= localMutation.timestamp()) {
                results.add(remoteMutation);
            }else {
                results.add(localMutation);
            }
        }
        return Collections.unmodifiableSequencedCollection(results);
    }

    private byte[] computeNewLTHash(PatchType patchType, SequencedCollection<DecryptedMutation.Untrusted> mutations) {
        // Get current hash
        var currentHashState = whatsapp.store().findWebAppHashStateByName(patchType)
                .orElseGet(() -> new AppStateSyncHash(patchType));

        var currentHash = currentHashState.hash() != null ? currentHashState.hash() : LTHash.EMPTY_HASH;

        // Separate SET and REMOVE operations
        var toAdd = new ArrayList<byte[]>();
        var toRemove = new ArrayList<byte[]>();

        for (var mutation : mutations) {
            var indexMac = mutation.indexMac();
            var valueMac = mutation.valueMac();
            var mutationHash = SecureBytes.concat(indexMac, valueMac);
            if (mutation.operation() == RecordSync.Operation.SET) {
                toAdd.add(mutationHash);
            } else {
                toRemove.add(mutationHash);
            }
        }

        // Compute new hash
        return LTHash.subtractThenAdd(currentHash, toAdd, toRemove);
    }

    private void updateCollectionState(PatchType collectionName, long version, byte[] ltHash) {
        var hashState = new AppStateSyncHash(collectionName);
        hashState.setHash(ltHash);
        hashState.setVersion(version);
        whatsapp.store()
                .addWebAppHashState(hashState);
        store.updateVersion(collectionName, version, ltHash);
    }

    private void handleSyncError(Throwable error, PatchType collectionName) {
        if (collectionName == null) {
            return;
        }

        var metadata = store.getMetadata(collectionName);
        switch (error) {
            case WebAppStateSyncMissingKeyException missingKeyEx -> {
                store.markBlocked(collectionName);
                var keyId = missingKeyEx.keyId();
                // TODO: Request missing key with peer message
            }
            case WebAppStateSyncGenericRetryableException _ -> {
                var result = retryScheduler.scheduleRetry(
                        collectionName,
                        metadata.retryCount(),
                        () -> syncCollection(collectionName)
                );
                if(result) {
                    store.markErrorRetry(collectionName);
                }else {
                    store.markErrorFatal(collectionName);
                }
            }
            case WebAppStateSyncFatalException fatalException -> {
                store.markErrorFatal(collectionName);
                throw fatalException;
            }
            default -> {
                store.markErrorFatal(collectionName);
                throw new WebAppStateSyncFatalException(error);
            }
        }
    }

    @Override
    public void close() {
        retryScheduler.close();
    }
}

