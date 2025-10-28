package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.chat.ChatMute;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.RecordSync;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles mute chat actions.
 *
 * <p>This handler processes mutations that mute or unmute chat notifications.
 *
 * <p>Index format: ["muteAction", "chatJid"]
 */
public final class MuteChatHandler implements WebAppStateActionHandler {
    public static final MuteChatHandler INSTANCE = new MuteChatHandler();

    private MuteChatHandler() {

    }

    @Override
    public String actionName() {
        return "muteAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .muteAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing muteAction"));

        var chatJidString = JSON.parseArray(mutation.index())
                .getString(1);
        var chatJid = Jid.of(chatJidString);

        var chat = store.findChatByJid(chatJid);
        if (chat.isEmpty()) {
            return false;
        }

        if (mutation.operation() == RecordSync.Operation.SET) {
            chat.get().setMute(ChatMute.muted(action.muteEndTimestampSeconds()));
        } else {
            chat.get().setMute(ChatMute.notMuted());
        }

        return true;
    }
}
