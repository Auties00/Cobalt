package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.PrimaryVersionAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the {@code primaryVersion} app-state action that distributes the
 * primary device's WhatsApp version string across linked devices.
 *
 * <p>The primary device announces its current build and its session-start
 * build to the paired companions so each companion can render version-skew
 * warnings when needed. The handler validates the per-mutation shape but
 * writes nothing to the store: WA Web emits only {@code WARN} telemetry for
 * invalid entries, and Cobalt has no consumer for the version data either. The
 * mutation index keys each entry by the checkpoint name, formatted as
 * {@snippet :
 *     ["primaryVersion", "current"]
 *     ["primaryVersion", "session_start"]
 * }
 *
 * @implNote
 * This implementation overrides
 * {@link #applyMutationBatch(LinkedWhatsAppClient, List)} to mirror WA Web's batch
 * loop shape, but the per-mutation arm delegates to
 * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} because
 * there is no batch-level state to track. WA Web's {@code WARN} batch counters
 * and the missing-version / unknown-sub-index trace messages are dropped.
 */
@WhatsAppWebModule(moduleName = "WAWebPrimaryVersionSync")
public final class PrimaryVersionHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link PrimaryVersionHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PrimaryVersionHandler.class);

    /**
     * Holds the {@code "current"} sub-index value identifying the primary
     * device's current-build version checkpoint.
     *
     * @implNote
     * This implementation matches WA Web's
     * {@code WAWebPrimaryVersionSync.CURRENT = "current"} literal.
     */
    @WhatsAppWebExport(moduleName = "WAWebPrimaryVersionSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private static final String INDEX_CURRENT = "current";

    /**
     * Holds the {@code "session_start"} sub-index value identifying the primary
     * device's session-start version checkpoint.
     *
     * @implNote
     * This implementation matches WA Web's
     * {@code WAWebPrimaryVersionSync.SESSION_START = "session_start"} literal.
     */
    @WhatsAppWebExport(moduleName = "WAWebPrimaryVersionSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private static final String INDEX_SESSION_START = "session_start";

    /**
     * Constructs the singleton primary-version sync handler.
     *
     * @implNote
     * This implementation is stateless; no AB-prop or store dependency is
     * held.
     */
    @WhatsAppWebExport(moduleName = "WAWebPrimaryVersionSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public PrimaryVersionHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryVersionSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return PrimaryVersionAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryVersionSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return PrimaryVersionAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryVersionSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return PrimaryVersionAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation delegates to
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} for
     * every entry; there is no batch-level deduplication or latest-wins
     * tracking. WA Web's per-batch {@code WARN} counters for the unsupported
     * and malformed-value paths are dropped.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryVersionSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.DIRECT)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            results.add(applyMutation(client, mutation));
        }
        return results;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation walks the per-mutation arms of WA Web's
     * {@code WAWebPrimaryVersionSync.applyMutations} in this order: only
     * {@link SyncdOperation#SET} is accepted; {@code indexParts[1]} must equal
     * exactly one of {@link #INDEX_CURRENT} or {@link #INDEX_SESSION_START},
     * with any missing, empty, or unknown value surfacing as
     * {@link SyncdIndexUtils#malformedActionIndex(String, String)}; the
     * mutation value must decode to a {@link PrimaryVersionAction} with a
     * non-empty {@link PrimaryVersionAction#version()}; otherwise it returns
     * {@link MutationApplicationResult#success()}. No store side effect is
     * performed.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebPrimaryVersionSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "primary version mutation malformed: missing sub-index");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var subIndex = indexArray.getString(1);
        if (subIndex == null || subIndex.isEmpty() || (!subIndex.equals(INDEX_CURRENT) && !subIndex.equals(INDEX_SESSION_START))) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "primary version mutation malformed: unknown sub-index {0}", subIndex);
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PrimaryVersionAction action) || action.version().isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "primary version mutation malformed: missing version");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "primary version: validated checkpoint {0}", subIndex);
        return MutationApplicationResult.success();
    }
}
