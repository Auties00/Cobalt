package com.github.auties00.cobalt.wire.linked.sync.action.media;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Sync index arguments that identify which sticker is being favorited or
 * unfavorited by a {@link StickerAction}.
 *
 * <p>Favorite-sticker entries are keyed by the sticker's file hash so that the
 * same sticker is always addressed by the same index regardless of the source
 * message it was received from.
 *
 * @param stickerFileHash the file hash of the sticker being favorited or unfavorited
 */
public record StickerActionArgs(String stickerFileHash) implements SyncActionArgs {
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
