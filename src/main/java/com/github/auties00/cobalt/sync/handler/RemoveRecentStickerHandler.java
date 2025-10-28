package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles remove recent sticker actions.
 *
 * <p>This handler processes mutations that remove stickers from the recent list.
 *
 * <p>Index format: ["removeRecentStickerAction", "stickerHash"]
 */
public final class RemoveRecentStickerHandler implements WebAppStateActionHandler {

    public static final RemoveRecentStickerHandler INSTANCE = new RemoveRecentStickerHandler();

    private RemoveRecentStickerHandler() {
    }

    @Override
    public String actionName() {
        return "removeRecentStickerAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var indexArray = JSON.parseArray(mutation.index());
        var stickerHash = indexArray.getString(1);

        store.removeRecentSticker(stickerHash);

        return true;
    }
}
