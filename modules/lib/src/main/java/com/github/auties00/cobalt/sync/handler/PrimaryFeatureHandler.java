package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedWebAppPrimaryFeaturesListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.PrimaryFeatureAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the {@code primary_feature} app-state action that distributes the
 * primary device's advertised feature flag set across linked devices.
 *
 * <p>Every paired device learns the union of feature strings the primary
 * advertises so companion devices can light up or hide UI affordances
 * accordingly. Within a batch only the latest mutation by timestamp is
 * persisted; per-mutation results report the per-entry outcome. The mutation
 * index is the singleton {@snippet :
 *     ["primary_feature"]
 * }
 *
 * <p>An empty {@link PrimaryFeatureAction#flags()} list is treated as success
 * (the only malformed-value branch is a missing action payload).
 *
 * @implNote
 * This implementation overrides
 * {@link #applyMutationBatch(LinkedWhatsAppClient, List)} to implement the
 * latest-wins semantics inside a single store write; the single-mutation
 * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} adapter
 * persists the same mutation immediately for callers that dispatch outside the
 * batch path. WA Web's {@code WARN} batch counters are dropped.
 */
@WhatsAppWebModule(moduleName = "WAWebPrimaryFeatureSync")
public final class PrimaryFeatureHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link PrimaryFeatureHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PrimaryFeatureHandler.class);

    /**
     * Constructs the singleton primary-feature sync handler.
     *
     * @implNote
     * This implementation is stateless; no AB-prop or store dependency is
     * held.
     */
    @WhatsAppWebExport(moduleName = "WAWebPrimaryFeatureSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public PrimaryFeatureHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryFeatureSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return PrimaryFeatureAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryFeatureSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return PrimaryFeatureAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryFeatureSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return PrimaryFeatureAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code WAWebPrimaryFeatureSync.applyMutations}: it walks the batch and
     * for each mutation appends an
     * {@link MutationApplicationResult#unsupported()} (non-{@code SET}), a
     * {@link SyncdIndexUtils#malformedActionValue(String)} (wrong action
     * type), or a {@link MutationApplicationResult#success()} entry; in
     * parallel it tracks the latest valid mutation by timestamp. After the
     * walk, the latest mutation's flags are persisted via
     * {@code LinkedWhatsAppStore.setPrimaryFeatures}; an empty flags list is accepted
     * as success (only a missing action payload triggers malformed).
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryFeatureSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.DIRECT)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        DecryptedMutation.Trusted latest = null;
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            if (mutation.operation() != SyncdOperation.SET) {
                results.add(MutationApplicationResult.unsupported());
                continue;
            }

            var action = mutation.value().flatMap(sav -> sav.action()).orElse(null);
            if (!(action instanceof PrimaryFeatureAction)) {
                if (Log.WARNING)
                    LOGGER.log(Level.WARNING, "primary feature mutation malformed: missing action value");
                results.add(SyncdIndexUtils.malformedActionValue(collectionName().name()));
                continue;
            }

            if (latest == null || mutation.timestamp().compareTo(latest.timestamp()) > 0) {
                latest = mutation;
            }
            results.add(MutationApplicationResult.success());
        }
        if (latest != null) {
            var pfa = (PrimaryFeatureAction) latest.value().flatMap(sav -> sav.action()).orElseThrow();
            client.store().syncStore().setPrimaryFeatures(pfa.flags());
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "primary feature: batch persisted {0} flags", pfa.flags().size());
            notifyPrimaryFeatures(client, pfa.flags());
        }

        return results;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation collapses WA Web's batch loop to the single-mutation
     * case: a non-{@code SET} mutation surfaces as
     * {@link MutationApplicationResult#unsupported()}, a wrong action type as
     * {@link SyncdIndexUtils#malformedActionValue(String)}, and a valid
     * mutation persists its {@link PrimaryFeatureAction#flags()} via
     * {@code LinkedWhatsAppStore.setPrimaryFeatures} and returns
     * {@link MutationApplicationResult#success()}. An empty flags list is
     * accepted as success.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryFeatureSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PrimaryFeatureAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "primary feature mutation malformed: missing action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        client.store().syncStore().setPrimaryFeatures(action.flags());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "primary feature: persisted {0} flags", action.flags().size());
        notifyPrimaryFeatures(client, action.flags());
        return MutationApplicationResult.success();
    }

    /**
     * Fans the newly-persisted primary feature flag set out to every registered
     * {@link LinkedWebAppPrimaryFeaturesListener}.
     *
     * <p>Invoked from both apply paths after
     * {@code LinkedWhatsAppSyncStore.setPrimaryFeatures} commits the latest flag set, so listeners observe
     * the same union of feature strings the local store now holds. An empty flag set is still delivered,
     * since the primary clearing its advertised features is itself an observable change.
     *
     * @implNote
     * This implementation dispatches each listener on its own virtual thread so a slow or throwing
     * listener never blocks the sync pipeline, matching the fan-out used by the other app-state handlers.
     *
     * @param client the {@link LinkedWhatsAppClient} emitting the event
     * @param flags  the primary device's advertised feature flags that were just persisted
     */
    private void notifyPrimaryFeatures(LinkedWhatsAppClient client, List<String> flags) {
        for (var listener : client.store().listeners()) {
            if (listener instanceof LinkedWebAppPrimaryFeaturesListener typed) {
                Thread.startVirtualThread(() -> typed.onWebAppPrimaryFeatures(client, flags));
            }
        }
    }
}
