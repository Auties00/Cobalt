package it.auties.whatsapp.model.chat;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.Contact;

/**
 * The constants of this enumerated type describe the various actions that can be executed on a
 * {@link Contact} in a {@link Chat}. Said chat should be a group: {@link Chat#isGroup()}. Said
 * actions can be executed using various methods in {@link Whatsapp}.
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
     * @return a lowercase non-null String
     */
    public String data() {
        return name().toLowerCase();
    }
}
