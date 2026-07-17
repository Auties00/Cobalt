package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ClearChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ClearChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that clear the messages of a chat without deleting the chat itself.
 *
 * <p>When the user clears a conversation, the resulting
 * {@link SyncPendingMutation} is pushed via
 * {@link com.github.auties00.cobalt.sync.WebAppStateService#pushPatches} so
 * linked devices apply the same range-bounded delete. This factory builds the
 * outgoing mutation; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.ClearChatHandler}.
 *
 * @implNote
 * This implementation accepts a caller-supplied {@link SyncActionMessageRange}
 * because Cobalt does not run WA Web's forward-moving message-range pipeline,
 * which is tied to the browser-side active-message-range IndexedDB tables. It
 * also drops WA Web's WAM telemetry commit; the caller layer is responsible for
 * emitting WAM events.
 */
public final class ClearChatMutationFactory {
    /**
     * The logger for {@link ClearChatMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(ClearChatMutationFactory.class);

    /**
     * Creates a stateless factory with no collaborators.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     */
    public ClearChatMutationFactory() {

    }

    /**
     * Returns a SET mutation that clears the messages of the given chat over the supplied range.
     *
     * <p>The mutation index follows
     * {@snippet :
     *     ["clearChat", chatJid.toString(), deleteStarred ? "1" : "0", deleteMedia ? "1" : "0"]
     * }
     * and the {@link ClearChatAction} sub-message carries the
     * {@link SyncActionMessageRange} that bounds the delete. Passing a
     * {@code null} {@code messageRange} emits a clear with no range, which the
     * receive-side handler treats as clearing whatever was visible at mutation
     * time.
     *
     * @implNote
     * This implementation does not coalesce against existing pending mutations
     * the way WA Web does; Cobalt's app-state pipeline merges at a higher layer.
     * The {@code deleteMedia} flag is written verbatim as the fourth index
     * segment to keep wire parity with WA Web's index builder.
     *
     * @param timestamp     the mutation timestamp
     * @param chatJid       the chat {@link Jid} whose messages are being cleared
     * @param deleteStarred {@code true} if starred messages must be deleted alongside the rest
     * @param deleteMedia   {@code true} if the on-disk media files must be deleted as well
     * @param messageRange  the pre-built range covering the messages to clear, or {@code null}
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebClearChatSync", exports = {"getClearChatMutation", "$ClearChatSync$p_3"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getClearChatMutation(
            Instant timestamp,
            Jid chatJid,
            boolean deleteStarred,
            boolean deleteMedia,
            SyncActionMessageRange messageRange
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building clear chat mutation chat={0} deleteStarred={1} deleteMedia={2}", chatJid, deleteStarred, deleteMedia);
        var actionBuilder = new ClearChatActionBuilder();
        if (messageRange != null) {
            actionBuilder.messageRange(messageRange);
        }
        var action = actionBuilder.build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .clearChatAction(action)
                .build();
        var index = JSON.toJSONString(List.of(
                ClearChatAction.ACTION_NAME,
                chatJid.toString(),
                deleteStarred ? "1" : "0",
                deleteMedia ? "1" : "0"
        ));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                ClearChatAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
