package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.*;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.DeleteMessageForMeAction;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.sync.ConflictResolution;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code deleteMessageForMe} app-state sync action that removes a
 * single message from the local view without touching the remote copy.
 *
 * <p>This handler drives the "Delete for me" message-context affordance that
 * the primary device fans out to its companions through the
 * {@link SyncPatchType#REGULAR_HIGH} collection. The mutation index encodes the
 * chat, the message id, the {@code fromMe} flag and the participant (relevant
 * for group messages), formatted as
 * {@snippet :
 *     ["deleteMessageForMe", chatJid, messageId, fromMe, participant] // fromMe = "1" or "0"
 * }
 *
 * @implNote
 * This implementation locates the chat directly via
 * {@link LinkedWhatsAppChatStore#findChatByJid} rather
 * than through WA Web's {@code WAWebSyncdResolveMessages.resolveMessagesForMutations}
 * cache, and uses
 * {@link LinkedWhatsAppChatStore#findMessageById}
 * filtered by {@code fromMe} and participant in place of the
 * {@code msgKeyToDbIdWithoutFromMeParticipant} prefix scan. Add-on cleanup,
 * {@code processDeleteForMeSingle} fallbacks, employee-only debug logging
 * and the LID alternate-key probe are not modelled.
 */
@WhatsAppWebModule(moduleName = "WAWebDeleteMessageForMeSync")
public final class DeleteMessageForMeHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link DeleteMessageForMeHandler}.
     */
    private static final System.Logger LOGGER = Log.get(DeleteMessageForMeHandler.class);

    /**
     * Constructs a new singleton {@link DeleteMessageForMeHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebDeleteMessageForMeSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public DeleteMessageForMeHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteMessageForMeSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return DeleteMessageForMeAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteMessageForMeSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return DeleteMessageForMeAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteMessageForMeSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return DeleteMessageForMeAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reports
     * {@link MutationApplicationResult#orphan(String, String)} with model
     * type {@code "Msg"} when either the chat or the matching message is
     * absent, so the orchestrator can later replay the mutation; WA Web's
     * {@code resolveMessagesForMutations} batches that lookup across all
     * mutations in the same collection. The fallback that probes for an
     * add-on via {@code WAWebAddonProcessDeleteForMe} is not modelled, so
     * a deletion that targets a previously-saved add-on without a
     * surviving primary message remains classified as orphan.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteMessageForMeSync", exports = {"applyMutations", "getMessageKey"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() < 5) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "delete message for me: malformed mutation index");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var chatJidString = indexArray.getString(1);
        var messageId = indexArray.getString(2);
        var fromMeString = indexArray.getString(3);
        var participantString = indexArray.getString(4);

        if (isNullOrEmpty(chatJidString) || isNullOrEmpty(messageId)
                || isNullOrEmpty(fromMeString) || isNullOrEmpty(participantString)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "delete message for me: malformed mutation index fields");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var msgKey = SyncdIndexUtils.syncKeyToMsgKey(
                client.store(), chatJidString, messageId, fromMeString, participantString
        );
        if (msgKey.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "delete message for me: failed to resolve message key, id={0}", messageId);
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var chatJid = Jid.of(chatJidString);
        var chat = client.store().chatStore().findChatByJid(chatJid);
        if (chat.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "delete message for me: orphan, chat not found {0}", chatJid);
            return MutationApplicationResult.orphan(
                    SyncdIndexUtils.serializeMessageKey(msgKey.get()),
                    "Msg"
            );
        }

        var fromMe = "1".equals(fromMeString);
        var participant = !"0".equals(participantString) ? Jid.of(participantString) : null;
        var removed = client.store().chatStore().findMessageById(chat.get(), messageId)
                .filter(msg -> msg.key().fromMe() == fromMe)
                .filter(msg -> participant == null || participant.toUserJid().equals(msg.key().senderJid().map(Jid::toUserJid).orElse(null)))
                .flatMap(info -> info.key().id())
                .map(id -> {
                    chat.get().removeMessage(id);
                    return id;
                })
                .isPresent();

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "delete message for me: {0} messageId={1} chat={2}",
                    removed ? "removed" : "orphan, message not found", messageId, chatJid);
        }

        return removed
                ? MutationApplicationResult.success()
                : MutationApplicationResult.orphan(
                        SyncdIndexUtils.serializeMessageKey(msgKey.get()),
                        "Msg"
                );
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reads only the
     * {@link DeleteMessageForMeAction#deleteMedia()} flag on each side and
     * keeps the more aggressive local mutation
     * ({@link MutationConflictResolutionState#SKIP_REMOTE}) when the remote keeps
     * media but the local would erase it; in every other case both
     * mutations are dropped via
     * {@link MutationConflictResolutionState#SKIP_REMOTE_DROP_LOCAL}. Timestamps
     * are intentionally ignored, mirroring WA Web. A {@code null} payload
     * on either side coalesces to {@code false} where WA Web would throw
     * via {@code WANullthrows}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDeleteMessageForMeSync", exports = "resolveConflicts", adaptation = WhatsAppAdaptation.DIRECT)
    public ConflictResolution resolveConflicts(DecryptedMutation.Trusted localMutation, DecryptedMutation.Trusted remoteMutation) {
        var localDeleteMedia = localMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof DeleteMessageForMeAction)
                .map(a -> ((DeleteMessageForMeAction) a).deleteMedia())
                .orElse(false);
        var remoteDeleteMedia = remoteMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof DeleteMessageForMeAction)
                .map(a -> ((DeleteMessageForMeAction) a).deleteMedia())
                .orElse(false);

        if (!remoteDeleteMedia && localDeleteMedia) {
            return ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE);
        }

        return ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL);
    }

    /**
     * Returns whether the given string is {@code null} or empty.
     *
     * <p>Gates the malformed-index check applied to every part of the
     * four-element index before the mutation is accepted.
     *
     * @param value the candidate string
     * @return {@code true} when {@code value} is {@code null} or has length
     *         zero
     */
    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
