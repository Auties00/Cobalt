package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.AudienceManagementEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.AudienceEventSurfaceType;
import com.github.auties00.cobalt.wire.wam.type.AudienceManagementActionType;
import com.github.auties00.cobalt.wire.wam.type.AudiencePredicateTypeEnum;
import com.github.auties00.cobalt.wire.wam.type.AudienceResolutionTriggerType;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Builds outgoing app-state mutations that create, update, or delete a Business Broadcast campaign.
 *
 * <p>SMB users schedule a campaign through the Web UI, and the resulting
 * mutations are pushed via
 * {@link com.github.auties00.cobalt.sync.WebAppStateService#pushPatches} so the
 * primary device and other linked devices populate the broadcast-campaign
 * collection consistently. This factory builds the outgoing mutations; the
 * inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.BusinessBroadcastCampaignHandler}.
 *
 * <p>Every campaign mutation this factory builds manages the campaign's
 * audience (the targeted broadcast list), so each build commits an
 * {@link com.github.auties00.cobalt.wire.wam.event.AudienceManagementEvent} (WAM id
 * {@code 7900}) describing the audience action, predicate, resolution trigger,
 * and a JSON descriptor of the affected campaign. WA Web only emits this beacon
 * from its {@code WAWebBizBroadcastAudienceRefreshJob} periodic resolver;
 * Cobalt has no such background job, so the equivalent telemetry is committed at
 * the point each campaign audience is set or removed.
 *
 * @implNote
 * This implementation does not gate on
 * {@code WAWebBizGatingUtils.isBizBroadcastSendWebEnabledNoExposure()}; WA Web
 * only consults that flag on the receive side, so the outgoing factory remains
 * callable from any embedder regardless of the SMB feature gate.
 */
public final class BusinessBroadcastCampaignMutationFactory {
    /**
     * The logger for {@link BusinessBroadcastCampaignMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(BusinessBroadcastCampaignMutationFactory.class);

    /**
     * Holds the WAM telemetry service used to commit the
     * {@link com.github.auties00.cobalt.wire.wam.event.AudienceManagementEvent}
     * emitted whenever a campaign audience is set or removed.
     */
    private final WamService wamService;

    /**
     * Creates a factory bound to the WAM telemetry service.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     *
     * @param wamService the WAM telemetry service that receives the
     *                   audience-management beacon, must not be {@code null}
     * @throws NullPointerException if {@code wamService} is {@code null}
     */
    public BusinessBroadcastCampaignMutationFactory(WamService wamService) {
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
    }

