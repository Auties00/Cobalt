package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for a {@link ClearChatAction}.
 *
 * <p>Clear-chat operations are uniquely identified by the target chat plus
 * the user's choice of whether starred messages and media attachments should
 * also be removed. All three values participate in the mutation index so
 * that variants of the same logical operation do not collide.
 *
 * <p>Boolean flags are encoded as {@code "1"} for {@code true} and
 * {@code "0"} for {@code false}. The full encoded index is
 * {@code ["clearChat", chatJid, deleteStarred, deleteMedia]}.
 *
 * @param chatJid       the {@link Jid} of the chat being cleared
 * @param deleteStarred {@code true} to also delete starred messages
 * @param deleteMedia   {@code true} to also delete media files
 */
public record ClearChatActionArgs(Jid chatJid, boolean deleteStarred, boolean deleteMedia) implements SyncActionArgs {
    /**
     * Converts this record into the tail portion of the sync index array.
     *
     * @return a three-element array containing the chat JID string and the
     *         two boolean flags encoded as {@code "1"} or {@code "0"}
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{chatJid.toString(), deleteStarred ? "1" : "0", deleteMedia ? "1" : "0"};
    }
}
