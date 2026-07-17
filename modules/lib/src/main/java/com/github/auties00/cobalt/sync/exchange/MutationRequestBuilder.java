package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.media.MediaConnectionService;
import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReference;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReferenceBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyData;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyId;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.wire.linked.signal.KeyIdBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEntry;
import com.github.auties00.cobalt.wire.linked.sync.SyncCollectionMetadata;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.*;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.*;
import com.github.auties00.cobalt.sync.key.SyncKeyUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MediaUpload2EventBuilder;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadModeType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadResultType;
import com.github.auties00.cobalt.wire.wam.type.UploadOriginType;

import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Builds the outgoing {@code <iq xmlns="w:sync:app:state">} stanza that pushes encrypted app
 * state mutations to the server.
 *
 * <p>The builder sits at the heart of the syncd send pipeline. It loads the active sync key via
 * {@link SyncKeyUtils#findNewestKey(Collection)}, encrypts the compacted pending
 * mutations into {@link EncryptedMutation} records, computes the rolling {@code newLtHash},
 * derives the {@code snapshotMac} and {@code patchMac} via {@link MutationIntegrityVerifier},
 * and packages the result either as an inline {@code SyncdPatch} protobuf or an MMS-uploaded
 * external blob reference depending on mutation count and protobuf size. The
 * {@link #buildSyncRequest(SyncPatchType, SequencedCollection)} entry point handles the
 * single-collection push, while {@link #buildBatchedSyncRequest(Map)} merges several dirty
 * collections under one {@code <iq>}; both return upload metadata that the post-response success
 * path consumes after the server ACK.
 *
 * @implNote
 * This implementation captures upload metadata
 * ({@link SyncRequest.UploadedPatchInfo}, {@link SyncRequest.UploadedMutationInfo}) at build
 * time so the post-response success path does not have to recompute MACs or re-derive sync
 * action entries.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestBuilder")
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestBuilderBuild")
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestBuilderTypesConverter")
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestBuilderUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestEncode")
@WhatsAppWebModule(moduleName = "WAWebSyncdMMSUpload")
public final class MutationRequestBuilder {
    /**
     * The logger for {@link MutationRequestBuilder}.
     */
    private static final System.Logger LOGGER = Log.get(MutationRequestBuilder.class);

    /**
     * Holds the injected {@link LinkedWhatsAppClient} used for store access (sync keys, hash state,
     * sync action entries).
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the {@link ABPropsService} used to read {@code syncd_inline_mutations_max_count},
     * {@code syncd_patch_protobuf_max_size}, and {@code syncd_additional_mutations_count}.
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the {@link WamService} used to commit media-upload success and failure beacons for
     * the MMS-uploaded patch path.
     */
    private final WamService wamService;

    /**
     * Holds the shared {@link MediaConnectionService} used to upload external mutations to MMS4
     * when the patch exceeds the inline threshold.
     */
    private final MediaConnectionService mediaConnectionService;

    /**
     * Constructs a new {@code MutationRequestBuilder}.
     *
     * <p>The builder holds no per-collection state, so a single instance handles every patch
     * type.
     *
     * @param whatsapp the {@link LinkedWhatsAppClient} that owns the store
     * @param abPropsService the {@link ABPropsService} used to read upload thresholds
     * @param wamService the {@link WamService} used to commit media-upload events
     * @param mediaConnectionService the {@link MediaConnectionService} used to upload external mutations
     */
    public MutationRequestBuilder(LinkedWhatsAppClient whatsapp, ABPropsService abPropsService, WamService wamService,
                                  MediaConnectionService mediaConnectionService) {
        this.whatsapp = whatsapp;
        this.abPropsService = abPropsService;
        this.wamService = wamService;
        this.mediaConnectionService = Objects.requireNonNull(mediaConnectionService, "mediaConnectionService cannot be null");
    }

    /**
     * Builds an outgoing sync IQ for a single collection.
     *
     * <p>Used when only one collection has dirty patches to push. The returned
     * {@link SyncRequest} carries the built {@link StanzaBuilder} (still mutable so callers can
     * attach an id) and, when mutations were encoded, the {@link SyncRequest.UploadedPatchInfo}
     * the success path needs after the server ACKs. When the collection has not yet been
     * bootstrapped its pending patches are skipped and the request returns a snapshot-requesting
     * {@code <collection>} with no patch content.
     *
     * @implNote
     * This implementation captures the full pre-compaction pending-mutation id list so that on
     * success the caller can clear every pending mutation, not just the deduplicated subset that
     * survived {@link #compactPatch(SequencedCollection)}.
     *
     * @param patchType the collection type to sync
     * @param patches the pending mutations to include; may be empty
     * @return the {@link SyncRequest} containing the IQ {@link StanzaBuilder} and (when applicable)
     *         the {@link SyncRequest.UploadedPatchInfo}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestBuilder", exports = "buildAppStateSyncRequest", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestBuilderBuild", exports = "buildSyncIqNode", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncRequest buildSyncRequest(SyncPatchType patchType, SequencedCollection<SyncPendingMutation> patches) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building sync request for {0}: {1} pending mutations", patchType, patches.size());

        var collectionState = whatsapp.store().syncStore().findWebAppState(patchType);

        var bootstrapped = collectionState.bootstrapped();

        var collectionBuilder = new StanzaBuilder()
                .description("collection")
                .attribute("name", patchType.toString())
                .attribute("version", collectionState.version())
                .attribute("return_snapshot", !bootstrapped ? "true" : "false");

        SyncRequest.UploadedPatchInfo uploadInfo = null;
        if (bootstrapped) {
            var allMutationIds = new ArrayList<String>(patches.size());
            for (var patch : patches) {
                if (patch.mutationId() != null) {
                    allMutationIds.add(patch.mutationId());
                }
            }

            var compacted = compactPatch(patches);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "compacted {0} mutations to {1} for collection {2}", patches.size(), compacted.size(), patchType);
            if (!compacted.isEmpty()) {
                var buildResult = buildPatchProtobuf(patchType, compacted, collectionState, List.copyOf(allMutationIds));
                var patchNode = new StanzaBuilder()
                        .description("patch")
                        .content(buildResult.bytes())
                        .build();
                collectionBuilder.content(patchNode);
                uploadInfo = buildResult.uploadInfo();
            }
        } else if (!patches.isEmpty()) {
            if (Log.INFO) LOGGER.log(Level.INFO, "skipping mutations for collection {0}: initial sync incomplete", patchType);
        }

        var collectionNode = collectionBuilder.build();

        var syncNode = new StanzaBuilder()
                .description("sync")
                .content(collectionNode)
                .build();

        var node = new StanzaBuilder()
                .description("iq")
                .attribute("type", "set")
                .attribute("xmlns", "w:sync:app:state")
                .attribute("to", Jid.userServer())
                .content(syncNode);
        return new SyncRequest(node, uploadInfo);
    }

    /**
     * Pairs the encoded {@code SyncdPatch} bytes with the upload metadata captured at build time.
     *
     * <p>Internal carrier used between {@link #buildPatchProtobuf(SyncPatchType,
     * SequencedCollection, SyncCollectionMetadata, List)} and its callers so the two outputs stay paired.
     *
     * @param bytes the serialized {@code SyncdPatch} protobuf bytes
     * @param uploadInfo the upload metadata for the post-response success path
     */
    private record PatchBuildResult(byte[] bytes, SyncRequest.UploadedPatchInfo uploadInfo) {
    }

    /**
     * Carries the result of building a multi-collection batched sync request.
     *
     * <p>Returned by {@link #buildBatchedSyncRequest(Map)}. The {@link #node} is still mutable so
     * callers can attach an id; {@link #uploadInfos} maps each successful collection to the
     * metadata the success path needs; {@link #skippedUploads} flags collections whose patches
     * were dropped, either because the collection was not yet bootstrapped or because compaction
     * reduced them to empty.
     *
     * @param node the IQ {@link StanzaBuilder}
     * @param uploadInfos per-collection upload metadata; immutable
     * @param skippedUploads collections whose mutations were dropped from the request; immutable
     */
    public record BatchedSyncRequest(
            StanzaBuilder node,
            Map<SyncPatchType, SyncRequest.UploadedPatchInfo> uploadInfos,
            Set<SyncPatchType> skippedUploads
    ) {
    }

    /**
     * Builds a serialized {@code SyncdPatch} protobuf for the supplied collection.
     *
     * <p>Encrypts the mutations, generates key rotation mutations, computes the new LT-Hash,
     * derives the MACs, and either packs the patch inline or uploads it as an external blob
     * reference. The captured {@link SyncRequest.UploadedPatchInfo} lets the post-response
     * success path persist sync action entries and update LT-Hash state without reprocessing
     * mutations.
     *
     * @implNote
     * This implementation: (a) drops orphaned REMOVE mutations whose plaintext index is absent
     * from the local sync action store; (b) chains
     * {@link #buildKeyRotationMutations(SyncPatchType, SequencedCollection, Collection,
     * MutationKeys, byte[], List)} after the user mutations so old-key entries are gradually
     * re-encrypted under the new key; (c) computes the new LT-Hash via
     * {@link MutationLTHash#subtractThenAdd(byte[], List, List)}; (d) consults
     * {@code syncd_inline_mutations_max_count} and {@code syncd_patch_protobuf_max_size} to
     * decide between an inline patch and an MMS upload, clamping the count to {@code [100, 2000]}
     * and the size to {@code [10, 100]} kilobytes; (e) emits the inline patch with
     * {@code clientDebugData} populated but no version field, and the external patch with
     * neither.
     *
     * @param patchType the collection type
     * @param patches the compacted pending mutations
     * @param collectionState the current persisted app-state record (version and LT-Hash) for this collection
     * @param allMutationIds all original pending mutation ids (pre-compaction)
     * @return the serialized patch bytes paired with upload metadata
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMMSUpload", exports = "exceedInlineMutationCount", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdMMSUpload", exports = "exceedPatchProtobufSize", adaptation = WhatsAppAdaptation.ADAPTED)
    private PatchBuildResult buildPatchProtobuf(SyncPatchType patchType, SequencedCollection<SyncPendingMutation> patches, SyncCollectionMetadata collectionState, List<String> allMutationIds) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building patch protobuf for {0}: {1} mutations", patchType, patches.size());

        var keys = whatsapp.store().syncStore().appStateKeys();
        if (keys.isEmpty()) {
            throw new IllegalStateException("No app state sync keys available");
        }

        var latestKey = SyncKeyUtils.findNewestKey(keys);
        if (latestKey == null) {
            throw new IllegalStateException("No usable app state sync key found");
        }
        var latestKeyId = latestKey.keyId()
                .flatMap(AppStateSyncKeyId::keyId)
                .orElseThrow(() -> new IllegalArgumentException("No app state sync key found"));
        var latestKeyData = latestKey.keyData()
                .flatMap(AppStateSyncKeyData::keyData)
                .orElseThrow(() -> new IllegalStateException("Latest app state sync key data has no ID"));

        try (var derivedKeys = MutationKeys.ofSyncKey(latestKeyData)) {
            var storedEntries = whatsapp.store().syncStore().getSyncActionEntries(patchType);
            var storedIndices = new HashSet<String>(storedEntries.size());
            for (var entry : storedEntries) {
                if (entry.actionIndex() != null) {
                    storedIndices.add(entry.actionIndex());
                }
            }
            var filteredPatches = new ArrayList<SyncPendingMutation>(patches.size());
            for (var patch : patches) {
                if (patch.mutation().operation() == SyncdOperation.REMOVE
                        && !storedIndices.contains(patch.mutation().index())) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "dropping orphaned remove mutation for collection {0}, action {1}", patchType, new LogRedactable.Secret(patch.mutation().index()));
                    continue;
                }
                filteredPatches.add(patch);
            }

            var encryptedMutations = encryptMutations(patchType, filteredPatches, derivedKeys, latestKeyId);
            var userMutationCount = encryptedMutations.size();

            var keyRotationSources = new ArrayList<DecryptedMutation.Trusted>();
            var keyRotationMutations = buildKeyRotationMutations(patchType, patches, storedEntries, derivedKeys, latestKeyId, keyRotationSources);
            encryptedMutations.addAll(keyRotationMutations);

            var currentLtHash = collectionState.ltHash() != null ? collectionState.ltHash() : MutationLTHash.EMPTY_HASH;
            var toAdd = new ArrayList<byte[]>(encryptedMutations.size());
            var toRemove = new ArrayList<byte[]>();
            for (var encrypted : encryptedMutations) {
                if (encrypted.operation() == SyncdOperation.SET) {
                    whatsapp.store().syncStore().findSyncActionEntry(patchType, encrypted.indexMac())
                            .ifPresent(existing -> toRemove.add(existing.valueMac()));
                    toAdd.add(encrypted.valueMac());
                } else {
                    whatsapp.store().syncStore().findSyncActionEntry(patchType, encrypted.indexMac())
                            .ifPresent(existing -> toRemove.add(existing.valueMac()));
                }
            }
            var newLtHash = MutationLTHash.subtractThenAdd(currentLtHash, toAdd, toRemove).ltHash();

            var patchesIterator = filteredPatches.iterator();
            var keyRotationIterator = keyRotationSources.iterator();
            var uploadedMutations = new ArrayList<SyncRequest.UploadedMutationInfo>(encryptedMutations.size());
            var index = 0;
            for (var encrypted : encryptedMutations) {
                DecryptedMutation.Trusted source;
                if (index < userMutationCount) {
                    var pendingMutation = patchesIterator.next();
                    source = pendingMutation.mutation();
                } else {
                    source = keyRotationIterator.next();
                }
                uploadedMutations.add(new SyncRequest.UploadedMutationInfo(
                        encrypted.indexMac(),
                        encrypted.valueMac(),
                        encrypted.keyId(),
                        encrypted.operation(),
                        source.index(),
                        source.value().orElse(null),
                        source.actionVersion()
                ));
                index++;
            }

            var newVersion = collectionState.version() + 1;
            var snapshotMac = MutationIntegrityVerifier.computeSnapshotMac(
                    derivedKeys.snapshotMacKey(), newLtHash, newVersion, patchType);
            var valueMacs = encryptedMutations.stream()
                    .map(EncryptedMutation::valueMac)
                    .toList();
            var patchMac = MutationIntegrityVerifier.computePatchMac(
                    derivedKeys.patchMacKey(), snapshotMac, valueMacs, newVersion, patchType);

            var syncdMutations = new ArrayList<SyncdMutation>(encryptedMutations.size());
            for (var encrypted : encryptedMutations) {
                var record = new SyncdRecordBuilder()
                        .index(new SyncdIndexBuilder().blob(encrypted.indexMac()).build())
                        .value(new SyncdValueBuilder().blob(encrypted.encryptedValue()).build())
                        .keyId(new KeyIdBuilder().id(encrypted.keyId()).build())
                        .build();
                syncdMutations.add(new SyncdMutationBuilder()
                        .operation(encrypted.operation())
                        .record(record)
                        .build());
            }

            var deviceIndex = whatsapp.store().accountStore().jid()
                    .map(jid -> jid.device())
                    .orElse(0);
            var debugData = new PatchDebugDataBuilder()
                    .isSenderPrimary(false)
                    .senderPlatform(PatchDebugData.Platform.WEB)
                    .build();
            var clientDebugData = PatchDebugDataSpec.encode(debugData);

            var uploadInfo = new SyncRequest.UploadedPatchInfo(
                    patchType,
                    newLtHash,
                    newVersion,
                    allMutationIds,
                    List.copyOf(uploadedMutations)
            );

            var maxInlineCount = Math.min(2000, Math.max(100, SyncKeyUtils.getSyncdInlineMutationsMaxCount(abPropsService)));
            var externalRef = (ExternalBlobReference) null;
            if (syncdMutations.size() > maxInlineCount) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "collection {0} exceeds inline mutation count ({1} > {2}), uploading externally", patchType, syncdMutations.size(), maxInlineCount);
                externalRef = uploadExternalMutations(syncdMutations);
            } else {
                var inlinePatch = new SyncdPatchBuilder()
                        .mutations(syncdMutations)
                        .snapshotMac(snapshotMac)
                        .patchMac(patchMac)
                        .keyId(new KeyIdBuilder().id(latestKeyId).build())
                        .deviceIndex(deviceIndex)
                        .clientDebugData(clientDebugData)
                        .build();
                var inlineBytes = encodeSyncdPatch(inlinePatch);
                var maxSizeBytes = Math.min(100, Math.max(10, SyncKeyUtils.getSyncdPatchProtobufMaxSize(abPropsService))) * 1000L;
                if (inlineBytes.length > maxSizeBytes) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "collection {0} patch size {1} exceeds max {2}, uploading externally", patchType, inlineBytes.length, maxSizeBytes);
                    externalRef = uploadExternalMutations(syncdMutations);
                } else {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "collection {0} patch built inline: {1} bytes, {2} mutations, version {3}", patchType, inlineBytes.length, syncdMutations.size(), newVersion);
                    return new PatchBuildResult(inlineBytes, uploadInfo);
                }
            }

            var syncdPatch = new SyncdPatchBuilder()
                    .externalMutations(externalRef)
                    .snapshotMac(snapshotMac)
                    .patchMac(patchMac)
                    .keyId(new KeyIdBuilder().id(latestKeyId).build())
                    .deviceIndex(deviceIndex)
                    .build();

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "collection {0} patch built externally: {1} mutations, version {2}", patchType, syncdMutations.size(), newVersion);
            return new PatchBuildResult(encodeSyncdPatch(syncdPatch), uploadInfo);
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to build outgoing patch for " + patchType, exception);
            throw new IllegalStateException("Failed to build outgoing patch", exception);
        }
    }

    /**
     * Encrypts the supplied pending mutations into {@link EncryptedMutation} records.
     *
     * <p>SET mutations encrypt under the latest active key; REMOVE mutations look up the original
     * key recorded in the local sync action store and encrypt under that key instead. The latter
     * rule preserves the LT-Hash invariant: removing an entry must cancel the same
     * {@code valueMac} that was originally added, which can only happen if the same key encrypts
     * both directions.
     *
     * @implNote
     * This implementation throws an {@link IllegalStateException} when the original sync action
     * entry or its sync key data is missing for a REMOVE; WA Web wraps the same condition in a
     * {@code SyncdFatalError("no corresponding set mutation")} that disconnects the session,
     * whereas Cobalt's caller routes the exception through the configurable error handler
     * instead.
     *
     * @param patchType the collection type
     * @param patches the pending mutations to encrypt
     * @param derivedKeys the keys derived from the latest sync key
     * @param latestKeyId the latest sync key id
     * @return the encrypted mutations
     * @throws GeneralSecurityException when AES/HMAC operations fail
     */
    private SequencedCollection<EncryptedMutation> encryptMutations(
            SyncPatchType patchType,
            SequencedCollection<SyncPendingMutation> patches,
            MutationKeys derivedKeys,
            byte[] latestKeyId
    ) throws GeneralSecurityException {
        var result = new ArrayList<EncryptedMutation>(patches.size());
        for (var patch : patches) {
            var mutation = patch.mutation();
            EncryptedMutation encrypted;

            if (mutation.operation() == SyncdOperation.REMOVE) {
                var originalEntry = whatsapp.store().syncStore().findSyncActionEntryByActionIndex(patchType, mutation.index())
                        .orElseThrow(() -> {
                            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot find original key for remove operation on index {0} in {1}", new LogRedactable.Secret(mutation.index()), patchType);
                            return new IllegalStateException(
                                    "Cannot find original key for REMOVE operation on index: " + mutation.index()
                            );
                        });

                var originalKeyData = whatsapp.store().syncStore().findWebAppStateKeyById(originalEntry.keyId())
                        .flatMap(AppStateSyncKey::keyData)
                        .flatMap(AppStateSyncKeyData::keyData)
                        .orElseThrow(() -> {
                            if (Log.WARNING) LOGGER.log(Level.WARNING, "original sync key {0} not found for remove operation in {1}", originalEntry.keyId(), patchType);
                            return new IllegalStateException(
                                    "Original sync key not found for REMOVE operation"
                            );
                        });

                try (var originalDerivedKeys = MutationKeys.ofSyncKey(originalKeyData)) {
                    encrypted = EncryptedMutation.of(patch, originalDerivedKeys, originalEntry.keyId());
                }
            } else {
                encrypted = EncryptedMutation.of(patch, derivedKeys, latestKeyId);
            }

            result.add(encrypted);
        }
        return result;
    }

    /**
     * Generates the supplemental SET and REMOVE mutations that re-encrypt old-key entries under
     * the latest active key.
     *
     * <p>Per push the additional SET count is bounded by
     * {@code syncd_additional_mutations_count} (clamped to {@code [0, 5]}); the REMOVE leg is
     * unbounded but only fires for stored entries whose plaintext index also appears as a SET in
     * this batch, which keeps the old-key entry from lingering under both keys after the new SET
     * lands.
     *
     * @implNote
     * This implementation sorts the SET path by ascending {@code keyEpoch} so the oldest entries
     * rotate first, but iterates the REMOVE path in original insertion order to mirror WA Web's
     * unsorted handling. The {@code Trusted} record built per entry collapses WA Web's 7-field
     * pending-mutation literal into the 5-field shape Cobalt's encrypt path consumes.
     *
     * @param patchType the collection type
     * @param patches the user mutations being sent (their indices are excluded from rotation)
     * @param storedEntries the stored sync action entries for this collection
     * @param derivedKeys the keys derived from the latest sync key
     * @param latestKeyId the latest sync key id
     * @param keyRotationSourcesOut output list populated with source data for upload metadata
     * @return the additional encrypted mutations for key rotation
     * @throws GeneralSecurityException when AES/HMAC operations fail
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestBuilderBuild", exports = "_generateMutationsToUpload", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestBuilderTypesConverter", exports = "syncActionsToPendingMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    private List<EncryptedMutation> buildKeyRotationMutations(
            SyncPatchType patchType,
            SequencedCollection<SyncPendingMutation> patches,
            Collection<SyncActionEntry> storedEntries,
            MutationKeys derivedKeys,
            byte[] latestKeyId,
            List<DecryptedMutation.Trusted> keyRotationSourcesOut
    ) throws GeneralSecurityException {
        var batchIndices = new HashSet<String>();
        for (var patch : patches) {
            batchIndices.add(patch.mutation().index());
        }

        var sortedEntriesForSet = storedEntries.stream()
                .sorted(Comparator.comparingInt(e -> SyncKeyUtils.getKeyEpoch(e.keyId())))
                .toList();
        var maxAdditional = Math.min(5, Math.max(0, abPropsService.getInt(ABProp.SYNCD_ADDITIONAL_MUTATIONS_COUNT)));
        var result = new ArrayList<EncryptedMutation>();

        for (var entry : sortedEntriesForSet) {
            if (result.size() >= maxAdditional) {
                break;
            }

            if (SyncKeyUtils.syncKeyIdsEqual(entry.keyId(), latestKeyId)) {
                continue;
            }

            if (entry.actionIndex() == null || entry.actionValue() == null) {
                continue;
            }

            if (batchIndices.contains(entry.actionIndex())) {
                continue;
            }

            var trusted = new DecryptedMutation.Trusted(
                    entry.actionIndex(),
                    entry.actionValue(),
                    SyncdOperation.SET,
                    Instant.now(),
                    entry.actionVersion()
            );
            keyRotationSourcesOut.add(trusted);
            var pending = new SyncPendingMutation(trusted, 0);
            result.add(EncryptedMutation.of(pending, derivedKeys, latestKeyId));
        }

        var allSetIndices = new HashSet<String>();
        for (var patch : patches) {
            if (patch.mutation().operation() == SyncdOperation.SET) {
                allSetIndices.add(patch.mutation().index());
            }
        }
        for (var rotationSource : keyRotationSourcesOut) {
            allSetIndices.add(rotationSource.index());
        }

        for (var entry : storedEntries) {
            if (entry.actionIndex() == null || !allSetIndices.contains(entry.actionIndex())) {
                continue;
            }

            if (SyncKeyUtils.syncKeyIdsEqual(entry.keyId(), latestKeyId)) {
                continue;
            }

            if (entry.actionValue() == null) {
                continue;
            }

            var removeTrusted = new DecryptedMutation.Trusted(
                    entry.actionIndex(),
                    entry.actionValue(),
                    SyncdOperation.REMOVE,
                    Instant.now(),
                    entry.actionVersion()
            );
            keyRotationSourcesOut.add(removeTrusted);
            var removePending = new SyncPendingMutation(removeTrusted, 0);

            var originalKeyData = whatsapp.store().syncStore().findWebAppStateKeyById(entry.keyId())
                    .flatMap(AppStateSyncKey::keyData)
                    .flatMap(AppStateSyncKeyData::keyData)
                    .orElseThrow(() -> {
                        if (Log.WARNING) LOGGER.log(Level.WARNING, "original sync key {0} not found for key rotation remove in {1}", entry.keyId(), patchType);
                        return new IllegalStateException(
                                "Original sync key not found for key rotation REMOVE"
                        );
                    });
            try (var originalDerivedKeys = MutationKeys.ofSyncKey(originalKeyData)) {
                result.add(EncryptedMutation.of(removePending, originalDerivedKeys, entry.keyId()));
            }
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "generated {0} key rotation mutations for {1}", result.size(), patchType);
        return result;
    }

    /**
     * Builds an outgoing sync IQ that batches multiple collections into a single {@code <sync>}
     * child.
     *
     * <p>Used when multiple collections have dirty patches at once; one IQ per push round is
     * cheaper than one IQ per collection. The returned {@link BatchedSyncRequest#uploadInfos()}
     * is keyed by collection so the post-response success path can update each collection
     * independently, while {@link BatchedSyncRequest#skippedUploads()} flags the collections
     * whose patches were dropped before encoding.
     *
     * @implNote
     * This implementation does not delegate to {@link #buildSyncRequest(SyncPatchType,
     * SequencedCollection)} because the per-collection outputs need to be merged under a single
     * shared {@code <sync>} parent; instead the per-collection logic is inlined. The experimental
     * {@code WAWebKmpSyncdRequestBuilder.buildOutgoingRequestWithKmp} branch (gated on
     * {@code kmp_syncd_engine_outgoing_processor_enabled}) is not implemented; the AB prop
     * defaults to {@code false} so default-config callers see no difference.
     *
     * @param collectionPatches map of collection types to their pending mutations
     * @return the {@link BatchedSyncRequest} carrying the IQ {@link StanzaBuilder}, the
     *         per-collection upload metadata, and the skipped collection set
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestBuilder", exports = "buildAppStateSyncRequest", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestBuilderBuild", exports = "buildSyncIqNode", adaptation = WhatsAppAdaptation.ADAPTED)
    public BatchedSyncRequest buildBatchedSyncRequest(Map<SyncPatchType, SequencedCollection<SyncPendingMutation>> collectionPatches) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building batched sync request for {0} collections", collectionPatches.size());

        var collectionNodes = new ArrayList<Stanza>();
        var uploadInfos = new LinkedHashMap<SyncPatchType, SyncRequest.UploadedPatchInfo>();
        var skippedUploads = new LinkedHashSet<SyncPatchType>();
        for (var entry : collectionPatches.entrySet()) {
            var patchType = entry.getKey();
            var patches = entry.getValue();

            var collectionState = whatsapp.store().syncStore().findWebAppState(patchType);

            var bootstrapped = collectionState.bootstrapped();

            var collectionBuilder = new StanzaBuilder()
                    .description("collection")
                    .attribute("name", patchType.toString())
                    .attribute("version", collectionState.version())
                    .attribute("return_snapshot", !bootstrapped ? "true" : "false");

            if (bootstrapped) {
                var allMutationIds = new ArrayList<String>(patches.size());
                for (var patch : patches) {
                    if (patch.mutationId() != null) {
                        allMutationIds.add(patch.mutationId());
                    }
                }

                var compacted = compactPatch(patches);
                if (!compacted.isEmpty()) {
                    var buildResult = buildPatchProtobuf(patchType, compacted, collectionState, List.copyOf(allMutationIds));
                    var patchNode = new StanzaBuilder()
                            .description("patch")
                            .content(buildResult.bytes())
                            .build();
                    collectionBuilder.content(patchNode);
                    if (buildResult.uploadInfo() != null) {
                        uploadInfos.put(patchType, buildResult.uploadInfo());
                    }
                } else if (!patches.isEmpty()) {
                    skippedUploads.add(patchType);
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "collection {0} patches compacted to empty, skipping", patchType);
                }
            } else if (!patches.isEmpty()) {
                skippedUploads.add(patchType);
                if (Log.INFO) LOGGER.log(Level.INFO, "skipping mutations for collection {0}: initial sync incomplete", patchType);
            }

            collectionNodes.add(collectionBuilder.build());
        }

        var syncNode = new StanzaBuilder()
                .description("sync")
                .content(collectionNodes)
                .build();

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "batched sync request built: {0} collections uploaded, {1} skipped", uploadInfos.size(), skippedUploads.size());

        return new BatchedSyncRequest(
                new StanzaBuilder()
                        .description("iq")
                        .attribute("type", "set")
                        .attribute("xmlns", "w:sync:app:state")
                        .attribute("to", Jid.userServer())
                        .content(syncNode),
                Collections.unmodifiableMap(uploadInfos),
                Collections.unmodifiableSet(skippedUploads)
        );
    }

    /**
     * Uploads a list of mutations to the media CDN as an external blob and returns the populated
     * {@link ExternalBlobReference} for inclusion in the {@code SyncdPatch}.
     *
     * <p>Called when the patch exceeds either the inline-mutation count threshold or the
     * encoded-protobuf size threshold; the patch then carries a thin {@link ExternalBlobReference}
     * pointing at the CDN object while the actual mutations live in MMS storage. The MMS upload
     * result is mirrored to a media-upload beacon via
     * {@link #commitMediaUpload2Success(Instant)} or
     * {@link #commitMediaUpload2Failure(Instant, Throwable)}.
     *
     * @implNote
     * This implementation collapses WA Web's separate upload-manager call (with explicit
     * {@code type: "md-app-state"}, {@code uploadOrigin: UNKNOWN}, and similar parameters) into a
     * single {@link MediaConnectionService#upload(com.github.auties00.cobalt.wire.linked.media.MediaProvider,
     * MediaPayload)} which encrypts, uploads, and populates the {@link ExternalBlobReference}
     * fields in one round trip.
     *
     * @param mutations the mutations to upload
     * @return the populated {@link ExternalBlobReference}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdMMSUpload", exports = "uploadPatch", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdMMSUpload", exports = "buildExternalBlobReference", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdNetCallbacksApi", exports = "uploadSyncExternalPatch", adaptation = WhatsAppAdaptation.ADAPTED)
    private ExternalBlobReference uploadExternalMutations(List<SyncdMutation> mutations) {
        var syncdMutations = new SyncdMutationsBuilder()
                .mutations(mutations)
                .build();
        var encoded = encodeSyncdMutations(syncdMutations);
        var externalRef = new ExternalBlobReferenceBuilder().build();
        var uploadStart = Instant.now();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "uploading {0} mutations as external patch, {1} bytes", mutations.size(), encoded.length);
        try {
            boolean uploaded;
            try (var payload = new MediaPayload.OfBytes(encoded)) {
                uploaded = mediaConnectionService.upload(externalRef, payload);
            }
            if (!uploaded) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "missing handle after uploading external patch to mms4");
                throw new IllegalStateException("Missing handle after uploading external patch to mms4");
            }
            commitMediaUpload2Success(uploadStart);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "external patch upload succeeded, {0} mutations", mutations.size());
            return externalRef;
        } catch (InterruptedException exception) {
            commitMediaUpload2Failure(uploadStart, exception);
            Thread.currentThread().interrupt();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "interrupted while uploading external mutations", exception);
            throw new IllegalStateException("Interrupted while uploading external mutations", exception);
        } catch (IllegalStateException exception) {
            commitMediaUpload2Failure(uploadStart, exception);
            throw exception;
        } catch (Throwable throwable) {
            commitMediaUpload2Failure(uploadStart, throwable);
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to upload external mutations", throwable);
            throw new IllegalStateException("Failed to upload external mutations", throwable);
        }
    }

    /**
     * Commits a successful media-upload beacon for the app-state external patch upload that just
     * landed.
     *
     * @implNote
     * This implementation collapses WA Web's transient {@code markOverallCumT} timers into a
     * single {@code overallT} duration since the upload is a single round trip rather than the WA
     * Web resume-then-upload-then-finalize sequence; the {@code resumeHttpCode} of 404 and
     * {@code uploadHttpCode}/{@code finalizeHttpCode} of 200 are synthetic stand-ins for that
     * collapsed sequence.
     *
     * @param uploadStart the moment the upload attempt started
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaUploadMetrics",
            exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMediaUpload2Success(Instant uploadStart) {
        var overallT = Instant.ofEpochMilli(Duration.between(uploadStart, Instant.now()).toMillis());
        wamService.commit(new MediaUpload2EventBuilder()
                .overallMediaType(MediaType.MD_APP_STATE)
                .overallMmsVersion(4)
                .overallUploadOrigin(UploadOriginType.MESSAGE_HISTORY_SYNC)
                .overallUploadMode(MediaUploadModeType.REGULAR)
                .overallUploadResult(MediaUploadResultType.OK)
                .overallIsFinal(Boolean.TRUE)
                .resumeHttpCode(404)
                .uploadHttpCode(200)
                .finalizeHttpCode(200)
                .overallT(overallT)
                .overallAttemptCount(1)
                .overallRetryCount(0)
                .build());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "media upload2 success beacon committed, duration={0}ms", overallT.toEpochMilli());
    }

    /**
     * Commits a failing media-upload beacon for the app-state external patch upload that just
     * aborted.
     *
     * <p>The resulting {@link MediaUploadResultType} is determined by
     * {@link #classifyMediaUploadError(Throwable)}, and the HTTP status code (when available) is
     * stamped on both the upload and finalize legs.
     *
     * @param uploadStart the moment the upload attempt started
     * @param throwable the error that aborted the upload
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateMediaUploadMetrics",
            exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricUploadErrorResultType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void commitMediaUpload2Failure(Instant uploadStart, Throwable throwable) {
        var overallT = Instant.ofEpochMilli(Duration.between(uploadStart, Instant.now()).toMillis());
        var builder = new MediaUpload2EventBuilder()
                .overallMediaType(MediaType.MD_APP_STATE)
                .overallMmsVersion(4)
                .overallUploadOrigin(UploadOriginType.MESSAGE_HISTORY_SYNC)
                .overallUploadMode(MediaUploadModeType.REGULAR)
                .overallUploadResult(classifyMediaUploadError(throwable))
                .overallIsFinal(Boolean.TRUE)
                .overallT(overallT)
                .overallAttemptCount(1)
                .overallRetryCount(0);
        var statusCode = extractUploadHttpStatusCode(throwable);
        if (statusCode != null) {
            builder.uploadHttpCode(statusCode);
            builder.finalizeHttpCode(statusCode);
        }
        wamService.commit(builder.build());
        if (Log.WARNING) LOGGER.log(Level.WARNING, "media upload2 failure beacon committed, statusCode={0}", statusCode);
    }

    /**
     * Classifies an upload error into a {@link MediaUploadResultType} bucket for the media-upload
     * beacon.
     *
     * <p>Routes by HTTP status code when the exception carries one and by exception type
     * otherwise: an {@link InterruptedException} yields a cancel bucket, a
     * {@link WhatsAppMediaException.Upload} with a status code maps 401, 413, 415 and 507 to their
     * dedicated buckets and 5xx to a server bucket, and any other media error falls back to the
     * generic upload bucket.
     *
     * @implNote
     * This implementation collapses WA Web's prototype-chain dispatch ({@code MMSThrottleError},
     * {@code MMSUnauthorizedError}, {@code MediaTooLargeError}, {@code MediaInvalidError},
     * {@code HttpStatusCodeError}) into status-code dispatch because Cobalt does not expose a
     * class hierarchy for media errors, only an optional status code.
     *
     * @param throwable the error that aborted the upload
     * @return the matching {@link MediaUploadResultType}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getMetricUploadErrorResultType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private MediaUploadResultType classifyMediaUploadError(Throwable throwable) {
        if (throwable instanceof InterruptedException) {
            return MediaUploadResultType.ERROR_CANCEL;
        }
        if (throwable instanceof WhatsAppMediaException.Upload uploadException) {
            var statusCode = uploadException.httpStatusCode();
            if (statusCode.isPresent()) {
                return switch (statusCode.getAsInt()) {
                    case 401 -> MediaUploadResultType.ERROR_REQUEST;
                    case 413 -> MediaUploadResultType.ERROR_TOO_LARGE;
                    case 415 -> MediaUploadResultType.ERROR_BAD_MEDIA;
                    case 507 -> MediaUploadResultType.ERROR_THROTTLE;
                    default -> {
                        if (statusCode.getAsInt() >= 500) {
                            yield MediaUploadResultType.ERROR_SERVER;
                        }
                        yield MediaUploadResultType.ERROR_UPLOAD;
                    }
                };
            }
            return MediaUploadResultType.ERROR_UPLOAD;
        }
        if (throwable instanceof WhatsAppMediaException) {
            return MediaUploadResultType.ERROR_UPLOAD;
        }
        return MediaUploadResultType.ERROR_UNKNOWN;
    }

    /**
     * Extracts the HTTP status code carried by an upload error, walking the exception cause chain.
     *
     * <p>Returns the first status code found on a {@link WhatsAppMediaException} in the chain, used
     * to populate the {@code uploadHttpCode} and {@code finalizeHttpCode} fields on the failure
     * beacon.
     *
     * @implNote
     * This implementation guards against self-cause loops by breaking out as soon as
     * {@code current.getCause() == current}.
     *
     * @param throwable the error to inspect
     * @return the HTTP status code, or {@code null} when none of the wrapped exceptions carry one
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMediaMetricUtils",
            exports = "getStatusCode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Integer extractUploadHttpStatusCode(Throwable throwable) {
        var current = throwable;
        while (current != null) {
            if (current instanceof WhatsAppMediaException mediaException) {
                var statusCode = mediaException.httpStatusCode();
                if (statusCode.isPresent()) {
                    return statusCode.getAsInt();
                }
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * Deduplicates the supplied pending mutations by index, keeping the last mutation for each
     * index.
     *
     * <p>This pre-encryption pass prevents the same chat being archived, unarchived, and archived
     * again from showing up as three mutations on the wire when only the final state matters.
     *
     * @implNote
     * This implementation reverses the input, applies a unique-by-index pass, then reverses the
     * result so the last occurrence of each index is preserved in its original relative position,
     * mirroring WA Web's reverse-uniq-reverse chain.
     *
     * @param patches the pending mutations to compact
     * @return the deduplicated mutations
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestBuilderUtils", exports = "compactPatch", adaptation = WhatsAppAdaptation.ADAPTED)
    private SequencedCollection<SyncPendingMutation> compactPatch(SequencedCollection<SyncPendingMutation> patches) {
        var reversed = new ArrayList<SyncPendingMutation>(patches);
        Collections.reverse(reversed);
        var seen = new HashSet<String>();
        var deduplicated = new ArrayList<SyncPendingMutation>(reversed.size());
        for (var patch : reversed) {
            if (seen.add(patch.mutation().index())) {
                deduplicated.add(patch);
            }
        }
        Collections.reverse(deduplicated);
        return List.copyOf(deduplicated);
    }

    /**
     * Encodes a {@code SyncdPatch} protobuf into bytes, wrapping any encoder failure in a fatal
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError}.
     *
     * <p>A serialisation failure here means the patch cannot be sent and the syncd pipeline must
     * be torn down.
     *
     * @param patch the syncd patch to encode
     * @return the protobuf-encoded bytes
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError when the encoder throws
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestEncode", exports = "encodeSyncdPatch", adaptation = WhatsAppAdaptation.DIRECT)
    private static byte[] encodeSyncdPatch(SyncdPatch patch) {
        try {
            return SyncdPatchSpec.encode(patch);
        } catch (Exception exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "patch protobuf serialization failed", exception);
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "patch protobuf serialization failed", exception
            );
        }
    }

    /**
     * Encodes a {@code SyncdMutations} protobuf into bytes, wrapping any encoder failure in a
     * fatal {@link WhatsAppWebAppStateSyncException.UnexpectedError}.
     *
     * <p>Used by {@link #uploadExternalMutations(List)} to serialise the mutation block before MMS
     * upload.
     *
     * @param mutations the syncd mutations to encode
     * @return the protobuf-encoded bytes
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError when the encoder throws
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestEncode", exports = "encodeSyncdMutations", adaptation = WhatsAppAdaptation.DIRECT)
    private static byte[] encodeSyncdMutations(SyncdMutations mutations) {
        try {
            return SyncdMutationsSpec.encode(mutations);
        } catch (Exception exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "mutations protobuf serialization failed", exception);
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "mutations protobuf serialization failed", exception
            );
        }
    }
}
