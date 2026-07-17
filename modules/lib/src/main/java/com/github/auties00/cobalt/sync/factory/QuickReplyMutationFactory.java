package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.QuickReplyAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.QuickReplyActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Builds outgoing quick-reply sync mutations.
 *
 * <p>This factory backs the Business quick-replies management surface and supports the create,
 * edit, and delete paths. Mutations produced here are consumed on receiving devices by
 * {@link com.github.auties00.cobalt.sync.handler.QuickReplyHandler}.
 *
 * @implNote
 * This implementation emits the always-empty {@code associatedLabelIds} field that WA Web emits on
 * both the add-or-edit and delete paths, and adds defensive {@code null} checks on every reference
 * parameter because the WA Web bundle assumes the caller never passes {@code null}.
 */
public final class QuickReplyMutationFactory {
    /**
     * The logger for {@link QuickReplyMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(QuickReplyMutationFactory.class);

    /**
     * Constructs a quick-reply mutation factory.
     *
     * <p>The factory keeps no state, so a single instance is sufficient per client.
     */
    public QuickReplyMutationFactory() {

    }

    /**
     * Builds a pending mutation that deletes a quick reply by its id.
     *
     * <p>The receiver detects the deletion via {@link QuickReplyAction#deleted()} returning
     * {@code true} and removes the matching row. The remaining payload fields are set to their
     * canonical empty values to match WA Web's emitter shape. The index follows the standard
     * {@code [actionName, quickReplyId]} shape.
     *
     * @implNote
     * This implementation adds defensive {@code null} checks on the {@code quickReplyId} and
     * {@code timestamp} parameters that are not present in WA Web.
     *
     * @param quickReplyId the quick reply identifier (the {@code indexArgs[0]} entry)
     * @param timestamp    the mutation timestamp
     * @return the pending mutation that removes the quick reply on the server side
     * @throws NullPointerException if {@code quickReplyId} or {@code timestamp} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebQuickRepliesSync", exports = "getQuickReplyDeleteMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getQuickReplyDeleteMutation(String quickReplyId, Instant timestamp) {
        Objects.requireNonNull(quickReplyId, "quickReplyId cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building quick reply delete mutation id={0}", quickReplyId);
        var action = new QuickReplyActionBuilder()
                .deleted(true)
                .keywords(List.of())
                .shortcut("")
                .message("")
                .count(0)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .quickReplyAction(action)
                .build();
        var index = JSON.toJSONString(List.of(QuickReplyAction.ACTION_NAME, quickReplyId));
        var pendingMutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                QuickReplyAction.ACTION_VERSION
        );
        return new SyncPendingMutation(pendingMutation, 0);
    }

    /**
     * Builds a pending mutation that creates or edits a quick reply.
     *
     * <p>The receiver-side branch rejects mutations with an empty {@code shortcut} or empty
     * {@code message} as malformed, so callers must supply non-empty strings on both. The index
     * follows the standard {@code [actionName, quickReplyId]} shape.
     *
     * @implNote
     * This implementation adds defensive {@code null} checks not present in WA Web, which trusts
     * every caller-supplied reference.
     *
     * @param quickReplyId the quick reply identifier (the {@code indexArgs[0]} entry)
     * @param shortcut     the shortcut text; non-empty
     * @param message      the quick reply message body; non-empty
     * @param count        the usage counter, incremented on every send of the quick reply
     * @param keywords     the keyword list (may be empty but not {@code null})
     * @param timestamp    the mutation timestamp
     * @return the pending mutation that creates or updates the quick reply on the server side
     * @throws NullPointerException if any reference parameter is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebQuickRepliesSync", exports = "getQuickReplyAddOrEditMutation", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getQuickReplyAddOrEditMutation(
            String quickReplyId,
            String shortcut,
            String message,
            int count,
            List<String> keywords,
            Instant timestamp
    ) {
        Objects.requireNonNull(quickReplyId, "quickReplyId cannot be null");
        Objects.requireNonNull(shortcut, "shortcut cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(keywords, "keywords cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building quick reply add-or-edit mutation id={0} count={1}", quickReplyId, count);
        var action = new QuickReplyActionBuilder()
                .deleted(false)
                .keywords(keywords)
                .shortcut(shortcut)
                .message(message)
                .count(count)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .quickReplyAction(action)
                .build();
        var index = JSON.toJSONString(List.of(QuickReplyAction.ACTION_NAME, quickReplyId));
        var pendingMutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                QuickReplyAction.ACTION_VERSION
        );
        return new SyncPendingMutation(pendingMutation, 0);
    }
}
