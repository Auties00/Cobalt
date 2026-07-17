package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingDisableLinkPreviewsAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the {@code setting_disableLinkPreviews} app-state sync action that
 * toggles whether outgoing chat messages render link previews.
 *
 * <p>The action carries a single boolean fanned out across the
 * {@link SyncPatchType#REGULAR} collection so every linked device shares the
 * same "Disable link previews" privacy setting. The mutation index has no
 * variable parts and is always
 * {@snippet :
 *     ["setting_disableLinkPreviews"]
 * }
 */
@WhatsAppWebModule(moduleName = "WAWebDisableLinkPreviewsSync")
public final class DisableLinkPreviewsHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link DisableLinkPreviewsHandler}.
     */
    private static final System.Logger LOGGER = Log.get(DisableLinkPreviewsHandler.class);

    /**
     * Constructs a new singleton {@link DisableLinkPreviewsHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebDisableLinkPreviewsSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public DisableLinkPreviewsHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDisableLinkPreviewsSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return PrivacySettingDisableLinkPreviewsAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDisableLinkPreviewsSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return PrivacySettingDisableLinkPreviewsAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDisableLinkPreviewsSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return PrivacySettingDisableLinkPreviewsAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Walks the batch once classifying each mutation, tracks the last valid
     * {@link PrivacySettingDisableLinkPreviewsAction#isPreviewsDisabled()} flag
     * carried by a {@link SyncdOperation#SET}, and persists that single value
     * via {@link LinkedWhatsAppSettingsStore#setDisableLinkPreviews(boolean)}
     * after the loop. Non-{@code SET} operations yield
     * {@link MutationApplicationResult#unsupported()} and entries whose action
     * payload is not a {@link PrivacySettingDisableLinkPreviewsAction} yield
     * {@link MutationApplicationResult#malformed()}.
     *
     * @implNote
     * This implementation folds the latest valid value and persists once after
     * the loop rather than writing on every {@link SyncdOperation#SET}, because
     * the boolean is global and only its final value is observable.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDisableLinkPreviewsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        if (mutations.isEmpty()) {
            return List.of();
        }

        Boolean lastValid = null;
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            if (mutation.operation() != SyncdOperation.SET) {
                results.add(MutationApplicationResult.unsupported());
                continue;
            }

            if (mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PrivacySettingDisableLinkPreviewsAction action) {
                lastValid = action.isPreviewsDisabled();
                results.add(MutationApplicationResult.success());
            } else {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "disable link previews: malformed mutation in batch");
                results.add(MutationApplicationResult.malformed());
            }
        }

        if (lastValid != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "disable link previews: batch set disabled={0}", lastValid);
            client.store().settingsStore().setDisableLinkPreviews(lastValid);
        }

        return results;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rejects non-{@link SyncdOperation#SET} operations as
     * {@link MutationApplicationResult#unsupported()} and action payloads that
     * are not a {@link PrivacySettingDisableLinkPreviewsAction} as
     * {@link MutationApplicationResult#malformed()}, then persists the value
     * through {@link LinkedWhatsAppSettingsStore#setDisableLinkPreviews(boolean)}.
     *
     * @implNote
     * This implementation persists the value eagerly rather than folding it,
     * because a single mutation has no later value to supersede it.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDisableLinkPreviewsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PrivacySettingDisableLinkPreviewsAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "disable link previews: mutation value is not a PrivacySettingDisableLinkPreviewsAction");
            return MutationApplicationResult.malformed();
        }

        client.store().settingsStore().setDisableLinkPreviews(action.isPreviewsDisabled());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "disable link previews: set disabled={0}", action.isPreviewsDisabled());
        return MutationApplicationResult.success();
    }

}
