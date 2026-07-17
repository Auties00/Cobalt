package com.github.auties00.cobalt.wire.linked.sync.action.chat;


import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments for a {@link DeleteChatAction}.
 *
 * <p>Delete-chat operations are keyed by the chat JID and by whether media
 * files attached to the chat should be removed as well. The flag is encoded
 * as {@code "1"} for {@code true} and {@code "0"} for {@code false}.
 *
 * <p>The encoded index is {@code ["deleteChat", chatJid, deleteMediaFiles]}.
 *
 * @param chatJid          the {@link Jid} of the chat being deleted
 * @param deleteMediaFiles {@code true} to also delete associated media files
 */
public record DeleteChatActionArgs(Jid chatJid, boolean deleteMediaFiles) implements SyncActionArgs {
    /**
     * Converts this record into the tail portion of the sync index array.
     *
     * @return a two-element array containing the chat JID string and the
     *         media deletion flag encoded as {@code "1"} or {@code "0"}
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{chatJid.toString(), deleteMediaFiles ? "1" : "0"};
    }
}
