package it.auties.whatsapp.model.chat;

import it.auties.whatsapp.api.Whatsapp;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a
 * group. Said settings can be changed using various methods in {@link Whatsapp}.
 */
public enum GroupSetting implements ChatSetting {
    /**
     * Who can edit the metadata of a group
     */
    EDIT_GROUP_INFO,

    /**
     * Who can send messages in a group
     */
    SEND_MESSAGES,

    /**
     * Who can add new members
     */
    ADD_PARTICIPANTS,

    /**
     * Who can accept new members
     */
    APPROVE_PARTICIPANTS
}
