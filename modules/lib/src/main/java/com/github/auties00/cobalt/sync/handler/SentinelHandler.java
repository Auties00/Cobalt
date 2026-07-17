package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.KeyExpirationAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Expires retired app-state-sync keys when the primary device announces a new
 * key epoch via the {@code "sentinel"} mutation.
 *
 * <p>The sync dispatcher hands an incoming sentinel mutation here after the
 * primary device rotates its sync key so the companion drops the matching local
 * app-state-sync key from
 * {@link LinkedWhatsAppStore} and forces a re-keyed
 * patch to be requested on the next sync.
 */
@WhatsAppWebModule(moduleName = "WAWebSentinelMutationSync")
public final class SentinelHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link SentinelHandler}.
     */
    private static final System.Logger LOGGER = Log.get(SentinelHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single
     * instance per client.
     */
    @WhatsAppWebExport(moduleName = "WAWebSentinelMutationSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SentinelHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSentinelMutationSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return KeyExpirationAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSentinelMutationSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return KeyExpirationAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSentinelMutationSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return KeyExpirationAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A non-{@link SyncdOperation#SET} operation is reported as
     * {@link MutationApplicationResult#unsupported()}. A {@code SET} whose
     * decoded action is not a {@link KeyExpirationAction} or whose
     * {@code expiredKeyEpoch} is empty is reported as malformed; otherwise the
     * named epoch is expired on the local store via
     * {@link LinkedWhatsAppSyncStore#expireAppStateKeysByEpoch(int)}.
     *
     * @implNote
     * This implementation omits WA Web's {@code WALogger.ERROR}/{@code WARN}
     * aggregation of the malformed and unsupported counters as telemetry, and
     * drops the outer {@code try/catch} that maps any throw to a sentinel result
     * per Cobalt's error model: thrown exceptions surface to the configured
     * {@link WhatsAppLinkedClientErrorHandler}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSentinelMutationSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.DIRECT)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof KeyExpirationAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sentinel mutation malformed: missing action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var expiredEpoch = action.expiredKeyEpoch();
        if (expiredEpoch.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sentinel mutation malformed: missing expired key epoch");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().syncStore().expireAppStateKeysByEpoch(expiredEpoch.getAsInt());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sentinel: expired app-state keys for epoch={0}", expiredEpoch.getAsInt());
        return MutationApplicationResult.success();
    }

}
