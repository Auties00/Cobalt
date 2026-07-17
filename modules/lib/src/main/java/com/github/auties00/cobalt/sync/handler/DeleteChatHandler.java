package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.ConflictResolution;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code deleteChat} app-state sync action that removes a chat
 * across the user's linked devices.
 *
 * <p>This handler drives the chat-list "Delete chat" affordance that the
 * primary device fans out to its companions through the
 * {@link SyncPatchType#REGULAR_HIGH} collection. The mutation index encodes the
 * target chat and a {@code deleteMedia} flag selecting whether attached media
 * should be erased alongside the messages, formatted as
 * {@snippet :
 *     ["deleteChat", chatJid, deleteMedia]   // deleteMedia = "0" keeps media, "1" deletes
 * }
 *
 * @implNote
 * This implementation derives from
 * {@link WebAppStateActionHandler} rather than WA Web's
 * {@code ChatMessageRangeSyncdActionBase}, so the handler does not maintain
 * an active-message-range cache and {@link #applyMutation} performs a full
 * chat removal regardless of the message-range comparison; partial
 * deletions, history-sync boundary updates, AI-thread cleanup and
 * {@code coexHostedDevices} bookkeeping are not modelled.
 */
@WhatsAppWebModule(moduleName = "WAWebDeleteChatSync")
public final class DeleteChatHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link DeleteChatHandler}.
     */
    private static final System.Logger LOGGER = Log.get(DeleteChatHandler.class);

    /**
     * Constructs a new singleton {@link DeleteChatHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebDeleteChatSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public DeleteChatHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteChatSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return DeleteChatAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteChatSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return DeleteChatAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteChatSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return DeleteChatAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation parses the mutation index, validates the embedded
     * {@link Jid} and the {@link DeleteChatAction#messageRange()} payload,
     * and then removes the entire chat from
     * {@link LinkedWhatsAppStore} when the chat is
     * present. WA Web's {@code $DeleteChatSync$p_1} compares the incoming
     * range against the local chat's current range and emits either a
     * partial {@code queryAndRemoveMessagesInMessageRange} or a full
     * {@code deleteFromStorage}; Cobalt always performs a full removal because
     * the {@link com.github.auties00.cobalt.wire.linked.chat.Chat} abstraction does
     * not expose per-range deletion. Any thrown exception is mapped to
     * {@link MutationApplicationResult#failed()} mirroring WA Web's
     * try/catch shape.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteChatSync", exports = {"applyMutations", "getMessageRange", "$DeleteChatSync$p_1", "deleteChat"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        try {
            var indexParts = JSON.parseArray(mutation.index());
            var chatJidString = indexParts.getString(1);
            var deleteMediaString = indexParts.getString(2);

            if (chatJidString == null || chatJidString.isEmpty()
                    || deleteMediaString == null || deleteMediaString.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "delete chat: malformed mutation index");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            Jid chatJid;
            try {
                chatJid = Jid.of(chatJidString);
            } catch (Exception e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "delete chat: failed to parse chat jid={0}", new LogRedactable.User(chatJidString));
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (chatJid == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "delete chat: chat jid resolved to null, jid={0}", new LogRedactable.User(chatJidString));
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof DeleteChatAction deleteChatAction)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "delete chat: mutation value is not a DeleteChatAction");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var messageRange = deleteChatAction.messageRange().orElse(null);
            if (messageRange == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "delete chat: missing message range chat={0}", chatJid);
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var chat = client.store().chatStore().findChatByJid(chatJid);
            if (chat.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "delete chat: orphan, chat not found {0}", chatJid);
                return MutationApplicationResult.orphan(chatJidString, "Chat");
            }

            // TODO: support partial chat deletion when the local message range only
            //       overlaps the incoming one. WA Web's $DeleteChatSync$p_1 calls
            //       queryAndRemoveMessagesInMessageRange in that branch; Cobalt
            //       currently falls through to a full removal and loses the messages
            //       outside the incoming range.
            client.store().chatStore().removeChat(chat.get());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "delete chat: removed chat={0} deleteMedia={1}", chatJid, deleteMediaString);

            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "delete chat: failed to apply mutation", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation decodes both {@link DeleteChatAction} payloads and
     * delegates the four-way enclosure decision to
     * {@link MessageRangeUtils#compareMessageRanges}. When neither range
     * encloses the other, a merged {@link DeleteChatAction} is returned via
     * {@link ConflictResolution#merged} for the caller to apply, separating
     * resolution from application; WA Web instead applies the merged
     * mutation immediately under {@code lockForMessageRangeSync}. A
     * {@code null} action or {@code messageRange} on either side defaults
     * to {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL} where WA
     * Web would throw via {@code WANullthrows}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteChatSync", exports = "resolveConflicts", adaptation = WhatsAppAdaptation.ADAPTED)
    public ConflictResolution resolveConflicts(DecryptedMutation.Trusted localMutation, DecryptedMutation.Trusted remoteMutation) {
        var localAction = localMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof DeleteChatAction)
                .map(a -> (DeleteChatAction) a)
                .orElse(null);
        var remoteAction = remoteMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof DeleteChatAction)
                .map(a -> (DeleteChatAction) a)
                .orElse(null);

        if (localAction == null || remoteAction == null) {
            return ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
        }

        var localRange = localAction.messageRange().orElse(null);
        var remoteRange = remoteAction.messageRange().orElse(null);

        if (localRange == null || remoteRange == null) {
            return ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
        }

        return switch (MessageRangeUtils.compareMessageRanges(remoteRange, localRange)) {
            case RANGE_A_ENCLOSES_RANGE_B ->
                    ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
            case RANGE_B_ENCLOSES_RANGE_A ->
                    ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE);
            case RANGES_ARE_EQUAL ->
                    localMutation.timestamp().compareTo(remoteMutation.timestamp()) <= 0
                            ? ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL)
                            : ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE);
            case RANGES_NOT_ENCLOSING -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "delete chat: merging conflicting message ranges");
                var mergedRange = MessageRangeUtils.mergeMessageRanges(remoteRange, localRange);
                var mergedAction = new DeleteChatActionBuilder()
                        .messageRange(mergedRange)
                        .build();
                var mergedValue = new SyncActionValueBuilder()
                        .timestamp(remoteMutation.timestamp())
                        .deleteChatAction(mergedAction)
                        .build();
                var merged = new DecryptedMutation.Trusted(
                        localMutation.index(),
                        mergedValue,
                        localMutation.operation(),
                        localMutation.timestamp(),
                        localMutation.actionVersion()
                );
                yield ConflictResolution.merged(merged);
            }
        };
    }

}
