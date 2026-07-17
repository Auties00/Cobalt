package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.UnarchiveChatsSetting;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors the "Keep chats archived" preference and applies the matching
 * side-effect to every previously archived chat.
 *
 * <p>The sync dispatcher routes incoming {@code setting_unarchiveChats}
 * mutations here whenever the user toggles "Keep chats archived" on another
 * linked device. When the setting flips to "do not keep archived", the handler
 * unarchives archived chats that have new messages waiting; when it flips back,
 * the handler re-archives previously-archived chats per their stored
 * {@link ArchiveChatAction} entries. The boolean preference itself is persisted
 * on
 * {@link LinkedWhatsAppSettingsStore#setUnarchiveChats(boolean)}.
 */
@WhatsAppWebModule(moduleName = "WAWebArchiveSettingSync")
public final class UnarchiveChatsSettingHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link UnarchiveChatsSettingHandler}.
     */
    private static final System.Logger LOGGER = Log.get(UnarchiveChatsSettingHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single
     * instance per client.
     */
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public UnarchiveChatsSettingHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return UnarchiveChatsSetting.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return UnarchiveChatsSetting.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return UnarchiveChatsSetting.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only the final mutation in the batch is applied. Every earlier mutation
     * is acknowledged as {@link MutationApplicationResult#skipped()} so the
     * dispatcher can still correlate the result list with the input by position;
     * the trailing mutation runs through
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)}. An empty
     * batch returns an empty list.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        if (mutations.isEmpty()) {
            return List.of();
        }

        var results = new ArrayList<MutationApplicationResult>(mutations.size());
        for (var i = 0; i < mutations.size() - 1; i++) {
            results.add(MutationApplicationResult.skipped());
        }
        results.add(applyMutation(client, mutations.getLast()));
        return results;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A non-{@link SyncdOperation#SET} operation is reported as
     * {@link MutationApplicationResult#unsupported()}. A value that does not decode
     * to a {@link UnarchiveChatsSetting} is reported as malformed; otherwise the
     * boolean preference (a missing nullable Boolean coalescing to {@code false})
     * is persisted via
     * {@link LinkedWhatsAppSettingsStore#setUnarchiveChats(boolean)}
     * and {@link #updateSideEffectOnChats(LinkedWhatsAppClient, boolean)} runs. Any
     * thrown exception maps to {@link MutationApplicationResult#failed()}.
     *
     * @implNote
     * WA Web also writes {@code archiveV2EnabledSetting = true} when unset and
     * fires {@code WAWebBackendApi.frontendFireAndForget("applyAppSetting", ...)};
     * Cobalt drops both because archive-v2 is always enabled in Cobalt and there
     * is no frontend to fire at.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        try {
            if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof UnarchiveChatsSetting setting)) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unarchive chats setting: malformed mutation value");
                return SyncdIndexUtils.malformedActionValue(collectionName().name());
            }

            var unarchiveChats = setting.unarchiveChats();
            client.store().settingsStore().setUnarchiveChats(unarchiveChats);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unarchive chats setting: set unarchiveChats={0}", unarchiveChats);
            updateSideEffectOnChats(client, unarchiveChats);
            return MutationApplicationResult.success();
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "unarchive chats setting mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * Dispatches the setting-change side-effect to the matching unarchive /
     * re-archive helper.
     *
     * <p>Runs after the new setting value has been persisted; the choice between
     * unarchiving newly-active chats and re-archiving previously archived chats is
     * determined solely by the setting value, delegating to
     * {@link #applyUnarchiveSideEffect(LinkedWhatsAppClient)} or
     * {@link #applyArchiveSideEffect(LinkedWhatsAppClient)}.
     *
     * @param client         the {@link LinkedWhatsAppClient} whose store is being updated
     * @param unarchiveChats the new setting value
     */
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    private void updateSideEffectOnChats(LinkedWhatsAppClient client, boolean unarchiveChats) {
        if (unarchiveChats) {
            applyUnarchiveSideEffect(client);
        } else {
            applyArchiveSideEffect(client);
        }
    }

    /**
     * Unarchives chats that have new activity once the user opts out of "Keep
     * chats archived".
     *
     * <p>Side-effect of switching the setting to {@code true}; iterates the stored
     * {@link ArchiveChatAction} entries and clears the archived flag on chats that
     * have a successful archive sync action with a recorded message range.
     *
     * @implNote
     * This implementation simplifies WA Web's {@code $ArchiveSettingSync$p_1},
     * which compares the current message range against the stored range and only
     * unarchives when the current range strictly encloses the stored one. That
     * comparison relies on WA Web's active-message-range IDB layer, which Cobalt
     * does not maintain; without that probe Cobalt conservatively unarchives every
     * archived chat with a successful archive entry, matching the user-facing intent
     * of the setting change.
     *
     * @param client the {@link LinkedWhatsAppClient} whose store is being updated
     */
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private void applyUnarchiveSideEffect(LinkedWhatsAppClient client) {
        // TODO: replicate WA Web's MessageRangeUtils.compareMessageRanges gating once Cobalt
        //       maintains active-message-range tracking; today Cobalt may unarchive chats
        //       that WA Web would have left archived.
        var archiveEntries = client.store().syncStore().getSyncActionEntries(SyncPatchType.REGULAR_LOW);
        var unarchivedCount = 0;
        for (var entry : archiveEntries) {
            if (entry.actionState() != SyncActionState.SUCCESS
                    && entry.actionState() != SyncActionState.ORPHAN) {
                continue;
            }

            var actionValue = entry.actionValue();
            if (actionValue == null) {
                continue;
            }

            var action = actionValue.action().orElse(null);
            if (!(action instanceof ArchiveChatAction archiveAction)) {
                continue;
            }

            if (!archiveAction.archived()) {
                continue;
            }

            if (archiveAction.messageRange().isEmpty()) {
                continue;
            }

            var actionIndex = entry.actionIndex();
            if (actionIndex == null) {
                continue;
            }

            var chatJid = extractChatJidFromArchiveIndex(actionIndex);
            if (chatJid == null) {
                continue;
            }

            var chat = client.store().chatStore().findChatByJid(chatJid);
            if (chat.isEmpty()) {
                continue;
            }

            if (!chat.get().archived()) {
                continue;
            }

            chat.get().setArchived(false);
            unarchivedCount++;
        }
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "unarchive chats setting: unarchived {0} chats", unarchivedCount);
    }

    /**
     * Re-archives chats that the user previously archived once the "Keep chats
     * archived" setting is turned back on.
     *
     * <p>Side-effect of switching the setting to {@code false}; replays stored
     * {@link ArchiveChatAction} entries with {@code archived = true} and a
     * successful action state.
     *
     * @implNote
     * This implementation inspects only the persisted archive entries, where WA
     * Web's {@code $ArchiveSettingSync$p_2} also merges the pending mutations
     * table; the practical effect is identical because the dispatcher applies
     * pending mutations to the store before invoking this handler.
     *
     * @param client the {@link LinkedWhatsAppClient} whose store is being updated
     */
    @WhatsAppWebExport(moduleName = "WAWebArchiveSettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private void applyArchiveSideEffect(LinkedWhatsAppClient client) {
        var archiveEntries = client.store().syncStore().getSyncActionEntries(SyncPatchType.REGULAR_LOW);
        var archivedCount = 0;
        for (var entry : archiveEntries) {
            if (entry.actionState() != SyncActionState.SUCCESS) {
                continue;
            }

            var actionValue = entry.actionValue();
            if (actionValue == null) {
                continue;
            }

            var action = actionValue.action().orElse(null);
            if (!(action instanceof ArchiveChatAction archiveAction)) {
                continue;
            }

            if (!archiveAction.archived()) {
                continue;
            }

            var actionIndex = entry.actionIndex();
            if (actionIndex == null) {
                continue;
            }

            var chatJid = extractChatJidFromArchiveIndex(actionIndex);
            if (chatJid == null) {
                continue;
            }

            var chat = client.store().chatStore().findChatByJid(chatJid);
            if (chat.isEmpty()) {
                continue;
            }

            chat.get().setArchived(true);
            archivedCount++;
        }
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "unarchive chats setting: re-archived {0} chats", archivedCount);
    }

    /**
     * Extracts the chat JID from a stored archive-action index.
     *
     * <p>The archive action index has the shape {@code ["archive", chatJidString]};
     * this helper parses the JSON array and returns the chat JID, defaulting to
     * {@code null} for a missing index, a missing JID slot, an empty JID string, or
     * a JID parse failure so callers can skip the entry instead of raising an
     * exception out of the side-effect helpers.
     *
     * @param actionIndex the JSON-encoded archive-action index
     * @return the parsed chat JID, or {@code null} on any failure
     */
    private Jid extractChatJidFromArchiveIndex(String actionIndex) {
        try {
            var parsed = JSON.parseArray(actionIndex);
            if (parsed == null || parsed.size() < 2) {
                return null;
            }
            var jidString = parsed.getString(1);
            if (jidString == null || jidString.isEmpty()) {
                return null;
            }
            return Jid.of(jidString);
        } catch (Exception e) {
            return null;
        }
    }

}
