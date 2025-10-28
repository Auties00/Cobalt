package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.store.WhatsappStore;

/**
 * Handles user status mute actions.
 *
 * <p>This handler processes mutations that mute or unmute a contact's status updates.
 *
 * <p>Index format: ["userStatusMuteAction", "userJid"]
 */
public final class UserStatusMuteHandler implements WebAppStateActionHandler {
    public static final UserStatusMuteHandler INSTANCE = new UserStatusMuteHandler();

    private UserStatusMuteHandler() {

    }

    @Override
    public String actionName() {
        return "userStatusMuteAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .userStatusMuteAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing userStatusMuteAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var userJidString = indexArray.getString(1);
        var userJid = Jid.of(userJidString);

        var contact = store.findContactByJid(userJid)
                .orElseGet(() -> store.addNewContact(userJid));

        switch (mutation.operation()) {
            case SET -> contact.setStatusMuted(action.muted());
            case REMOVE -> contact.setStatusMuted(false);
        }

        return true;
    }
}
