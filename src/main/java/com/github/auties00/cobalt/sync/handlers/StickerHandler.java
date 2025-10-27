package com.github.auties00.cobalt.sync.handlers;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.core.sync.DecryptedMutation;
import com.github.auties00.cobalt.model.proto.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

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
    public boolean applyMutation(
            WhatsappStore store,
            DecryptedMutation.Trusted mutation
    ) {

        // Extract sticker hash from index
        var indexArray = JSON.parseArray(mutation.index());
        var stickerHash = indexArray.getString(1);

        // Apply the action
        if (mutation.operation() == RecordSync.Operation.SET) {
            // Track sticker usage
            store.trackStickerUsage(stickerHash);
        } else {
            // REMOVE operation - remove sticker tracking
            store.removeStickerTracking(stickerHash);
        }

        return true;


    }
}
