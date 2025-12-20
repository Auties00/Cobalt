package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

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
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .favoriteStickerAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing favoriteStickerAction"));
        var indexArray = JSON.parseArray(mutation.index());
        var stickerHash = indexArray.getString(1);

        switch (mutation.operation()) {
            case SET -> client.store()
                    .addFavouriteSticker(stickerHash, action.toSticker());
            case REMOVE -> client.store()
                    .removeFavouriteSticker(stickerHash);
        }

        return true;
    }
}
