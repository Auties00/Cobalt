package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.AndroidUnsupportedActions;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Tracks the primary device's permission for companions to ship mutations the primary cannot interpret.
 *
 * <p>A WA Android primary device running an older release that does not
 * understand a newer mutation type can still grant companions permission to
 * author such mutations rather than have the server gate them out. This
 * handler reads the {@link AndroidUnsupportedActions#allowed()} flag from each
 * {@code android_unsupported_actions} mutation and, when it is {@code true},
 * latches the
 * {@link LinkedWhatsAppSyncStore#primaryAllowsAllMutations()}
 * flag so the rest of Cobalt's sync engine knows it is safe to author action
 * types the primary may not yet understand.
 *
 * @implNote
 * This implementation latches the flag once on the first observed
 * {@code allowed = true}; further mutations that would re-set it are
 * no-ops, matching WA Web's
 * {@code getPrimaryAllowsAllMutations()}-gated update.
 */
@WhatsAppWebModule(moduleName = "WAWebAndroidUnsupportedActionsSync")
public final class AndroidUnsupportedActionsHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link AndroidUnsupportedActionsHandler}.
     */
    private static final System.Logger LOGGER = Log.get(AndroidUnsupportedActionsHandler.class);

    /**
     * Constructs the singleton android-unsupported-actions handler.
     *
     * <p>The sync handler registry instantiates this type exactly once.
     */
    @WhatsAppWebExport(moduleName = "WAWebAndroidUnsupportedActionsSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public AndroidUnsupportedActionsHandler() {

    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebAndroidUnsupportedActionsSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return AndroidUnsupportedActions.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebAndroidUnsupportedActionsSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return AndroidUnsupportedActions.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebAndroidUnsupportedActionsSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return AndroidUnsupportedActions.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the {@link AndroidUnsupportedActions#allowed()} flag from the
     * mutation value and, when it is {@code true}, calls
     * {@link #updatePrimaryAllowsAllMutationsFlag(LinkedWhatsAppClient)}. Non-{@link SyncdOperation#SET}
     * operations are reported as {@link MutationApplicationResult#unsupported()};
     * a missing or mistyped action payload is reported as malformed via
     * {@link SyncdIndexUtils#malformedActionValue(String)}; any thrown exception
     * is reported as {@link MutationApplicationResult#failed()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebAndroidUnsupportedActionsSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            if (mutation.operation() != SyncdOperation.SET) {
                return MutationApplicationResult.unsupported();
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof AndroidUnsupportedActions action)) {
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            if (action.allowed()) {
                updatePrimaryAllowsAllMutationsFlag(client);
            }

            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "android unsupported actions mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * Latches the {@code primaryAllowsAllMutations} store flag if it is not already set.
     *
     * <p>Invoked from {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)}
     * when the incoming mutation's {@code allowed} flag is {@code true}.
     *
     * @implNote
     * This implementation reads the current value first and only writes
     * (and emits a log line) when the flag transitions from unset to
     * set, mirroring WA Web's
     * {@code getPrimaryAllowsAllMutations() || setPrimaryAllowsAllMutations()}
     * guard.
     *
     * @param client the {@link LinkedWhatsAppClient} whose store hosts the flag
     */
    @WhatsAppWebExport(moduleName = "WAWebAndroidUnsupportedActionsSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private void updatePrimaryAllowsAllMutationsFlag(LinkedWhatsAppClient client) {
        if (!client.store().syncStore().primaryAllowsAllMutations()) {
            if (Log.INFO) LOGGER.log(Level.INFO, "primary allows all mutations flag set");
            client.store().syncStore().setPrimaryAllowsAllMutations(true);
        }
    }
}
