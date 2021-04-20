package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import jakarta.validation.constraints.NotNull;


/**
 * The constants of this enumerated type describe the various actions that can be executed on a {@link it.auties.whatsapp4j.model.WhatsappContact} in a {@link it.auties.whatsapp4j.model.WhatsappChat}.
 * Said chat should be a group: {@link WhatsappChat#isGroup()}.
 * Said actions can be executed using various methods in {@link WhatsappAPI}.
 */
public enum WhatsappGroupAction {
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
    public @NotNull String data() {
        return name().toLowerCase();
    }
}
