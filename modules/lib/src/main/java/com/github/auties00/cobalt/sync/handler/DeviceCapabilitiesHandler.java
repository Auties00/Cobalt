package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilities;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilitiesEntry;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilitiesEntryBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wire.wam.event.Lid11MigrationLifecycleEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.MigrationStageEnum;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Applies the {@code device_capabilities} app-state sync action that
 * advertises a device's feature support across the user's linked devices.
 *
 * <p>This handler drives the per-device capability matrix that companions
 * consult before surfacing chat lock, AI threads, business broadcasts and
 * avatars; the primary device emits its capability snapshot through the
 * {@link SyncPatchType#REGULAR_LOW} collection. The mutation index is the
 * device's legacy JID string and only the entry where {@link Jid#device()} is
 * {@code 0} is treated as the primary's payload.
 *
 * @implNote
 * This implementation persists every {@link DeviceCapabilities} payload
 * keyed by the indexed {@link Jid} so the latest snapshot is fast-pathed
 * when an identical primary mutation is replayed; WA Web always re-merges
 * its storage entry. The companion-received-device-capability lifecycle
 * WAM event is only committed when
 * {@link DeviceCapabilities.LIDMigration#chatDbMigrationTimestamp} appears
 * and the local migration state is not yet complete, matching WA Web's
 * guarded emission. The frontend
 * {@code initializeMetaAiBotAiThreads} fire-and-forget, the
 * SMB-only business-broadcast diff against
 * {@code primarySupportsBusinessBroadcast} and the {@code "primary caps
 * updated Nx"} log line are not modelled.
 */
@WhatsAppWebModule(moduleName = "WAWebDeviceCapabilitiesSync")
public final class DeviceCapabilitiesHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link DeviceCapabilitiesHandler}.
     */
    private static final System.Logger LOGGER = Log.get(DeviceCapabilitiesHandler.class);

    /**
     * The legacy device-component value identifying the primary device in
     * the JID string parsed from the mutation index.
     *
     * <p>Gates the primary-capability branch in {@link #applyMutation}; companion
     * devices report a non-zero device component and do not affect the primary
     * capability snapshot.
     */
    @WhatsAppWebExport(moduleName = "WAWebDeviceCapabilitiesSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int PRIMARY_DEVICE = 0;

    /**
     * The position of the JID element inside the parsed mutation
     * {@code indexParts} array.
     */
    @WhatsAppWebExport(moduleName = "WAWebDeviceCapabilitiesSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int JID_INDEX = 1;

    /**
     * The {@link LidMigrationService} consulted whenever a primary
     * {@link DeviceCapabilities.LIDMigration#chatDbMigrationTimestamp}
     * arrives.
     *
     * <p>The handler forwards the timestamp so the local LID 1:1 migration
     * state machine can advance, and reads back
     * {@link LidMigrationService#isLidMigrated()} when emitting the lifecycle
     * WAM event.
     */
    private final LidMigrationService lidMigrationService;

    /**
     * The {@link WamService} used to commit the
     * {@code Lid11MigrationLifecycleWamEvent} carrying the
     * companion-received-device-capability stage.
     */
    private final WamService wamService;

    /**
     * Constructs a {@link DeviceCapabilitiesHandler} bound to the given
     * dependencies.
     *
     * @implNote
     * This implementation surfaces the {@link SyncPatchType#REGULAR_LOW}
     * collection through {@link #collectionName()} rather than holding it as an
     * instance field.
     *
     * @param lidMigrationService the LID 1:1 migration service consulted
     *                            on primary capability arrival
     * @param wamService          the WAM telemetry service used to commit
     *                            lifecycle events
     */
    @WhatsAppWebExport(moduleName = "WAWebDeviceCapabilitiesSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public DeviceCapabilitiesHandler(LidMigrationService lidMigrationService, WamService wamService) {
        this.lidMigrationService = lidMigrationService;
        this.wamService = wamService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeviceCapabilitiesSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return DeviceCapabilities.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeviceCapabilitiesSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return DeviceCapabilities.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeviceCapabilitiesSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return DeviceCapabilities.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation accepts non-{@link SyncdOperation#SET}
     * mutations, mutations with a missing or blank JID component and
     * mutations whose payload is not a {@link DeviceCapabilities} as
     * {@link MutationApplicationResult#success()}, mirroring WA Web which
     * always returns {@code {actionState: Success}} from the loop body.
     * On the primary path, the
     * {@link DeviceCapabilities#userHasAvatar()} flag is published
     * eagerly so subsequent reads can be served without re-decoding the
     * payload, and the
     * {@link DeviceCapabilities.LIDMigration#chatDbMigrationTimestamp}
     * forwarding is preserved verbatim. The
     * {@code checkLidTimeout} backend listener that schedules a delayed
     * logout when the LID 1:1 migration deadline elapses, the SMB-only
     * business-broadcast diff and the
     * {@code initializeMetaAiBotAiThreads} fire-and-forget are not
     * modelled because Cobalt has no equivalent frontend.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeviceCapabilitiesSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.success();
        }

        var indexArray = JSON.parseArray(mutation.index());
        var deviceJidString = indexArray.size() > JID_INDEX ? indexArray.getString(JID_INDEX) : null;
        var capabilities = mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof DeviceCapabilities entry
                ? entry
                : null;
        if (capabilities == null || deviceJidString == null || deviceJidString.isBlank()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "device capabilities: skipping mutation, missing device jid or payload");
            return MutationApplicationResult.success();
        }

        var deviceJid = Jid.of(deviceJidString);
        var previous = client.store().contactStore().findDeviceCapabilitiesEntry(deviceJid)
                .map(DeviceCapabilitiesEntry::capabilities)
                .orElse(null);
        client.store().contactStore().putDeviceCapabilitiesEntry(new DeviceCapabilitiesEntryBuilder().deviceJid(deviceJid).capabilities(capabilities).build());
        if (Objects.equals(previous, capabilities)) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "device capabilities: unchanged for {0}", deviceJid);
            return MutationApplicationResult.success();
        }

        if (deviceJid.device() == PRIMARY_DEVICE) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "device capabilities: primary device capabilities updated {0}", deviceJid);
            client.store().contactStore().setPrimaryDeviceCapabilities(capabilities);
            capabilities.userHasAvatar()
                    .ifPresent(avatar -> client.store().accountStore().setHasAvatar(avatar.userHasAvatar()));
            capabilities.lidMigration()
                    .flatMap(DeviceCapabilities.LIDMigration::chatDbMigrationTimestamp)
                    .ifPresent(timestamp -> {
                        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "device capabilities: observed chat db migration timestamp {0}", timestamp);
                        lidMigrationService.observeChatDbMigrationTimestamp(timestamp);
                        if (!lidMigrationService.isLidMigrated()) {
                            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "device capabilities: committing lid 1:1 migration lifecycle event");
                            this.wamService.commit(new Lid11MigrationLifecycleEventBuilder()
                                    .migrationStage(MigrationStageEnum.COMPANION_RECEIVED_DEVICE_CAPABILITY)
                                    .isLocally1x1MigratedFromDb(lidMigrationService.isLidMigrated())
                                    .build());
                        }
                    });
        }
        return MutationApplicationResult.success();
    }
}
