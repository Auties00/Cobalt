package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.sync.ConflictResolution;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ClearChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ClearChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Clears the message history of a chat in response to a {@code clearChat} sync mutation.
 *
 * <p>This handler drives the per-chat "Clear messages" action. When the user
 * clears a chat on another device, the server replays the resulting
 * {@link ClearChatAction} here, and the affected
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat} loses its message history
 * while the chat row itself remains.
 *
 * @implNote
 * This implementation reduces WA Web's
 * {@code WAWebClearChatSync.$ClearChatSync$p_2} pipeline to a single
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat#removeMessages()}
 * call. The
 * {@code deleteStarred} / {@code deleteMedia} / {@code skipMessages}
 * filters, the per-thread cleanup, the add-on cleanup, the AI-thread
 * deletion, and the {@code maybeClearGroupStatus} side effect are all
 * dropped because they are browser-IndexedDB concerns or surface on
 * collections Cobalt does not maintain. The
 * {@code addActiveMessageRange} bookkeeping is likewise dropped
 * because Cobalt does not track active message ranges.
 */
@WhatsAppWebModule(moduleName = "WAWebClearChatSync")
public final class ClearChatHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link ClearChatHandler}.
     */
    private static final System.Logger LOGGER = Log.get(ClearChatHandler.class);

    /**
     * The zero-based index slot of the chat JID inside a {@code clearChat} mutation index array.
     *
     * <p>The {@code deleteStarred} and {@code deleteMedia} flags sit at the two
     * slots immediately after.
     */
    @WhatsAppWebExport(moduleName = "WAWebClearChatSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int CHAT_JID_INDEX = 1;

    /**
     * Constructs the singleton clear-chat handler.
     *
     * <p>The sync handler registry instantiates this once during client
     * bootstrap.
     */
    @WhatsAppWebExport(moduleName = "WAWebClearChatSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public ClearChatHandler() {
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebClearChatSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return ClearChatAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebClearChatSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return ClearChatAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebClearChatSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return ClearChatAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the four-element JSON index
     * {@code ["clearChat", chatJid, deleteStarred, deleteMedia]} where
     * {@code deleteStarred} and {@code deleteMedia} are the wire strings
     * {@code "1"} or {@code "0"}, locates the target
     * {@link com.github.auties00.cobalt.wire.linked.chat.Chat}, and drops its
     * messages. Returns {@link MutationApplicationResult#unsupported()} for
     * non-{@code SET} operations, an orphan result keyed by chat JID and model
     * type {@code "Chat"} when the chat is not in the store, malformed results
     * when the index or value is shaped wrong, and
     * {@link MutationApplicationResult#failed()} on any thrown exception
     * (including a {@link Jid#of(String)} parse failure).
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebClearChatSync", exports = {"applyMutations", "getMessageRange"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat mutation unsupported: operation={0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        try {
            var indexParts = JSON.parseArray(mutation.index());
            var chatJidString = indexParts.getString(CHAT_JID_INDEX);
            var deleteStarredString = indexParts.getString(CHAT_JID_INDEX + 1);
            var deleteMediaString = indexParts.getString(CHAT_JID_INDEX + 2);

            if (chatJidString == null || chatJidString.isEmpty()
                    || deleteStarredString == null || deleteStarredString.isEmpty()
                    || deleteMediaString == null || deleteMediaString.isEmpty()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "clear chat mutation malformed: incomplete index");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            Jid chatJid;
            try {
                chatJid = Jid.of(chatJidString);
            } catch (Exception e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "clear chat mutation malformed: chat jid parse failed");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (chatJid == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "clear chat mutation malformed: chat jid resolved to null");
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof ClearChatAction clearChatAction)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "clear chat mutation malformed: missing action value");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var messageRange = clearChatAction.messageRange().orElse(null);
            if (messageRange == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "clear chat mutation malformed: missing message range");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var chat = client.store().chatStore().findChatByJid(chatJid);
            if (chat.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat mutation orphaned: chat not found {0}", chatJid);
                return MutationApplicationResult.orphan(chatJidString, "Chat");
            }

            chat.get().removeMessages();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat applied: chat={0}", chatJid);

            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "clear chat mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the local-vs-remote tie by comparing the message ranges
     * carried inside each {@link ClearChatAction}. Returns
     * {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL} when the remote
     * range encloses the local one,
     * {@link MutationConflictResolutionState#SKIP_REMOTE} when the local range encloses
     * the remote one, a timestamp tiebreaker when ranges are equal, and a
     * merged {@link ConflictResolution} carrying the union of both ranges when
     * ranges partially overlap.
     *
     * @implNote
     * This implementation returns the merged mutation for the caller
     * to apply rather than applying it inline; WA Web's
     * {@code lockForMessageRangeSync} writes the merged state to the
     * chat DB during conflict resolution. A defensive
     * {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL} is
     * returned when either side lacks a {@link ClearChatAction} or a
     * message range, replacing WA Web's {@code nullthrows} which would
     * throw.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebClearChatSync", exports = "resolveConflicts", adaptation = WhatsAppAdaptation.ADAPTED)
    public ConflictResolution resolveConflicts(DecryptedMutation.Trusted localMutation, DecryptedMutation.Trusted remoteMutation) {
        var localAction = localMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof ClearChatAction)
                .map(a -> (ClearChatAction) a)
                .orElse(null);
        var remoteAction = remoteMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof ClearChatAction)
                .map(a -> (ClearChatAction) a)
                .orElse(null);

        if (localAction == null || remoteAction == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat conflict resolved: missing action, applying remote");
            return ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
        }

        var localRange = localAction.messageRange().orElse(null);
        var remoteRange = remoteAction.messageRange().orElse(null);

        if (localRange == null || remoteRange == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat conflict resolved: missing message range, applying remote");
            return ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
        }

        return switch (MessageRangeUtils.compareMessageRanges(remoteRange, localRange)) {
            case RANGE_A_ENCLOSES_RANGE_B -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat conflict resolved: remote range encloses local, applying remote");
                yield ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
            }
            case RANGE_B_ENCLOSES_RANGE_A -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat conflict resolved: local range encloses remote, skipping remote");
                yield ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE);
            }
            case RANGES_ARE_EQUAL -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat conflict resolved: ranges equal, tiebreak by timestamp");
                yield localMutation.timestamp().compareTo(remoteMutation.timestamp()) <= 0
                        ? ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL)
                        : ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE);
            }
            case RANGES_NOT_ENCLOSING -> {
                var mergedRange = MessageRangeUtils.mergeMessageRanges(remoteRange, localRange);
                var mergedAction = new ClearChatActionBuilder()
                        .messageRange(mergedRange)
                        .build();
                var mergedValue = new SyncActionValueBuilder()
                        .timestamp(remoteMutation.timestamp())
                        .clearChatAction(mergedAction)
                        .build();
                var merged = new DecryptedMutation.Trusted(
                        localMutation.index(),
                        mergedValue,
                        localMutation.operation(),
                        localMutation.timestamp(),
                        localMutation.actionVersion()
                );
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clear chat conflict resolved: ranges partially overlap, merging");
                yield ConflictResolution.merged(merged);
            }
        };
    }

}