    /**
     * Returns a SET mutation that creates or updates a Business Broadcast campaign.
     *
     * <p>Call this when the user schedules a new campaign or edits an existing
     * one; the mutation index follows
     * {@snippet :
     *     ["businessBroadcastCampaign", campaignId]
     * }
     * and the {@link BusinessBroadcastCampaignAction} sub-message carries the
     * campaign descriptor (broadcast JID, device id, status, timestamps,
     * reserved quota). The action payload is passed in already built, so callers
     * control all required fields ({@code broadcastJid}, {@code deviceId},
     * {@code status}) that the receive-side handler validates.
     *
     * @implNote
     * This implementation emits a {@link SyncdOperation#SET} pinned to
     * {@link BusinessBroadcastCampaignAction#ACTION_VERSION}.
     *
     * @param campaignId the campaign identifier used as the mutation index
     * @param action     the pre-built campaign descriptor payload
     * @param timestamp  the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebBroadcastCampaignSync", exports = "getCampaignMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getCampaignMutation(
            String campaignId,
            BusinessBroadcastCampaignAction action,
            Instant timestamp
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building business broadcast campaign mutation id={0}", campaignId);
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .businessBroadcastCampaignAction(action)
                .build();
        var index = JSON.toJSONString(List.of(BusinessBroadcastCampaignAction.ACTION_NAME, campaignId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                BusinessBroadcastCampaignAction.ACTION_VERSION
        );
        var dynamic = action.adId().isPresent();
        var extraData = new LinkedHashMap<String, Object>();
        extraData.put("campaign_id", campaignId);
        action.broadcastJid().ifPresent(jid -> extraData.put("broadcast_jid", jid));
        action.reservedQuota().ifPresent(quota -> extraData.put("reserved_count", quota));
        action.status().ifPresent(status -> extraData.put("campaign_status", status.name().toLowerCase(Locale.ROOT)));
        emitAudienceManagement(
                dynamic ? AudienceManagementActionType.SET_DYNAMIC : AudienceManagementActionType.SET_EXPLICIT,
                dynamic ? AudiencePredicateTypeEnum.CHATTED_RECENTLY : AudiencePredicateTypeEnum.EXPLICIT,
                AudienceResolutionTriggerType.USER_VIEW,
                AudienceEventSurfaceType.MANUAL_PICK,
                JSON.toJSONString(extraData)
        );
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Returns a REMOVE mutation that deletes a Business Broadcast campaign.
     *
     * <p>Call this when the user cancels a scheduled campaign or deletes an
     * already-completed one; the mutation index follows
     * {@snippet :
     *     ["businessBroadcastCampaign", campaignId]
     * }
     * with an empty value, which the receive-side handler uses to look up the
     * existing campaign row before removing it.
     *
     * @implNote
     * This implementation emits a {@link SyncdOperation#REMOVE} with an empty
     * {@link SyncActionValue}; only the
     * timestamp and index travel on the wire.
     *
     * @param campaignId the campaign identifier to delete
     * @param timestamp  the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebBroadcastCampaignSync", exports = "getDeleteCampaignMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getDeleteCampaignMutation(String campaignId, Instant timestamp) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building business broadcast campaign delete mutation id={0}", campaignId);
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .build();
        var index = JSON.toJSONString(List.of(BusinessBroadcastCampaignAction.ACTION_NAME, campaignId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.REMOVE,
                timestamp,
                BusinessBroadcastCampaignAction.ACTION_VERSION
        );
        var extraData = new LinkedHashMap<String, Object>();
        extraData.put("campaign_id", campaignId);
        emitAudienceManagement(
                AudienceManagementActionType.DELETED,
                AudiencePredicateTypeEnum.UNKNOWN,
                AudienceResolutionTriggerType.USER_VIEW,
                null,
                JSON.toJSONString(extraData)
        );
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Commits the {@link com.github.auties00.cobalt.wire.wam.event.AudienceManagementEvent}
     * (WAM id {@code 7900}) describing a change to a campaign's audience.
     *
     * <p>Both campaign mutations funnel through this helper: the SET path
     * reports the audience being set (explicit broadcast list or ad-driven
     * dynamic audience) and the REMOVE path reports the audience being deleted.
     * The {@code surface} argument records where the audience selection
     * originated and is omitted (left absent on the wire) when {@code null},
     * mirroring WA Web's resolver which sets it only when the origin is known.
     *
     * @implNote
     * This implementation reports {@link AudienceResolutionTriggerType#USER_VIEW}
     * because Cobalt commits from the user-driven campaign mutation rather than
     * from WA Web's {@code WAWebBizBroadcastAudienceRefreshJob} background job,
     * which is the only upstream call site and always reports
     * {@link AudienceResolutionTriggerType#PERIODIC_REFRESH}.
     *
     * @param action    the audience-management action being performed
     * @param predicate the predicate that classifies the campaign's audience
     * @param trigger   the resolution trigger that produced this beacon
     * @param surface   the surface the audience selection came from, or
     *                  {@code null} to leave it unset
     * @param extraData the JSON descriptor of the affected campaign
     */
    @WhatsAppWebExport(moduleName = "WAWebBizBroadcastAudienceRefreshJob", exports = "refreshTimeBasedAudiences", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitAudienceManagement(
            AudienceManagementActionType action,
            AudiencePredicateTypeEnum predicate,
            AudienceResolutionTriggerType trigger,
            AudienceEventSurfaceType surface,
            String extraData
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "committing audience management event action={0} predicate={1} trigger={2}", action, predicate, trigger);
        var builder = new AudienceManagementEventBuilder()
                .audienceManagementAction(action)
                .audiencePredicateType(predicate)
                .audienceResolutionTrigger(trigger)
                .audienceExtraData(extraData);
        if (surface != null) {
            builder.audienceEventSurface(surface);
        }
        wamService.commit(builder.build());
    }
}
