package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that delete a chat entirely (storage row, messages, AI threads).
 *
 * Drives the chat-delete affordance: when the user removes a chat the resulting
 * {@link SyncPendingMutation} is pushed via
 * {@link com.github.auties00.cobalt.sync.WebAppStateService} so linked devices delete the same chat
 * row and drop the associated AI threads. This factory is the outgoing-mutation counterpart of
 * {@link com.github.auties00.cobalt.sync.handler.DeleteChatHandler}.
 *
 * @implNote
 * This implementation accepts a caller-supplied {@link SyncActionMessageRange} because Cobalt does
 * not run the {@code WAWebMessageRangeUtils.constructForwardMovingMessageRange} pipeline, which is
 * tied to the browser-side active-message-range IndexedDB tables. WA Web's
 * {@code getDeleteChatMutation} also emits a {@code WAWebMdSyncdDogfoodingFeatureUsageWamEvent}; in
 * Cobalt the WAM event is dispatched from the caller layer because this factory has no
 * {@link com.github.auties00.cobalt.wam.WamService} handle.
 */
public final class DeleteChatMutationFactory {
    /**
     * The logger for {@link DeleteChatMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(DeleteChatMutationFactory.class);

    /**
     * Creates an instance with no collaborators.
     *
     * The factory is stateless, so a single instance may be shared across the lifetime of the
     * client.
     */
    public DeleteChatMutationFactory() {

    }

    /**
     * Returns a SET mutation that deletes the given chat.
     *
     * The mutation index follows
     * {@snippet :
     *     ["deleteChat", chatJid.toString(), deleteMediaFiles ? "1" : "0"]
     * }
     * The {@link DeleteChatAction} sub-message carries the {@link SyncActionMessageRange} that bounds
     * the delete; passing {@code messageRange == null} emits a delete with no range, suitable for
     * chats that hold no messages.
     *
     * @implNote
     * This implementation does not coalesce against existing pending mutations the way WA Web's
     * {@code getDeleteChatMutation} does via {@code WAWebSyncdDb.getPendingMutationsRowsByIndex};
     * Cobalt's app-state pipeline merges at a higher layer.
     *
     * @param timestamp        the mutation timestamp
     * @param chatJid          the chat {@link Jid} being deleted
     * @param deleteMediaFiles {@code true} if the on-disk media files must be deleted as well
     * @param messageRange     the pre-built range covering the messages to delete, or {@code null}
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebDeleteChatSync", exports = {"getDeleteChatMutation", "buildDeleteChatMutation", "buildDeleteChatIndexArgs"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getDeleteChatMutation(
            Instant timestamp,
            Jid chatJid,
            boolean deleteMediaFiles,
            SyncActionMessageRange messageRange
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building delete chat mutation chat={0} deleteMedia={1}", chatJid, deleteMediaFiles);
        var actionBuilder = new DeleteChatActionBuilder();
        if (messageRange != null) {
            actionBuilder.messageRange(messageRange);
        }
        var action = actionBuilder.build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .deleteChatAction(action)
                .build();
        var index = JSON.toJSONString(List.of(
                DeleteChatAction.ACTION_NAME,
                chatJid.toString(),
                deleteMediaFiles ? "1" : "0"
        ));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                DeleteChatAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
