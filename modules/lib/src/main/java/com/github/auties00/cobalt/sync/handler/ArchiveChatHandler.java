package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
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
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Flips the archive state of a chat in the local store in response to an {@code archive} sync mutation.
 *
 * <p>When a chat is archived or unarchived on another device, the server
 * replays the action here as an {@link ArchiveChatAction} and the per-chat
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat#archived()} flag is
 * updated.
 *
 * @implNote
 * This implementation drops WA Web's {@code activeMessageRanges} state
 * tracking and the {@code unarchive-chats-setting} gate that suppresses
 * unarchive when the user has opted out of read-receipt-driven
 * unarchive. Cobalt does not maintain active message ranges (a
 * browser-IndexedDB concern) and unconditionally applies the new
 * archive state to the chat row. Conflict resolution still mirrors WA
 * Web exactly via {@link MessageRangeUtils}.
 */
@WhatsAppWebModule(moduleName = "WAWebArchiveChatSync")
public final class ArchiveChatHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link ArchiveChatHandler}.
     */
    private static final System.Logger LOGGER = Log.get(ArchiveChatHandler.class);

    /**
     * Constructs the singleton archive-chat handler.
     *
     * <p>The sync handler registry instantiates this type exactly once.
     */
    @WhatsAppWebExport(moduleName = "WAWebArchiveChatSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public ArchiveChatHandler() {

    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveChatSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return ArchiveChatAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveChatSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return ArchiveChatAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveChatSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return ArchiveChatAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads {@link ArchiveChatAction#archived()} from the mutation value,
     * locates the target {@link com.github.auties00.cobalt.wire.linked.chat.Chat} by
     * JID, and writes the archive flag through
     * {@link com.github.auties00.cobalt.wire.linked.chat.Chat#setArchived(Boolean)}.
     * Returns {@link MutationApplicationResult#unsupported()} for non-{@link SyncdOperation#SET}
     * operations, an orphan result keyed by chat JID and model type {@code "Chat"}
     * when the chat is not in the store, malformed results when the index or
     * value is shaped wrong, and {@link MutationApplicationResult#failed()} on
     * any thrown exception.
     *
     * @implNote
     * This implementation skips WA Web's
     * {@code WAWebMessageRangeUtils.validateMessageRange} pre-check and
     * the {@code activeMessageRanges} bookkeeping; the archive flag is
     * written unconditionally once the chat is resolved. This matches
     * the Cobalt store model where chat archive state is a single
     * boolean rather than a message-range-keyed history.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveChatSync", exports = {"applyMutations", "validateSyncActionValue", "getMessageRange"}, adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        try {
            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof ArchiveChatAction action)) {
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
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "archive chat: orphan chat={0}", chatJid);
                return MutationApplicationResult.orphan(chatJidString, "Chat");
            }

            chat.get().setArchived(action.archived());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "archive chat: set archived={0} for chat={1}", action.archived(), chatJid);
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "archive chat mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the local-vs-remote tie by comparing the message ranges
     * carried inside each {@link ArchiveChatAction} via
     * {@link MessageRangeUtils#compareMessageRanges(SyncActionMessageRange, SyncActionMessageRange)}.
     * Returns {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL} when the
     * remote range encloses the local one,
     * {@link MutationConflictResolutionState#SKIP_REMOTE} when the local range encloses
     * the remote one, a timestamp tiebreaker when ranges are equal, and a merged
     * {@link ConflictResolution} (combining both ranges and the newer side's
     * {@link ArchiveChatAction#archived()} flag) when ranges partially overlap.
     *
     * @implNote
     * This implementation returns the merged mutation for the caller to
     * apply rather than applying it inline. WA Web's
     * {@code lockForMessageRangeSync} writes the merged state to the
     * chat DB during conflict resolution; Cobalt separates resolution
     * from application so the caller controls when the write lands. A
     * defensive {@link MutationConflictResolutionState#APPLY_REMOTE_DROP_LOCAL}
     * is returned when either side lacks a {@link ArchiveChatAction} or
     * a message range, replacing WA Web's
     * {@code nullthrows} which would throw.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveChatSync", exports = "resolveConflicts", adaptation = WhatsAppAdaptation.ADAPTED)
    public ConflictResolution resolveConflicts(DecryptedMutation.Trusted localMutation, DecryptedMutation.Trusted remoteMutation) {
        var localAction = localMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof ArchiveChatAction)
                .map(a -> (ArchiveChatAction) a)
                .orElse(null);
        var remoteAction = remoteMutation.value().flatMap(sav -> sav.action())
                .filter(a -> a instanceof ArchiveChatAction)
                .map(a -> (ArchiveChatAction) a)
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
                var localWins = localMutation.timestamp().compareTo(remoteMutation.timestamp()) > 0;
                var archived = localWins ? localAction.archived() : remoteAction.archived();
                var mergedRange = MessageRangeUtils.mergeMessageRanges(remoteRange, localRange);
                var mergedAction = new ArchiveChatActionBuilder()
                        .archived(archived)
                        .messageRange(mergedRange)
                        .build();
                var mergedValue = new SyncActionValueBuilder()
                        .timestamp(remoteMutation.timestamp())
                        .archiveChatAction(mergedAction)
                        .build();
                var merged = new DecryptedMutation.Trusted(
                        localMutation.index(),
                        mergedValue,
                        localMutation.operation(),
                        localMutation.timestamp(),
                        localMutation.actionVersion()
                );
                if (Log.DEBUG)
                    LOGGER.log(Level.DEBUG, "archive chat: merged conflicting mutations, archived={0}, localWins={1}", archived, localWins);
                yield ConflictResolution.merged(merged);
            }
        };
    }

}
