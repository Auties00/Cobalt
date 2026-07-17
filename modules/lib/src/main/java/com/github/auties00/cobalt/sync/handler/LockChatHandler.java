package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.LockChatAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code lock} app-state sync action that locks or unlocks a chat
 * across the user's linked devices.
 *
 * <p>The bit fans out across the {@link SyncPatchType#REGULAR_LOW} collection.
 * Locking forcibly clears the chat's archive and pin states so the three
 * sticky-chat states remain mutually consistent. The mutation index keys each
 * entry by the chat JID, formatted as
 * {@snippet :
 *     ["lock", chatJid]
 * }
 *
 * @implNote
 * This implementation writes the lock state directly on the in-memory
 * {@link com.github.auties00.cobalt.wire.linked.chat.Chat}. Because the
 * {@link LockChatAction#locked()} accessor coalesces a {@code null} protobuf
 * field to {@code false}, a missing flag is treated as an unlock rather than as
 * {@link MutationApplicationResult#malformed()}.
 */
@WhatsAppWebModule(moduleName = "WAWebLockChatSync")
public final class LockChatHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link LockChatHandler}.
     */
    private static final System.Logger LOGGER = Log.get(LockChatHandler.class);

    /**
     * Constructs a new singleton {@link LockChatHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebLockChatSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public LockChatHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLockChatSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return LockChatAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLockChatSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return LockChatAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLockChatSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return LockChatAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rejects non-{@link SyncdOperation#SET} operations as
     * {@link MutationApplicationResult#unsupported()}, an absent action payload
     * or missing chat JID as malformed, and an absent chat as
     * {@link MutationApplicationResult#orphan(String, String)} with model type
     * {@code "Chat"}. Otherwise the chat's lock state is set from
     * {@link LockChatAction#locked()}; when locking, the chat is additionally
     * marked as not archived and not pinned so the sticky-chat invariant is
     * preserved.
     *
     * @implNote
     * This implementation mutates the {@link com.github.auties00.cobalt.wire.linked.chat.Chat}
     * in place in a single pass rather than collecting then writing, and maps a
     * failed {@link Jid#of(String)} to {@link MutationApplicationResult#malformed()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLockChatSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof LockChatAction action)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "lock chat mutation has malformed action value");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var chatJidString = JSON.parseArray(mutation.index()).getString(1);
        if (chatJidString == null || chatJidString.isEmpty()) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        Jid chatJid;
        try {
            chatJid = Jid.of(chatJidString);
        } catch (Exception e) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var chat = client.store().chatStore().findChatByJid(chatJid);
        if (chat.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "lock chat mutation orphaned, chat {0} not found", chatJid);
            return MutationApplicationResult.orphan(chatJidString, "Chat");
        }

        chat.get().setLocked(action.locked());
        if (action.locked()) {
            chat.get().setArchived(false);
            chat.get().setPinnedTimestamp(null);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat lock state applied, chat={0} locked={1}", chatJid, action.locked());
        return MutationApplicationResult.success();
    }

}
