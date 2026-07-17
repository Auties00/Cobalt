package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastInsightBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastInsightsAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintains per-campaign delivery statistics from {@code business_broadcast_insights_sync} sync mutations.
 *
 * <p>The analytics surface tracks recipient, delivered, read, replied, and
 * quick-reply counts per campaign. When the server publishes updated insights
 * for a campaign, the mutation lands here, and the result is read back through
 * {@link LinkedWhatsAppBusinessStore#findBusinessBroadcastInsight(String)}.
 *
 * @implNote
 * This implementation drops two WA Web side effects: the
 * {@code isBizBroadcastSendWebEnabledNoExposure()} AB-prop gate that
 * short-circuits the entire batch and the post-batch
 * {@code refreshBroadcastCampaignState} fire-and-forget event. The
 * per-batch SET, REMOVE, and malformed counter logging is preserved
 * to mirror WA Web's diagnostics.
 */
@WhatsAppWebModule(moduleName = "WAWebBusinessBroadcastInsightsSync")
public final class BusinessBroadcastInsightsHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link BusinessBroadcastInsightsHandler}.
     */
    private static final System.Logger LOGGER = Log.get(BusinessBroadcastInsightsHandler.class);

    /**
     * Constructs the singleton broadcast-insights handler.
     *
     * <p>The sync handler registry instantiates this type exactly once.
     */
    @WhatsAppWebExport(moduleName = "WAWebBusinessBroadcastInsightsSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public BusinessBroadcastInsightsHandler() {

    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBusinessBroadcastInsightsSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return BusinessBroadcastInsightsAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBusinessBroadcastInsightsSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return BusinessBroadcastInsightsAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBusinessBroadcastInsightsSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return BusinessBroadcastInsightsAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@link SyncdOperation#SET} mutations, upserts a
     * {@link com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastInsight}
     * keyed by the {@code campaignId} in index slot 1 carrying the delivery
     * counters from the action value. For {@link SyncdOperation#REMOVE}
     * mutations, drops the insights record by id. Returns
     * {@link SyncdIndexUtils#malformedActionIndex(String, String)} when the
     * index slot is empty, {@link SyncdIndexUtils#malformedActionValue(String)}
     * when the required value is missing, and
     * {@link MutationApplicationResult#failed()} for unknown operations or any
     * thrown exception.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBusinessBroadcastInsightsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            var indexArray = JSON.parseArray(mutation.index());
            if (indexArray.size() <= 1) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast insights mutation malformed: index size={0}", indexArray.size());
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            var campaignId = indexArray.getString(1);
            if (campaignId == null || campaignId.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast insights mutation malformed: missing campaign id");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (mutation.operation() == SyncdOperation.SET) {
                if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof BusinessBroadcastInsightsAction action)) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast insights mutation malformed: missing action value campaign={0}", campaignId);
                    return SyncdIndexUtils.malformedActionValue(collectionName().name());
                }

                client.store().businessStore().putBusinessBroadcastInsight(new BusinessBroadcastInsightBuilder()
                        .id(campaignId)
                        .recipientCount(action.recipientCount().isPresent() ? action.recipientCount().getAsInt() : null)
                        .deliveredCount(action.deliveredCount().isPresent() ? action.deliveredCount().getAsInt() : null)
                        .readCount(action.readCount().isPresent() ? action.readCount().getAsInt() : null)
                        .repliedCount(action.repliedCount().isPresent() ? action.repliedCount().getAsInt() : null)
                        .quickReplyCount(action.quickReplyCount().isPresent() ? action.quickReplyCount().getAsInt() : null)
                        .build());
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "broadcast insights upserted: campaign={0}", campaignId);
                return MutationApplicationResult.success();
            }

            if (mutation.operation() == SyncdOperation.REMOVE) {
                client.store().businessStore().removeBusinessBroadcastInsight(campaignId);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "broadcast insights removed: campaign={0}", campaignId);
                return MutationApplicationResult.success();
            }

            if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast insights mutation failed: unexpected operation={0}", mutation.operation());
            return MutationApplicationResult.failed();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast insights mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates the batch, applying each mutation via
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} and
     * aggregating the SET, REMOVE, and malformed counters for the per-batch
     * warning log.
     *
     * @implNote
     * This implementation omits WA Web's
     * {@code isBizBroadcastSendWebEnabledNoExposure()} short-circuit
     * and the post-batch {@code refreshBroadcastCampaignState}
     * fire-and-forget event. The malformed counter is incremented only
     * for {@link SyncdOperation#SET} results to mirror WA Web's logic
     * where the {@code malformedActionValue} branch is the only place
     * the counter increments.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBusinessBroadcastInsightsSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        var malformedCount = 0;
        var setCount = 0;
        var removeCount = 0;
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            var result = applyMutation(client, mutation);
            if (result.actionState() == SyncActionState.MALFORMED && mutation.operation() == SyncdOperation.SET) {
                malformedCount++;
            }
            if (result.actionState() == SyncActionState.SUCCESS && mutation.operation() == SyncdOperation.SET) {
                setCount++;
            }
            if (result.actionState() == SyncActionState.SUCCESS && mutation.operation() == SyncdOperation.REMOVE) {
                removeCount++;
            }
            results.add(result);
        }
        if (setCount > 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "BBI SyncD received {0} SET operations", setCount);
        }
        if (removeCount > 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "BBI SyncD received {0} REMOVE operations", removeCount);
        }
        if (malformedCount > 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "BBI sync: {0} malformed mutations", malformedCount);
        }
        return results;
    }
}
