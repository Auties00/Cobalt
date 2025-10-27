package com.github.auties00.cobalt.sync.handlers;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.core.sync.DecryptedMutation;
import com.github.auties00.cobalt.model.proto.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

/**
 * Handles favorite sticker actions.
 *
 * <p>This handler processes mutations that add or remove stickers from favorites.
 *
 * <p>Index format: ["favoriteStickerAction", "stickerHash"]
 */
public final class FavoriteStickerHandler implements WebAppStateActionHandler {
    public static final FavoriteStickerHandler INSTANCE = new FavoriteStickerHandler();

    private FavoriteStickerHandler() {

    }

    @Override
    public String actionName() {
        return "favoriteStickerAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var indexArray = JSON.parseArray(mutation.index());
        var stickerHash = indexArray.getString(1);

        if (mutation.operation() == RecordSync.Operation.SET) {
            store.addFavoriteSticker(stickerHash);
        } else {
            store.removeFavoriteSticker(stickerHash);
        }

        return true;
    }
}
