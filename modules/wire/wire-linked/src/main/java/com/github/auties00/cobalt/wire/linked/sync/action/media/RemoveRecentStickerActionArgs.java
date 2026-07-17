package com.github.auties00.cobalt.wire.linked.sync.action.media;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Sync index arguments that identify which sticker is being removed from the
 * recent-stickers collection by a {@link RemoveRecentStickerAction}.
 *
 * <p>The sticker is addressed by its file hash so that the removal applies to
 * the exact sticker binary regardless of where it was originally received.
 *
 * @param stickerFileHash the file hash of the sticker to remove from the recent list
 */
public record RemoveRecentStickerActionArgs(String stickerFileHash) implements SyncActionArgs {
    /**
     * Returns the index arguments used to address this sticker inside the
     * app-state sync collection.
     *
     * @return a single-element array containing the sticker file hash
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{stickerFileHash};
    }
}
