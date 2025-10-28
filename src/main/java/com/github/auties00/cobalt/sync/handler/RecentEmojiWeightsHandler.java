package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles recent emoji weights actions.
 *
 * <p>This handler processes mutations that track frequently used emojis and their weights.
 */
public final class RecentEmojiWeightsHandler implements WebAppStateActionHandler {

    public static final RecentEmojiWeightsHandler INSTANCE = new RecentEmojiWeightsHandler();

    private RecentEmojiWeightsHandler() {
    }

    @Override
    public String actionName() {
        return "recentEmojiWeightsAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .recentEmojiWeightsAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing recentEmojiWeightsAction"));

        // Update recent emoji weights in the store
        action.weights().ifPresent(weights -> {
            weights.forEach(weight -> {
                weight.emoji().ifPresent(emoji -> {
                    weight.weight().ifPresent(w -> {
                        store.updateEmojiWeight(emoji, w);
                    });
                });
            });
        });

        return true;
    }
}
