package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelAssociationAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelAssociationActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that associate (or disassociate) a Business label with a chat or contact.
 *
 * Drives the Business label-management surfaces: per-target add and remove operations, association
 * purges on chat deletion, and editor commits all flow through this factory. This factory is the
 * outgoing-mutation counterpart of
 * {@link com.github.auties00.cobalt.sync.handler.LabelAssociationHandler}.
 *
 * @implNote
 * This implementation emits one mutation per {@code (labelId, targetJid)} pair. WA Web's
 * {@code createLabelAssociationMutations} loops over a {@code labels[]} x {@code targets[]} cartesian
 * product and additionally emits {@code WAWebWamLabelSyncTrackingReporter} telemetry; Cobalt does not
 * run WAM label tracking, so the call site loops at its own level and the telemetry hook is omitted.
 */
public final class LabelAssociationMutationFactory {
    /**
     * The logger for {@link LabelAssociationMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(LabelAssociationMutationFactory.class);

    /**
     * Creates an instance with no collaborators.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public LabelAssociationMutationFactory() {

    }

    /**
     * Returns a SET mutation that adds or removes a label-to-target association.
     *
     * The mutation index follows
     * {@snippet :
     *     ["label_jid", labelId, targetJid.toString()]
     * }
     * and the {@link LabelAssociationAction} sub-message carries the {@code labeled} flag (a "remove"
     * association still emits a SET, with {@code labeled == false}). Callers must pre-resolve LID 1x1
     * migration on {@code targetJid} when applicable; WA Web does that resolution inline by calling
     * {@code WAWebSyncdGetChat.getWidMutationIndexForWid} on each target.
     *
     * @implNote
     * This implementation pins the action version through {@link LabelAssociationAction#ACTION_VERSION},
     * which matches WA Web's {@code WASyncdConst.LABEL_ASSOCIATION_SYNC_VERSION}. The
     * {@code modelMetaData} field is left empty to match WA Web's call site, which also sets it to an
     * empty array.
     *
     * @param labelId   the label identifier (caller stringifies)
     * @param targetJid the chat or contact {@link Jid} being labelled
     * @param labeled   {@code true} to add the association, {@code false} to remove it
     * @param timestamp the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelJidSync", exports = "createLabelAssociationMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation createLabelAssociationMutation(
            String labelId,
            Jid targetJid,
            boolean labeled,
            Instant timestamp
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building label association mutation label={0} target={1} labeled={2}", labelId, targetJid, labeled);
        var action = new LabelAssociationActionBuilder()
                .labeled(labeled)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .labelAssociationAction(action)
                .build();
        var index = JSON.toJSONString(List.of(LabelAssociationAction.ACTION_NAME, labelId, targetJid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                LabelAssociationAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
