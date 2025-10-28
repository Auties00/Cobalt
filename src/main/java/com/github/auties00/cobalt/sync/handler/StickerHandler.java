package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles sticker actions.
 *
 * <p>This handler processes mutations related to sticker packs and usage.
 *
 * <p>Index format: ["stickerAction", "stickerHash"]
 */
public final class StickerHandler implements WebAppStateActionHandler {
    public static final StickerHandler INSTANCE = new StickerHandler();

    private StickerHandler() {

    }

    @Override
    public String actionName() {
        return "stickerAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .stickerAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing stickerAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var stickerHash = indexArray.getString(1);

        switch (mutation.operation()) {
            case SET -> store.addRecentSticker(stickerHash, action);
            case REMOVE -> store.removeRecentSticker(stickerHash);
        }

        return true;
    }
}
