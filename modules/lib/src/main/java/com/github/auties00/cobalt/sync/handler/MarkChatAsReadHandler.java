package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.sync.ConflictResolution;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MarkChatAsReadAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.MarkChatAsReadActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code markChatAsRead} app-state sync action that flips a
 * chat's read or unread state across the user's linked devices.
 *
 * <p>This handler backs the chat-list "Mark as read" / "Mark as unread"
 * affordance: when the primary device toggles the read state, the resulting
 * bit fans out across the {@link SyncPatchType#REGULAR_LOW} collection so
 * companions render the same unread badge. The mutation index keys each
 * entry by the chat JID, formatted as
 * {@snippet :
 *     ["markChatAsRead", chatJid]
 * }
 * For {@link MarkChatAsReadAction#read()} {@code == true} the chat is marked
 * as not unread with {@code unreadCount = 0}; for {@code false} it is marked
 * as unread with {@code unreadCount = -1}, the sentinel WA Web uses on its
 * own chat table to render the persistent "unread" badge.
 *
 * @implNote
 * This implementation applies the read-state change directly on the local
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat}, replacing WA Web's
 * {@code frontendSendAndReceive("updateChatReadStatus", ...)} RPC. The
 * {@code addActiveMessageRange} bookkeeping and the orphan branch that WA Web
 * drives off the active message ranges are not modelled because Cobalt does
 * not maintain browser-side IndexedDB active message ranges.
 */
@WhatsAppWebModule(moduleName = "WAWebMarkChatAsReadSync")
public final class MarkChatAsReadHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link MarkChatAsReadHandler}.
     */
    private static final System.Logger LOGGER = Log.get(MarkChatAsReadHandler.class);

    /**
     * Constructs a new {@link MarkChatAsReadHandler} for registration in the
     * sync handler registry.
     */
    @WhatsAppWebExport(moduleName = "WAWebMarkChatAsReadSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MarkChatAsReadHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebMarkChatAsReadSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return MarkChatAsReadAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebMarkChatAsReadSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return MarkChatAsReadAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebMarkChatAsReadSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return MarkChatAsReadAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only {@link SyncdOperation#SET} is accepted. The resolved
     * {@link MarkChatAsReadAction} read flag is written to the matching
     * {@link com.github.auties00.cobalt.wire.linked.chat.Chat}; an unknown chat is
     * reported as {@link MutationApplicationResult#orphan(String, String)}
     * with {@code modelType="Chat"}.
     *
     * @implNote
     * This implementation skips WA Web's {@code validateMessageRange} and the
     * {@code constructMessageRange + compareMessageRanges} chain because
     * Cobalt does not maintain active message ranges; the read-state change
     * is applied directly to the {@link com.github.auties00.cobalt.wire.linked.chat.Chat}.
     * Any thrown exception is mapped to {@link MutationApplicationResult#failed()}
     * mirroring WA Web's try/catch shape.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebMarkChatAsReadSync", exports = {"applyMutations", "validateSyncActionValue", "$MarkChatAsReadSync$p_3", "$MarkChatAsReadSync$p_1", "$MarkChatAsReadSync$p_2", "getMessageRange"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        try {
            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof MarkChatAsReadAction action)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "mark chat as read mutation has malformed action value");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var chatJidString = JSON.parseArray(mutation.index()).getString(1);
            if (chatJidString == null || chatJidString.isEmpty()) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            var chatJid = Jid.of(chatJidString);
            if (chatJid == null) {
                return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
            }

            var chat = client.store().chatStore().findChatByJid(chatJid);
            if (chat.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mark chat as read mutation orphaned, chat {0} not found", chatJid);
                return MutationApplicationResult.orphan(chatJidString, "Chat");
            }

            if (action.read()) {
                chat.get().setMarkedAsUnread(false);
                chat.get().setUnreadCount(0);
            } else {
                chat.get().setMarkedAsUnread(true);
                chat.get().setUnreadCount(-1);
            }

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat read state applied, chat={0} read={1}", chatJid, action.read());
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "mark chat as read mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Both sides are decoded as {@link MarkChatAsReadAction} and the
     * four-way enclosure decision is delegated to
     * {@link MessageRangeUtils#compareMessageRanges(SyncActionMessageRange, SyncActionMessageRange)}.
     * When neither range encloses the other a merged action is built with the
     * {@link MarkChatAsReadAction#read()} flag drawn from the more-recent
     * mutation and returned via
     * {@link ConflictResolution#merged(DecryptedMutation.Trusted)} for the
     * caller to apply. A {@code null} action or
     * {@link MarkChatAsReadAction#messageRange()} on either side defaults to
     * {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL}.
     *
     * @implNote
     * This implementation separates resolution from application; WA Web
     * instead applies the merged mutation immediately under
     * {@code lockForMessageRangeSync}. The {@code null}-action default
     * replaces WA Web's {@code WANullthrows}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebMarkChatAsReadSync", exports = "resolveConflicts", adaptation = WhatsAppAdaptation.ADAPTED)
    public ConflictResolution resolveConflicts(DecryptedMutation.Trusted localMutation, DecryptedMutation.Trusted remoteMutation) {
        var localAction = localMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof MarkChatAsReadAction)
                .map(a -> (MarkChatAsReadAction) a)
                .orElse(null);
        var remoteAction = remoteMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof MarkChatAsReadAction)
                .map(a -> (MarkChatAsReadAction) a)
                .orElse(null);

        if (localAction == null || remoteAction == null) {
            return ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
        }

        var localRange = localAction.messageRange().orElse(null);
        var remoteRange = remoteAction.messageRange().orElse(null);

        if (localRange == null || remoteRange == null) {
            return ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
        }

        var comparison = MessageRangeUtils.compareMessageRanges(remoteRange, localRange);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mark chat as read conflict comparison={0}", comparison);
        return switch (comparison) {
            case RANGE_A_ENCLOSES_RANGE_B ->
                    ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
            case RANGE_B_ENCLOSES_RANGE_A ->
                    ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE);
            case RANGES_ARE_EQUAL ->
                    localMutation.timestamp().compareTo(remoteMutation.timestamp()) <= 0
                            ? ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL)
                            : ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE);
            case RANGES_NOT_ENCLOSING -> {
                var localWins = localMutation.timestamp().compareTo(remoteMutation.timestamp()) > 0;
                var read = localWins ? localAction.read() : remoteAction.read();
                var mergedRange = MessageRangeUtils.mergeMessageRanges(remoteRange, localRange);
                var mergedAction = new MarkChatAsReadActionBuilder()
                        .read(read)
                        .messageRange(mergedRange)
                        .build();
                var mergedValue = new SyncActionValueBuilder()
                        .timestamp(remoteMutation.timestamp())
                        .markChatAsReadAction(mergedAction)
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
