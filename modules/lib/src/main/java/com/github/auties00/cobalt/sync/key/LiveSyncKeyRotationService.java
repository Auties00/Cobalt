package com.github.auties00.cobalt.sync.key;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.send.id.MessageIdGenerator;
import com.github.auties00.cobalt.message.send.id.MessageIdVersion;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.*;
import com.github.auties00.cobalt.wire.linked.sync.SyncCollectionState;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.sync.SyncdCoordinator;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.util.ScheduledTask;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MdAppStateKeyRotationEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdBootstrapAppStateCriticalDataProcessingEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.BootstrapAppStateDataStageCode;
import com.github.auties00.cobalt.wire.wam.type.MdAppStateKeyRotationReasonCode;

import java.lang.System.Logger.Level;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Live implementation of {@link SyncKeyRotationService} that manages the lifecycle of the active
 * app state sync key: detects expiry, detects companion device removal, mints fresh keys with a
 * monotonically increasing epoch, and shares them with every peer device.
 *
 * <p>Two entry points dominate. {@link #getActiveKey(boolean)} resolves the current key for
 * outgoing patches and rotates inline when a rotation trigger fires.
 * {@link #handleKeyShare(int, List)} consumes inbound {@code AppStateSyncKeyShare}
 * {@link ProtocolMessage} payloads from companion devices and reconciles them against the local
 * key store and the missing-key tracker. A periodic 27-day background check
 * ({@link #startPeriodicRotationJob()}) ensures rotation also happens during long
 * mutation-free windows.
 *
 * @implNote This implementation serialises the rotation flow through the shared
 * {@link SyncdCoordinator} monitor so the read-then-rotate-then-store sequence is atomic against
 * the apply path and inbound key shares; the key-share peer send runs with the monitor released
 * so no lock is held across network I/O.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdKeyManagement")
@WhatsAppWebModule(moduleName = "WAWebSyncdRotateKey")
@WhatsAppWebModule(moduleName = "WAWebSyncdKeyCallbacksApi")
@WhatsAppWebModule(moduleName = "WAWebSyncdHandleKeyShare")
@WhatsAppWebModule(moduleName = "WAWebKeyManagementSendKeyShareApi")
@WhatsAppWebModule(moduleName = "WAWebKeyManagementUtils")
@WhatsAppWebModule(moduleName = "WAWebTasksDefinitions")
public final class LiveSyncKeyRotationService implements SyncKeyRotationService {
    /**
     * The logger for {@link LiveSyncKeyRotationService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveSyncKeyRotationService.class);

    /**
     * Holds the lower bound applied to {@code syncd_key_max_use_days} after AB-prop clamping.
     */
    private static final int MIN_KEY_MAX_USE_DAYS = 1;

    /**
     * Holds the upper bound applied to {@code syncd_key_max_use_days} after AB-prop clamping.
     */
    private static final int MAX_KEY_MAX_USE_DAYS = 90;

    /**
     * Holds the interval between periodic rotation checks driven by
     * {@link #startPeriodicRotationJob()}.
     *
     * @implNote This implementation uses 27 days to match the WA Web rotation-task schedule.
     */
    private static final Duration PERIODIC_ROTATION_INTERVAL = Duration.ofDays(27);

    /**
     * Holds the source of randomness used to seed the 32-byte key data of every newly rotated
     * key.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Holds the injected client used to read and update the sync key store and to dispatch the
     * {@code AppStateSyncKeyShare} peer messages.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the owning service used to schedule missing-key follow-ups when a negative key
     * share resolves a tracker entry.
     */
    private final WebAppStateService webAppStateService;

    /**
     * Holds the AB prop source used to read {@code syncd_key_max_use_days} and the
     * persist-after-server-ack gate.
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the WAM service used to commit rotation and bootstrap-progress events.
     */
    private final WamService wamService;

    /**
     * Holds the shared syncd coordinator whose monitor serializes inbound key-share ingestion
     * and key rotation against the apply path, replacing the former private rotation monitor.
     */
    private final SyncdCoordinator coordinator;

    /**
     * Holds the handle of the periodic 27-day rotation check, or {@code null} until
     * {@link #startPeriodicRotationJob()} is called.
     */
    private volatile ScheduledTask periodicRotationJob;

    /**
     * Constructs a new rotation service bound to the supplied dependencies.
     *
     * @param whatsapp the client that owns the store and the peer-message channel
     * @param webAppStateService the owning service
     * @param abPropsService the AB prop source used to read rotation thresholds
     * @param wamService the WAM service used to commit rotation events
     * @param coordinator the shared syncd coordinator serializing key-share and rotation
     */
    public LiveSyncKeyRotationService(LinkedWhatsAppClient whatsapp, WebAppStateService webAppStateService, ABPropsService abPropsService, WamService wamService, SyncdCoordinator coordinator) {
        this.whatsapp = whatsapp;
        this.webAppStateService = webAppStateService;
        this.abPropsService = abPropsService;
        this.wamService = wamService;
        this.coordinator = coordinator;
    }

    /**
     * {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdHandleKeyShare",
            exports = "handleKeyShare",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void handleKeyShare(int senderDeviceId, List<AppStateSyncKey> keys) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "handling key share from device {0}, keys={1}", senderDeviceId, keys.size());
        var hasAnyKeyData = keys.stream()
                .anyMatch(key -> key.keyData()
                        .flatMap(AppStateSyncKeyData::keyData)
                        .map(data -> data.length > 0)
                        .orElse(false));
        if (!hasAnyKeyData) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "key share from device {0} has no keys with keydata", senderDeviceId);
        }

        var blockedToPull = coordinator.runLocked(() -> {
            var newKeysStored = new ArrayList<AppStateSyncKey>();
            var resolvedAny = false;

            for (var key : keys) {
                var keyIdBytes = key.keyId()
                        .flatMap(AppStateSyncKeyId::keyId)
                        .orElse(null);
                if (keyIdBytes == null) {
                    continue;
                }

                var hasKeyData = key.keyData()
                        .flatMap(AppStateSyncKeyData::keyData)
                        .map(data -> data.length > 0)
                        .orElse(false);
                var keyIdHex = SyncKeyUtils.syncKeyIdToHex(keyIdBytes);

                if (hasKeyData) {
                    var existingKey = whatsapp.store().syncStore().findWebAppStateKeyById(keyIdBytes).orElse(null);

                    if (existingKey == null) {
                        newKeysStored.add(key);

                        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "stored key share, keyId={0} device={1}", keyIdHex, senderDeviceId);
                    } else {
                        var existingKeyData = existingKey.keyData()
                                .flatMap(AppStateSyncKeyData::keyData)
                                .orElse(null);
                        var incomingKeyData = key.keyData()
                                .flatMap(AppStateSyncKeyData::keyData)
                                .orElse(null);
                        if (existingKeyData != null && incomingKeyData != null
                                && !Arrays.equals(existingKeyData, incomingKeyData)) {
                            if (Log.ERROR) LOGGER.log(Level.ERROR, "key share mismatch: existing key data differs, keyId={0} device={1}", keyIdHex, senderDeviceId);
                        }
                    }

                    if (whatsapp.store().syncStore().findMissingSyncKey(keyIdBytes).isPresent()) {
                        whatsapp.store().syncStore().removeMissingSyncKey(keyIdBytes);
                        resolvedAny = true;
                    }
                } else {
                    if (senderDeviceId >= 0) {
                        var missingKey = whatsapp.store().syncStore().findMissingSyncKey(keyIdBytes).orElse(null);
                        if (missingKey != null && missingKey.wasAsked(senderDeviceId)) {
                            if (!missingKey.hasDeviceRespondedWithoutKey(senderDeviceId)) {
                                missingKey.markDeviceRespondedWithoutKey(senderDeviceId);
                            }
                            if (missingKey.isMissingOnAllDevices()) {
                                webAppStateService.scheduleAllDevicesRespondedCheck();
                            }
                        }
                    }
                }
            }

            if (!newKeysStored.isEmpty()) {
                whatsapp.store().syncStore().addWebAppStateKeys(newKeysStored);
            }

            if (resolvedAny) {
                webAppStateService.rescheduleMissingSyncKeyTimeout();
            }

            var blocked = new ArrayList<SyncPatchType>();
            for (var patchType : SyncPatchType.values()) {
                if (whatsapp.store().syncStore().findWebAppState(patchType).state() == SyncCollectionState.BLOCKED) {
                    whatsapp.store().syncStore().markWebAppStateDirty(patchType);
                    blocked.add(patchType);
                }
            }
            return blocked;
        });

        if (!blockedToPull.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "key share unblocked {0} sync collection(s): {1}", blockedToPull.size(), blockedToPull);
            whatsapp.pullWebAppState(blockedToPull.toArray(SyncPatchType[]::new));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation is a thin shim onto
     * {@link #logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode)} that exists
     * only because the validator and the storage path live in different Cobalt classes; WA Web
     * fuses both into one function body.
     */
    @WhatsAppWebExport(moduleName = "WAWebKeyManagementHandleKeyShareApi", exports = "handleAppStateSyncKeyShare", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void logMissingKeysReceived() {
        logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode.MISSING_KEYS_RECEIVED);
    }

    /**
     * Emits a critical-data processing event for the supplied bootstrap stage when the critical
     * data sync is still in progress.
     *
     * <p>Drives the WAM-side critical-data progress beacon that populates the
     * preparing-your-data sub-states on first load. The method is a no-op once the
     * {@link SyncPatchType#CRITICAL_BLOCK} collection has been bootstrapped.
     *
     * @implNote This implementation approximates WA Web's critical-data sync state machine by
     * reading the {@code bootstrapped} flag for {@link SyncPatchType#CRITICAL_BLOCK}, mirroring
     * {@link WebAppStateService}.
     *
     * @param stage the bootstrap stage reached
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCriticalBootstrapProcessingApi", exports = "logCriticalBootstrapStageIfNecessary", adaptation = WhatsAppAdaptation.ADAPTED)
    private void logCriticalBootstrapStageIfNecessary(BootstrapAppStateDataStageCode stage) {
        if (whatsapp.store().syncStore().findWebAppState(SyncPatchType.CRITICAL_BLOCK).bootstrapped()) {
            return;
        }
        wamService.commit(new MdBootstrapAppStateCriticalDataProcessingEventBuilder()
                .bootstrapAppStateDataStage(stage)
                .mdTimestamp((int) System.currentTimeMillis())
                .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureActiveKey(boolean triggerRotation) {
        getActiveKey(triggerRotation);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation serialises the read-rotate-store sequence under the shared
     * {@link SyncdCoordinator} monitor so two concurrent encrypt callers cannot race into rotating
     * twice in a row, and so a rotation cannot interleave with the apply path or an inbound key
     * share.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdKeyManagement",
            exports = "getActiveKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public AppStateSyncKey getActiveKey(boolean triggerRotation) {
        return coordinator.runLocked(() -> getActiveKeyInternal(triggerRotation));
    }

    /**
     * Performs the read-rotate-store sequence inside the {@link SyncdCoordinator} monitor.
     *
     * <p>Must be called only with the coordinator monitor held. Returns the newest key unchanged
     * when rotation is not requested or not needed; otherwise mints a fresh key, persists and
     * shares it in the order dictated by the persist-after-server-ack gate, commits the
     * appropriate rotation-reason WAM event, and returns the new key. Throws when no key exists
     * at all.
     *
     * @implNote This implementation returns the freshly rotated key directly rather than
     * recursing: the recursive resolution would return the same key now that storage is
     * committed and neither rotation trigger fires for a brand-new key, so avoiding it keeps the
     * lock-held window short and removes one stack frame per encrypt call.
     *
     * @param triggerRotation whether to rotate when the active key is stale
     * @return the active {@link AppStateSyncKey}
     * @throws IllegalStateException when no sync key exists at all
     */
    private AppStateSyncKey getActiveKeyInternal(boolean triggerRotation) {
        var newestKey = getNewestKeyPair();
        var currentFingerprint = getCurrentDeviceFingerprint();
        var expired = false;
        var deviceRemoved = false;

        if (newestKey != null) {
            expired = hasKeyExpired(newestKey);
            deviceRemoved = hasADeviceBeenRemoved(newestKey, currentFingerprint);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "active key check: expired={0} deviceRemoved={1} triggerRotation={2}",
                    expired, deviceRemoved, triggerRotation);

            if (!triggerRotation || (!expired && !deviceRemoved)) {
                return newestKey;
            }
        } else {
            throw new IllegalStateException("syncd: No sync key available");
        }

        var rotatedKey = rotateKey(currentFingerprint, newestKey);
        if (rotatedKey == null) {
            return newestKey;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "rotating sync key, new keyId={0}", SyncKeyUtils.syncKeyIdToHex(rotatedKey));

        if (SyncKeyUtils.getEnableSyncdKeyPersistenceOnlyAfterServerAck(abPropsService)) {
            coordinator.runWithMonitorReleased(() -> shareKeyWithCompanionDevices(rotatedKey));
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "key share ack received, storing keyId={0}", SyncKeyUtils.syncKeyIdToHex(rotatedKey));
            whatsapp.store().syncStore().addWebAppStateKeys(List.of(rotatedKey));
        } else {
            whatsapp.store().syncStore().addWebAppStateKeys(List.of(rotatedKey));
            coordinator.runWithMonitorReleased(() -> shareKeyWithCompanionDevices(rotatedKey));
        }

        if (expired) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "key rotation triggered by expiry");
            wamService.commit(new MdAppStateKeyRotationEventBuilder()
                    .mdAppStateKeyRotationReason(MdAppStateKeyRotationReasonCode.APP_STATE_SYNC_KEY_EXPIRY)
                    .build());
        }
        if (deviceRemoved) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "key rotation triggered by device removal");
            wamService.commit(new MdAppStateKeyRotationEventBuilder()
                    .mdAppStateKeyRotationReason(MdAppStateKeyRotationReasonCode.DEVICE_DEREGISTERATION)
                    .build());
        }

        return rotatedKey;
    }

    /**
     * {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdKeyManagement",
            exports = "getNewestKeyPair",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public AppStateSyncKey getNewestKeyPair() {
        return SyncKeyUtils.findNewestKey(whatsapp.store().syncStore().appStateKeys());
    }

    /**
     * Returns whether the supplied key has aged past the configured maximum use threshold.
     *
     * <p>The threshold is read from {@code syncd_key_max_use_days} and clamped between
     * {@link #MIN_KEY_MAX_USE_DAYS} and {@link #MAX_KEY_MAX_USE_DAYS} so a misconfigured AB-prop
     * value can neither disable rotation entirely nor trigger it on every call. A key carrying
     * no timestamp is treated as not expired.
     *
     * @implNote This implementation returns {@code false} when the key carries no timestamp
     * because that protobuf field is optional.
     *
     * @param key the key to inspect
     * @return {@code true} when the key is past its maximum-use age
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRotateKey",
            exports = "hasKeyExpired",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean hasKeyExpired(AppStateSyncKey key) {
        var timestamp = key.keyData()
                .flatMap(AppStateSyncKeyData::timestamp)
                .orElse(null);
        if (timestamp == null) {
            return false;
        }

        var maxDays = Math.min(MAX_KEY_MAX_USE_DAYS,
                Math.max(MIN_KEY_MAX_USE_DAYS,
                        SyncKeyUtils.getSyncdKeyMaxUseDays(abPropsService)));
        var maxAge = Duration.ofDays(maxDays);
        var age = Duration.between(timestamp, Instant.now());
        return age.compareTo(maxAge) > 0;
    }

    /**
     * Returns whether a companion device has been removed since the supplied key was created.
     *
     * <p>Compares the key's recorded device fingerprint against the live device list; a removal
     * is one of the two rotation triggers, alongside expiry, and exists to cut a removed
     * device's access to future mutations. A key without a fingerprint, or a {@code null} live
     * fingerprint, yields {@code false}. A raw-id mismatch counts as a removal; otherwise only a
     * true device-index set mismatch counts.
     *
     * @implNote This implementation expands the key's stored device indexes from
     * {@code currentIndex + 1} up to the live current index so devices added legitimately since
     * the key was minted do not register as removals.
     *
     * @param key the key whose fingerprint to compare
     * @param currentFingerprint the current device fingerprint, possibly {@code null}
     * @return {@code true} when at least one device has been removed since the key was minted
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRotateKey",
            exports = "hasADeviceBeenRemoved",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean hasADeviceBeenRemoved(AppStateSyncKey key, DeviceFingerprint currentFingerprint) {
        var fingerprint = key.keyData()
                .flatMap(AppStateSyncKeyData::fingerprint)
                .orElse(null);
        if (fingerprint == null) {
            return false;
        }

        if (currentFingerprint == null) {
            return false;
        }

        var keyRawId = fingerprint.rawId().orElse(-1);
        if (keyRawId != currentFingerprint.rawId) {
            return true;
        }

        var keyDeviceIndexes = new HashSet<>(fingerprint.deviceIndexes());
        var keyCurrentIndex = fingerprint.currentIndex().orElse(0);
        for (var i = keyCurrentIndex + 1; i <= currentFingerprint.currentIndex; i++) {
            keyDeviceIndexes.add(i);
        }

        return !keyDeviceIndexes.equals(currentFingerprint.deviceIndexes);
    }

    /**
     * Computes the current device fingerprint snapshot used by the rotation triggers.
     *
     * <p>Returns {@code null} when the own JID is unavailable, the device list has not been synced,
     * or the device list carries no usable numeric {@code rawId}; all three cases make both
     * rotation triggers degrade to a no-op rather than forcing a rotation against an unknown peer
     * set.
     *
     * @implNote This implementation parses the device-list {@code rawId} as a uint32 (via
     * {@code (int) Long.parseLong}, matching the {@code UINT32} wire type of the
     * {@code AppStateSyncKeyFingerprint.rawId} field it is compared against) because Cobalt's
     * device-list model holds it as a string. A {@code null} or non-numeric value yields a
     * {@code null} fingerprint so the rotation triggers cannot fire against a corrupted rawId.
     *
     * @return the current {@link DeviceFingerprint} snapshot, or {@code null} when unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdKeyCallbacksApi",
            exports = "getDeviceFingerprint",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebApiDeviceList",
            exports = "getMyDeviceList",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private DeviceFingerprint getCurrentDeviceFingerprint() {
        var myJid = whatsapp.store().accountStore().jid().orElse(null);
        if (myJid == null) {
            return null;
        }

        var deviceListOpt = whatsapp.store().contactStore().findDeviceList(myJid.toUserJid());
        if (deviceListOpt.isEmpty()) {
            return null;
        }

        var deviceList = deviceListOpt.get();
        var rawId = deviceList.rawId();
        if (rawId == null) {
            return null;
        }
        int rawIdInt;
        try {
            rawIdInt = (int) Long.parseLong(rawId);
        } catch (NumberFormatException _) {
            // The device-list rawId is the ADV signed-key-index rawId, a uint32 numeric value
            // that must match the rawId embedded in a sync key's fingerprint. Some notification
            // paths historically stored a non-numeric device-list dhash here instead; when the
            // value is not a real numeric rawId the fingerprint cannot be compared, so report it
            // as unavailable. Both rotation triggers then degrade to a no-op (rather than firing
            // a spurious device-removed rotation on every push), which is the safe default.
            return null;
        }
        var currentIndex = deviceList.currentIndex();

        var deviceIndexes = new HashSet<Integer>();
        for (var device : deviceList.devices()) {
            deviceIndexes.add(device.keyIndex());
        }

        return new DeviceFingerprint(currentIndex, deviceIndexes, rawIdInt);
    }

    /**
     * Mints a new sync key with an incremented epoch, fresh random key data, and the supplied
     * device fingerprint.
     *
     * <p>Returns {@code null} when any input needed to derive the new key id is absent (the
     * previous key id, the own JID, or the current fingerprint), which translates upstream into
     * no rotation this round. The new key id encodes the own device id and the next epoch, and
     * the new key data is a fresh 32-byte random payload.
     *
     * @implNote This implementation builds the 6-byte key id via
     * {@link SyncKeyUtils#buildKeyId(int, int)} from a 2-byte device id and a 4-byte epoch, and
     * seeds the 32-byte payload from {@link SecureRandom}.
     *
     * @param currentFingerprint the device fingerprint to attach to the new key
     * @param previousKey the key being rotated out; its epoch seeds the new epoch
     * @return the newly minted {@link AppStateSyncKey}, or {@code null} when rotation is impossible
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdRotateKey",
            exports = "rotateKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private AppStateSyncKey rotateKey(DeviceFingerprint currentFingerprint, AppStateSyncKey previousKey) {
        var previousKeyIdBytes = previousKey.keyId()
                .flatMap(AppStateSyncKeyId::keyId)
                .orElse(null);
        if (previousKeyIdBytes == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot rotate key: previous key has no id");
            return null;
        }

        var myJid = whatsapp.store().accountStore().jid().orElse(null);
        if (myJid == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot rotate key: own jid not available");
            return null;
        }

        if (currentFingerprint == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot rotate key: device fingerprint not available");
            return null;
        }

        var newEpoch = SyncKeyUtils.generateNewKeyEpoch(previousKeyIdBytes);
        var myDeviceId = myJid.device();
        var newKeyId = SyncKeyUtils.buildKeyId(myDeviceId, newEpoch);

        var newKeyData = new byte[32];
        SECURE_RANDOM.nextBytes(newKeyData);

        var fingerprint = new AppStateSyncKeyFingerprintBuilder()
                .rawId(currentFingerprint.rawId)
                .currentIndex(currentFingerprint.currentIndex)
                .deviceIndexes(new ArrayList<>(currentFingerprint.deviceIndexes))
                .build();

        var keyData = new AppStateSyncKeyDataBuilder()
                .keyData(newKeyData)
                .fingerprint(fingerprint)
                .timestamp(Instant.now())
                .build();

        var keyIdProto = new AppStateSyncKeyIdBuilder()
                .keyId(newKeyId)
                .build();

        return new AppStateSyncKeyBuilder()
                .keyId(keyIdProto)
                .keyData(keyData)
                .build();
    }

    /**
     * Sends the rotated key to every companion device under the key-rotation reason.
     *
     * <p>Called by {@link #getActiveKeyInternal(boolean)} after a rotation has been decided; the
     * share lets every peer encrypt outgoing mutations with the new key id and decrypt incoming
     * ones from the rotating device.
     *
     * @param key the newly minted key to share
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdKeyCallbacksApi",
            exports = "sendSyncdKeyRotation",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void shareKeyWithCompanionDevices(AppStateSyncKey key) {
        var companionDevices = getCompanionDevices();
        sendAppStateSyncKeyShare(List.of(key), List.of(), companionDevices, "key_rotation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMissingKeyShare(List<AppStateSyncKey> keys, List<byte[]> orphanKeyIds, Jid peerDeviceJid) {
        sendAppStateSyncKeyShare(keys, orphanKeyIds, List.of(peerDeviceJid), "missing_key");
    }

    /**
     * Builds an {@code AppStateSyncKeyShare} {@link ProtocolMessage} and dispatches it to every
     * target device.
     *
     * <p>Shared backend for both {@link #shareKeyWithCompanionDevices(AppStateSyncKey)} (the
     * rotation broadcast) and {@link #sendMissingKeyShare(List, List, Jid)} (the missing-key
     * response). Orphan key ids are appended as data-less key entries so the requester can mark
     * them missing. The {@code reason} string is used in diagnostic logs only and is not encoded
     * on the wire. An empty target list, or an unavailable own JID, short-circuits the dispatch.
     *
     * @implNote This implementation iterates the per-device sends sequentially and swallows
     * individual failures with a warning log, so a single misbehaving peer cannot poison the
     * rest of the broadcast.
     *
     * @param keys the keys to share
     * @param orphanKeyIds the key ids without data, sent so the requester can mark them missing
     * @param targetDevices the JIDs to deliver the message to
     * @param reason the human-readable reason label used in diagnostic logs only
     */
    private void sendAppStateSyncKeyShare(List<AppStateSyncKey> keys, List<byte[]> orphanKeyIds, List<Jid> targetDevices, String reason) {
        if (targetDevices.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no target devices to share keys with");
            return;
        }

        var myJid = whatsapp.store().accountStore().jid().orElse(null);
        if (myJid == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot send key share: own jid not available");
            return;
        }

        var shareKeys = new ArrayList<AppStateSyncKey>(keys.size() + orphanKeyIds.size());
        shareKeys.addAll(keys);
        for (var orphanKeyId : orphanKeyIds) {
            var orphanKey = new AppStateSyncKeyBuilder()
                    .keyId(new AppStateSyncKeyIdBuilder().keyId(orphanKeyId).build())
                    .build();
            shareKeys.add(orphanKey);
        }
        var keyShare = new AppStateSyncKeyShareBuilder()
                .keys(shareKeys)
                .build();

        var protocolMessage = new ProtocolMessageBuilder()
                .type(ProtocolMessage.Type.APP_STATE_SYNC_KEY_SHARE)
                .appStateSyncKeyShare(keyShare)
                .build();

        var messageContainer = new LinkedMessageContainerBuilder()
                .protocolMessage(protocolMessage)
                .build();

        var messages = new ArrayList<Map.Entry<Jid, ChatMessageInfo>>(targetDevices.size());
        for (var device : targetDevices) {
            var messageKey = new MessageKeyBuilder()
                    .id(MessageIdGenerator.generate(MessageIdVersion.V1, myJid))
                    .parentJid(myJid)
                    .fromMe(true)
                    .build();
            var messageInfo = new ChatMessageInfoBuilder()
                    .key(messageKey)
                    .message(messageContainer)
                    .build();
            messages.add(Map.entry(device, messageInfo));
        }

        var keyIdHexList = keys.stream()
                .map(SyncKeyUtils::syncKeyIdToHex)
                .toList();
        var deviceIdList = targetDevices.stream()
                .map(Jid::device)
                .toList();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "send key share: keyIds={0} deviceIds={1} reason={2}",
                keyIdHexList, deviceIdList, reason);

        for (var entry : messages) {
            try {
                whatsapp.sendPeerMessage(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to send key share to device {0}: {1}", entry.getKey(), e.getMessage());
            }
        }
    }

    /**
     * Returns every companion device {@link Jid} this client may share or request keys with.
     *
     * <p>The result is the registered own-device list with the current device filtered out. An
     * unavailable own JID or a missing device list yields an empty list, which short-circuits
     * the dispatch path. A device-list lookup failure falls back to a singleton primary device
     * (device id {@code 0}) so a degraded device-list state still permits a rotation share to
     * land somewhere.
     *
     * @return the companion device {@link Jid}s, or a singleton primary device on lookup failure
     */
    private List<Jid> getCompanionDevices() {
        var myJid = whatsapp.store().accountStore().jid().orElse(null);
        if (myJid == null) {
            return List.of();
        }

        try {
            var myDeviceList = whatsapp.store().contactStore().findDeviceList(myJid.toUserJid());
            if (myDeviceList.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "getCompanionDevices: no device list available");
                return List.of();
            }

            return myDeviceList.get()
                    .devices()
                    .stream()
                    .filter(device -> device.id() != myJid.device())
                    .map(device -> device.toDeviceJid(myJid.user(), myJid.server()))
                    .toList();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "getPeerDevices: device list lookup failed, falling back to primary device only", e);
            return List.of(Jid.of(myJid.user(), myJid.server(), 0, 0));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startPeriodicRotationJob() {
        stopPeriodicRotationJob();
        periodicRotationJob = ScheduledTask.schedule(PERIODIC_ROTATION_INTERVAL, this::runPeriodicRotation);
    }

    /**
     * Runs one periodic rotation check, swallowing any failure so the recurrence survives it.
     *
     * <p>The body of the recurring rotation tick: re-derives the active key, forcing a rotation when
     * the device fingerprint demands one. A failure is logged and dropped so a single bad tick does
     * not halt the {@link #PERIODIC_ROTATION_INTERVAL} loop, which the scheduler keeps running until
     * {@link #stopPeriodicRotationJob()} cancels it.
     */
    private void runPeriodicRotation() {
        try {
            getActiveKey(true);
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "periodic key rotation check failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopPeriodicRotationJob() {
        var job = periodicRotationJob;
        if (job != null) {
            job.cancel();
            periodicRotationJob = null;
        }
    }

    /**
     * Carries a snapshot of the current device fingerprint used by both rotation triggers.
     *
     * <p>Holds only the fields
     * {@link #hasADeviceBeenRemoved(AppStateSyncKey, DeviceFingerprint)} needs to compare against
     * the per-key stored fingerprint.
     *
     * @param currentIndex the live device-list current-index counter
     * @param deviceIndexes the live set of active device key indexes
     * @param rawId the live device-list raw id, parsed as uint32 bits
     */
    private record DeviceFingerprint(int currentIndex, Set<Integer> deviceIndexes, int rawId) {
    }
}
