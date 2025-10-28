package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

/**
 * Handles contact actions.
 *
 * <p>This handler processes mutations that update contact information
 * (names, profile pictures, etc.).
 *
 * <p>Index format: ["contact", "contactJid"]
 */
public final class ContactActionHandler implements WebAppStateActionHandler {
    public static final ContactActionHandler INSTANCE = new ContactActionHandler();

    private ContactActionHandler() {

    }

    @Override
    public String actionName() {
        return "contact";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value().contactAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing contactAction"));

        var indexArray = JSON.parseArray(mutation.index());
        var contactJidString = indexArray.getString(1);
        var contactJid = Jid.of(contactJidString);

        var contact = store.findContactByJid(contactJid)
                .orElseGet(() -> store.addNewContact(contactJid));

        switch (mutation.operation()) {
            case SET -> {
                action.fullName().ifPresent(contact::setFullName);
                action.firstName().ifPresent(contact::setShortName);
            }
            case REMOVE -> {
                contact.setFullName(null);
                contact.setShortName(null);
            }
        }

        return true;
    }
}
