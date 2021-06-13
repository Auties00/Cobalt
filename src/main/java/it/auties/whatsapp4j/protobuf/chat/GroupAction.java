package it.auties.whatsapp4j.protobuf.chat;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.NonNull;

/**
 * The constants of this enumerated type describe the various actions that can be executed on a {@link it.auties.whatsapp4j.protobuf.contact.Contact} in a {@link Chat}.
 * Said chat should be a group: {@link Chat#isGroup()}.
 * Said actions can be executed using various methods in {@link WhatsappAPI}.
 */
public enum GroupAction {
    /**
     * Adds a contact to a group
     */
    ADD,

    /**
     * Removes a contact from a group
     */
    REMOVE,

    /**
     * Promotes a contact to admin in a group
     */
    PROMOTE,

    /**
     * Demotes a contact to user in a group
     */
    DEMOTE;

    /**
     * Returns the name of this enumerated constant
     *
     * @return a lowercase non null String
     */
    public @NonNull String data() {
        return name().toLowerCase();
    }
}
