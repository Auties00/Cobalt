package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelReorderingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelReorderingActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing label-reordering sync mutations.
 *
 * Drives the chat-label drag-to-reorder gesture on the Business chat-list surface; one call produces
 * a single {@link SyncPendingMutation} that carries the full ordered identifier list and replays via
 * {@link com.github.auties00.cobalt.sync.handler.LabelReorderingHandler} on every linked device.
 *
 * @implNote
 * This implementation has no direct WA Web counterpart: the {@code WAWebLabelReorderingSync} module
 * ships only the inbound {@code applyMutations} half and pairs it with a sibling
 * {@code WAWebDBLabelsReorder.updateLabelsSortOrder} call rather than a typed mutation builder. The
 * shape mirrors {@code WAWebSyncdActionUtils.buildPendingMutation} and reuses the action version
 * exposed by {@link LabelReorderingAction}.
 */
public final class LabelReorderingMutationFactory {
    /**
     * The logger for {@link LabelReorderingMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(LabelReorderingMutationFactory.class);

    /**
     * Constructs a label-reordering mutation factory.
     *
     * The factory keeps no state, so a single instance is sufficient per client.
     */
    public LabelReorderingMutationFactory() {

    }

    /**
     * Builds a pending SET mutation that broadcasts the current chat-label ordering to every linked device.
     *
     * The resulting {@link SyncPendingMutation} is appended to the regular collection queue and
     * consumed by {@link com.github.auties00.cobalt.sync.handler.LabelReorderingHandler} on receiving
     * devices. Empty or unordered lists are rejected by the receiver as a malformed action value, so
     * callers must pass the full sorted identifier list.
     *
     * @implNote
     * This implementation mirrors the sibling {@code WAWebSyncdActionUtils.buildPendingMutation} shape
     * because {@code WAWebLabelReorderingSync} exposes no outgoing helper of its own; the index uses
     * the canonical action name with no per-row arguments since the action is a singleton per account.
     *
     * @param sortedLabelIds the full ordered list of integer label identifiers; receiving devices store this list verbatim and reapply it as their sort order
     * @param timestamp      the mutation timestamp recorded both on the outer mutation and on the inner sync action value
     * @return the pending mutation for the reorder operation
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelReorderingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getReorderLabelsMutation(
            List<Integer> sortedLabelIds,
            Instant timestamp
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building label reordering mutation count={0}", sortedLabelIds.size());
        var action = new LabelReorderingActionBuilder()
                .sortedLabelIds(sortedLabelIds)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .labelReorderingAction(action)
                .build();
        var index = JSON.toJSONString(List.of(LabelReorderingAction.ACTION_NAME));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                LabelReorderingAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
