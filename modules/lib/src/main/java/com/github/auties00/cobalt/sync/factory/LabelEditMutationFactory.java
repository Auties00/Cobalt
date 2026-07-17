package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelEditAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.LabelEditActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that create, edit, or delete a Business label.
 *
 * Drives the Business label-editor surfaces (add, edit, and delete). The mutation keeps the label
 * schema and the in-memory label collection consistent across linked devices, including the
 * "deleted" path in which only the {@code deleted} flag travels with the label id. This factory is
 * the outgoing-mutation counterpart of
 * {@link com.github.auties00.cobalt.sync.handler.LabelEditHandler}.
 *
 * @implNote
 * This implementation does not call {@code WAWebWamLabelSyncTrackingReporter.generateLabelEditHash}
 * or emit the matching {@code logLabelSyncEvent}; WA Web records a SUCCESS event on the sender side
 * as part of {@code getLabelMutation}, but Cobalt does not run that telemetry pipeline. The
 * {@code type} value is supplied directly as {@link LabelEditAction.ListType}, so the malformed-cast
 * warning that WA Web logs is unreachable here.
 */
public final class LabelEditMutationFactory {
    /**
     * The logger for {@link LabelEditMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(LabelEditMutationFactory.class);

    /**
     * Creates an instance with no collaborators.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public LabelEditMutationFactory() {

    }

    /**
     * Returns a SET mutation that creates, edits, or deletes a Business label.
     *
     * The mutation index follows
     * {@snippet :
     *     ["label_edit", labelId]
     * }
     * and the {@link LabelEditAction} sub-message carries only the fields the caller actually
     * populated; {@code null} fields are omitted on the wire so unchanged attributes (colour,
     * predefined id, active flag, list type) round-trip correctly. Set {@code deleted == true} to
     * emit the delete branch, in which case the receive side skips most other fields.
     *
     * @implNote
     * This implementation preserves the WA Web parameter order
     * ({@code labelId, name, color, deleted, predefinedId, isActive, type, timestamp}); changing the
     * order would change the test fixtures' argument list. Lists with type {@code AI_HANDOFF} or
     * {@code AI_RESPONDING} are still allowed as outgoing edits; the receive side reconciles
     * duplicates against its own label storage.
     *
     * @param labelId      the label identifier used as the mutation index
     * @param name         the display name, or {@code null} when unchanged
     * @param color        the palette colour index, or {@code null} when unchanged
     * @param deleted      {@code true} when the label is being deleted
     * @param predefinedId the predefined-label identifier, or {@code null} when not predefined
     * @param isActive     the active flag, or {@code null} when unchanged
     * @param type         the list type (custom, AI, server-assigned, ...), or {@code null} when unchanged
     * @param timestamp    the mutation timestamp
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebLabelSync", exports = "getLabelMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getLabelMutation(
            String labelId,
            String name,
            Integer color,
            boolean deleted,
            Integer predefinedId,
            Boolean isActive,
            LabelEditAction.ListType type,
            Instant timestamp
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building label edit mutation label={0} deleted={1}", labelId, deleted);
        var action = new LabelEditActionBuilder()
                .name(name)
                .deleted(deleted)
                .color(color)
                .predefinedId(predefinedId)
                .isActive(isActive)
                .type(type)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .labelEditAction(action)
                .build();
        var index = JSON.toJSONString(List.of(LabelEditAction.ACTION_NAME, labelId));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                LabelEditAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
