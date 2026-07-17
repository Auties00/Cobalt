package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastCampaignBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintains the business-broadcast-campaign roster from {@code business_broadcast_campaign} sync mutations.
 *
 * <p>A campaign groups a marketing message, a target broadcast list, an ad
 * association, and a scheduled or pending status. When a campaign is created,
 * edited, or deleted on another device, the server replays the change here as a
 * {@link SyncdOperation#SET} (upsert) or {@link SyncdOperation#REMOVE}; the
 * result is read back through
 * {@link LinkedWhatsAppBusinessStore#findBusinessBroadcastCampaign(String)}.
 *
 * @implNote
 * This implementation drops two WA Web side effects: the
 * {@code isBizBroadcastSendWebEnabledNoExposure()} AB-prop gate that
 * short-circuits the entire batch (Cobalt does not gate on AB-prop
 * exposure here) and the post-batch
 * {@code refreshBroadcastCampaignState} fire-and-forget event (Cobalt
 * has no browser frontend bridge). The malformed-mutation count is
 * still logged from {@link #applyMutationBatch(LinkedWhatsAppClient, List)}
 * to match WA Web's per-batch warning.
 */
@WhatsAppWebModule(moduleName = "WAWebBroadcastCampaignSync")
public final class BusinessBroadcastCampaignHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link BusinessBroadcastCampaignHandler}.
     */
    private static final System.Logger LOGGER = Log.get(BusinessBroadcastCampaignHandler.class);

    /**
     * Constructs the singleton broadcast-campaign handler.
     *
     * <p>The sync handler registry instantiates this type exactly once.
     */
    @WhatsAppWebExport(moduleName = "WAWebBroadcastCampaignSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public BusinessBroadcastCampaignHandler() {

    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastCampaignSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return BusinessBroadcastCampaignAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastCampaignSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return BusinessBroadcastCampaignAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastCampaignSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return BusinessBroadcastCampaignAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For {@link SyncdOperation#SET} mutations, validates that the
     * {@link BusinessBroadcastCampaignAction} value carries a
     * {@code broadcastJid}, {@code deviceId}, and {@code status}, then upserts
     * the campaign keyed by the {@code campaignId} in index slot 1. For
     * {@link SyncdOperation#REMOVE} mutations, drops the campaign by id. Returns
     * {@link SyncdIndexUtils#malformedActionIndex(String, String)} when the
     * index slot is empty, {@link SyncdIndexUtils#malformedActionValue(String)}
     * when the required fields are missing, and
     * {@link MutationApplicationResult#failed()} for unknown operations or any
     * thrown exception.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastCampaignSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        try {
            var indexArray = JSON.parseArray(mutation.index());
            if (indexArray.size() <= 1) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast campaign mutation malformed: index size={0}", indexArray.size());
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }
            var campaignId = indexArray.getString(1);
            if (campaignId == null || campaignId.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast campaign mutation malformed: missing campaign id");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (mutation.operation() == SyncdOperation.SET) {
                if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof BusinessBroadcastCampaignAction action)) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast campaign mutation malformed: missing action value campaign={0}", campaignId);
                    return SyncdIndexUtils.malformedActionValue(collectionName().name());
                }

                if (action.broadcastJid().isEmpty() || action.deviceId().isEmpty() || action.status().isEmpty()) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast campaign mutation malformed: missing required fields campaign={0}", campaignId);
                    return SyncdIndexUtils.malformedActionValue(collectionName().name());
                }

                client.store().businessStore().putBusinessBroadcastCampaign(new BusinessBroadcastCampaignBuilder()
                        .id(campaignId)
                        .deviceId(action.deviceId().isPresent() ? action.deviceId().getAsInt() : null)
                        .adId(action.adId().orElse(null))
                        .name(action.name().orElse(null))
                        .marketingMessageId(action.msgId().orElse(null))
                        .broadcastJid(action.broadcastJid().map(Jid::of).orElse(null))
                        .reservedQuota(action.reservedQuota().isPresent() ? action.reservedQuota().getAsInt() : null)
                        .scheduledAt(action.scheduledTimestamp().isPresent() ? Instant.ofEpochMilli(action.scheduledTimestamp().getAsLong()) : null)
                        .createdAt(action.createTimestamp().isPresent() ? Instant.ofEpochMilli(action.createTimestamp().getAsLong()) : null)
                        .status(action.status().orElse(null))
                        .build());
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "broadcast campaign upserted: id={0} status={1}", campaignId, action.status().orElse(null));
                return MutationApplicationResult.success();
            }

            if (mutation.operation() == SyncdOperation.REMOVE) {
                client.store().businessStore().removeBusinessBroadcastCampaign(campaignId);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "broadcast campaign removed: id={0}", campaignId);
                return MutationApplicationResult.success();
            }

            if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast campaign mutation failed: unexpected operation={0}", mutation.operation());
            return MutationApplicationResult.failed();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast campaign mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates the batch, applying each mutation via
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} and
     * aggregating a malformed-mutation count for the warning log.
     *
     * @implNote
     * This implementation omits WA Web's
     * {@code isBizBroadcastSendWebEnabledNoExposure()} short-circuit
     * (Cobalt does not gate on AB-prop exposure here) and the
     * post-batch {@code refreshBroadcastCampaignState} fire-and-forget
     * event because Cobalt has no browser frontend bridge.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBroadcastCampaignSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        var malformedCount = 0;
        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var mutation : mutations) {
            var result = applyMutation(client, mutation);
            if (result.actionState() == SyncActionState.MALFORMED) {
                malformedCount++;
            }
            results.add(result);
        }
        if (malformedCount > 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "broadcast campaign sync: {0} malformed mutations", malformedCount);
        }
        return results;
    }

}
