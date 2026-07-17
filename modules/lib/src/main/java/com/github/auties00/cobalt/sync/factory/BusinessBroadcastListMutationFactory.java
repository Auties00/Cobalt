package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BroadcastListParticipantAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastListAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastListActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that create, update, or delete a Business Broadcast list.
 *
 * <p>SMB users curate the audience for a recurring broadcast through the Web
 * UI, and the resulting mutations populate the broadcast-list collection
 * consistently across linked devices. This factory builds the outgoing
 * mutations; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.BusinessBroadcastListHandler}.
 *
 * @implNote
 * This implementation accepts the audience expression as an already-serialized
 * JSON string. WA Web runs the object through
 * {@code WAWebAudienceExpressionTypes.serializeAudienceExpression} before
 * persisting; that AudienceExpression DSL is not ported to Cobalt, so callers
 * either supply the pre-serialized form or pass {@code null} to leave the
 * field clear.
 */
public final class BusinessBroadcastListMutationFactory {
    /**
     * The logger for {@link BusinessBroadcastListMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(BusinessBroadcastListMutationFactory.class);

    /**
     * Creates a stateless factory with no collaborators.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     */
    public BusinessBroadcastListMutationFactory() {

    }

    /**
     * Returns a SET mutation for a broadcast list whose audience is described purely by its explicit participant snapshot.
     *
     * <p>Convenience overload for the common call path where the broadcast list
     * has no label-driven or DSL-driven audience predicate; delegates to
     * {@link #getBroadcastListMutation(String, List, String, Instant, String)}
     * with a {@code null} audience expression, so the receive-side handler falls
     * back to an explicit expression over the participant LID list.
     *
     * @param listId       the broadcast-list identifier used as the mutation index
     * @param participants the ordered list of recipients in LID form
     * @param listName     the user-visible name of the broadcast list
     * @param timestamp    the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebBroadcastListSync", exports = "getBroadcastListMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getBroadcastListMutation(
            String listId,
            List<BroadcastListParticipantAction> participants,
            String listName,
            Instant timestamp
    ) {
        return getBroadcastListMutation(listId, participants, listName, timestamp, null);
    }

    /**
     * Returns a SET mutation that creates or updates a broadcast list with an explicit audience expression.
     *
     * <p>Call this when the user saves a broadcast-list edit through the Web UI;
     * the mutation index follows
     * {@snippet :
     *     ["businessBroadcastList", listId]
     * }
     * and the {@link BusinessBroadcastListAction} sub-message carries the
     * participants snapshot, the list name, an always-empty {@code labelIds}
     * field, and the serialized audience expression (or {@code null} to clear
     * it).
     *
     * @implNote
     * This implementation pins {@code labelIds} to an empty list to match WA
     * Web, which hardcodes the same empty list at the call site; receive-side
     * audience resolution prefers {@code audienceExpression} over
     * {@code labelIds} when both are present.
     *
     * @param listId             the broadcast-list identifier used as the mutation index
     * @param participants       the ordered list of recipients in LID form
     * @param listName           the user-visible name of the broadcast list
     * @param timestamp          the mutation timestamp
     * @param audienceExpression the pre-serialized audience-expression JSON, or {@code null}
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebBroadcastListSync", exports = "getBroadcastListMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getBroadcastListMutation(
            String listId,
            List<BroadcastListParticipantAction> participants,
            String listName,
            Instant timestamp,
            String audienceExpression
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building business broadcast list mutation id={0} participants={1}", listId, participants.size());
        var action = new BusinessBroadcastListActionBuilder()
                .participants(participants)
                .listName(listName)
                .labelIds(List.of())
                .audienceExpression(audienceExpression)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .businessBroadcastListAction(action)
                .build();
        var index = JSON.toJSONString(List.of(BusinessBroadcastListAction.ACTION_NAME, listId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                BusinessBroadcastListAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Returns a REMOVE mutation that deletes a broadcast list.
     *
     * <p>Call this when the user deletes a broadcast list from the Web UI; the
     * mutation index follows
     * {@snippet :
     *     ["businessBroadcastList", listId]
     * }
     * with an empty value, which the receive-side handler uses to remove the
     * list from the broadcast-list collection.
     *
     * @implNote
     * This implementation emits a {@link SyncdOperation#REMOVE} with an empty
     * {@link SyncActionValue}.
     *
     * @param listId    the broadcast-list identifier to delete
     * @param timestamp the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebBroadcastListSync", exports = "getDeleteBroadcastListMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getDeleteBroadcastListMutation(String listId, Instant timestamp) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building business broadcast list delete mutation id={0}", listId);
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .build();
        var index = JSON.toJSONString(List.of(BusinessBroadcastListAction.ACTION_NAME, listId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.REMOVE,
                timestamp,
                BusinessBroadcastListAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
