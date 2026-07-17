package com.github.auties00.cobalt.wire.linked.sync.action.bot;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that identify the target of an {@link AiThreadRenameAction}.
 *
 * <p>Every sync action is stored under a composite index that uniquely
 * describes the entity it mutates. For AI thread renames the index is the
 * triple {@code ["ai_thread_rename", botJid, threadId]}, where {@code botJid}
 * selects the AI bot owning the conversation and {@code threadId} selects
 * one specific thread within that bot's history. This ensures that a rename
 * applied to one thread does not overwrite the title of another thread
 * belonging to the same bot.
 *
 * @param botJid   the {@link Jid} of the AI bot that owns the thread
 * @param threadId the server-assigned identifier of the AI thread being
 *                 renamed
 */
public record AiThreadRenameActionArgs(Jid botJid, String threadId) implements SyncActionArgs {
    /**
     * Returns the positional index arguments used to key this action in the
     * sync patch storage.
     *
     * @return a two-element array containing the bot JID followed by the
     *         thread identifier
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{botJid.toString(), threadId};
    }
}
