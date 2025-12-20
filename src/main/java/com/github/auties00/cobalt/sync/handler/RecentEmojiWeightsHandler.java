package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

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
    public boolean applyMutation(WhatsAppClient client, DecryptedMutation.Trusted mutation) {
        // Not handled
        return true;
    }
}
