package com.github.auties00.cobalt.wire.linked.sync.action.bot;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that identify the bot chat targeted by a
 * {@link BotWelcomeRequestAction}.
 *
 * <p>Every sync action is stored under a composite index that uniquely
 * describes the entity it mutates. For bot welcome bookkeeping the index is
 * the pair {@code ["bot_welcome_request", chatJid]}, where {@code chatJid}
 * selects the specific bot conversation whose welcome delivery status is
 * being tracked. This ensures that marking one bot chat's welcome as sent
 * does not interfere with the welcome tracking of any other bot chat.
 *
 * @param chatJid the {@link Jid} of the bot chat whose welcome status this
 *                action records
 */
public record BotWelcomeRequestActionArgs(Jid chatJid) implements SyncActionArgs {
    /**
     * Returns the positional index arguments used to key this action in the
     * sync patch storage.
     *
     * @return a single-element array containing the chat JID string
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{chatJid.toString()};
    }
}
