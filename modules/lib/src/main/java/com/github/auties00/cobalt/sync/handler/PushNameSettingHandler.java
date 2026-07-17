package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedNameChangedListener;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.PushNameSetting;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wire.wam.event.MdBootstrapAppStateCriticalDataProcessingEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.BootstrapAppStateDataStageCode;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code setting_pushName} app-state action that distributes the
 * user's broadcast pushname across linked devices.
 *
 * <p>The pushname is the outwards-facing name the server attaches to the
 * user's outgoing presences and to message envelopes shown to peers. Each
 * {@link SyncdOperation#SET} mutation broadcasts a fresh
 * {@code <presence name="..."/>} stanza, persists the new name to
 * {@link LinkedWhatsAppAccountStore#setName(String)},
 * mirrors it onto the self-contact's {@code chosenName}, and fires
 * {@link LinkedWhatsAppClientListener#onNameChanged(LinkedWhatsAppClient, String, String)}
 * on every registered listener via virtual threads. The mutation index is the
 * singleton {@snippet :
 *     ["setting_pushName"]
 * }
 *
 * @implNote
 * This implementation collapses WA Web's two preference-side writes
 * ({@code WAWebConnModel.Conn.pushname} and
 * {@code WAWebUserPrefsGeneral.setPushname}) into a single
 * {@code LinkedWhatsAppStore.setName} call: Cobalt's store is the sole source of
 * truth for the broadcast pushname. The WA Web {@code syncdCritical}
 * coordination is omitted because Cobalt tracks bootstrap state at the
 * {@link com.github.auties00.cobalt.sync.WebAppStateService} layer keyed by
 * collection. The listener-fanout and self-contact mirror are Cobalt-only
 * because Cobalt has no equivalent of WA Web's observable propagation; direct
 * listener notification is the Cobalt analogue.
 */
@WhatsAppWebModule(moduleName = "WAWebPushNameSync")
public final class PushNameSettingHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link PushNameSettingHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PushNameSettingHandler.class);

    /**
     * Holds the WAM telemetry service used to commit critical-bootstrap stage
     * events when a mutation is applied during the initial sync.
     */
    private final WamService wamService;

    /**
     * Constructs the push-name sync handler bound to the given WAM telemetry
     * service.
     *
     * @param wamService the WAM telemetry service used by this handler
     */
    @WhatsAppWebExport(moduleName = "WAWebPushNameSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public PushNameSettingHandler(WamService wamService) {
        this.wamService = wamService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPushNameSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return PushNameSetting.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPushNameSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return PushNameSetting.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPushNameSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return PushNameSetting.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation walks WA Web's per-mutation arm in
     * {@code WAWebPushNameSync.applyMutations}: only {@link SyncdOperation#SET}
     * is accepted; the resolved pushname is read via the optional
     * {@link PushNameSetting#name()} accessor, with a missing or empty value
     * defaulting to the empty string and triggering a
     * {@link BootstrapAppStateDataStageCode#PUSHNAME_INVALID} WAM stage
     * emission; a {@code <presence name="..."/>} stanza is dispatched via
     * {@link LinkedWhatsAppClient#sendNodeWithNoResponse(Stanza)}
     * with no {@code type} attribute; the new name is persisted via
     * {@code LinkedWhatsAppStore.setName}; the self-contact's
     * {@link com.github.auties00.cobalt.wire.linked.contact.Contact#setChosenName(String)}
     * is updated when present;
     * {@link LinkedWhatsAppClientListener#onNameChanged(LinkedWhatsAppClient, String, String)}
     * is dispatched on every registered listener via a fresh virtual thread
     * per listener; and a
     * {@link BootstrapAppStateDataStageCode#PUSHNAME_APPLIED} WAM stage is
     * emitted on success. The WA Web {@code syncdCritical} coordination and the
     * trace logging are dropped. WA Web's outer try/catch is not mirrored:
     * Cobalt lets exceptions propagate so the configured
     * {@link WhatsAppLinkedClientErrorHandler}
     * decides recovery.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPushNameSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        var resolvedName = mutation.value().flatMap(sav -> sav.action())
                .filter(PushNameSetting.class::isInstance)
                .map(PushNameSetting.class::cast)
                .flatMap(PushNameSetting::name)
                .orElse(null);
        String name;
        if (resolvedName == null || resolvedName.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "pushname mutation: resolved name missing or empty");
            logCriticalBootstrapStageIfNecessary(client, BootstrapAppStateDataStageCode.PUSHNAME_INVALID);
            name = "";
        } else {
            name = resolvedName;
        }

        client.sendNodeWithNoResponse(new StanzaBuilder()
                .description("presence")
                .attribute("name", name)
                .build());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pushname: sent presence stanza with updated name, length={0}", name.length());

        var oldName = client.store().accountStore().name().orElse(null);
        client.store().accountStore().setName(name);

        client.store().accountStore().jid()
                .flatMap(self -> client.store().contactStore().findContactByJid(self.withoutData()))
                .ifPresent(contact -> contact.setChosenName(name));

        for (var listener : client.store().listeners()) {
            if (listener instanceof LinkedNameChangedListener typed) {
                Thread.startVirtualThread(() -> typed.onNameChanged(client, oldName, name));
            }
        }

        logCriticalBootstrapStageIfNecessary(client, BootstrapAppStateDataStageCode.PUSHNAME_APPLIED);

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pushname: updated successfully");
        return MutationApplicationResult.success();
    }

    /**
     * Emits an
     * {@link com.github.auties00.cobalt.wire.wam.event.MdBootstrapAppStateCriticalDataProcessingEvent}
     * for the given bootstrap stage when the critical data sync is still in
     * progress.
     *
     * @implNote
     * This implementation gates the WAM emission on the
     * {@link SyncPatchType#CRITICAL_BLOCK} app state's {@code bootstrapped}
     * flag; WA Web instead consults the global
     * {@code WAWebSyncBootstrap.isSyncDCriticalDataSyncInProcess()} state
     * machine. Cobalt approximates the same gate by checking whether the
     * critical-block collection has completed its initial sync: once
     * bootstrapped, no further stage events are emitted.
     *
     * @param client the {@link LinkedWhatsAppClient} whose store is queried for
     *               bootstrap state
     * @param stage  the bootstrap stage reached
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCriticalBootstrapProcessingApi", exports = "logCriticalBootstrapStageIfNecessary", adaptation = WhatsAppAdaptation.ADAPTED)
    private void logCriticalBootstrapStageIfNecessary(LinkedWhatsAppClient client, BootstrapAppStateDataStageCode stage) {
        if (client.store().syncStore().findWebAppState(SyncPatchType.CRITICAL_BLOCK).bootstrapped()) {
            return;
        }
        this.wamService.commit(new MdBootstrapAppStateCriticalDataProcessingEventBuilder()
                .bootstrapAppStateDataStage(stage)
                .mdTimestamp((int) System.currentTimeMillis())
                .build());
    }
}
