package com.github.auties00.cobalt.model.chat;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.model.contact.Contact;
import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * The constants of this enumerated type describe the various actions that can be executed on a
 * {@link Contact} in a {@link Chat}. Said chat should be a group: {@link Chat#isGroupOrCommunity()}. Said
 * actions can be executed using various methods in {@link Whatsapp}.
 */
@ProtobufEnum
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
