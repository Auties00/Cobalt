package it.auties.whatsapp.model.chat;

import it.auties.whatsapp.api.Whatsapp;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a
 * group. Said settings can be changed using various methods in {@link Whatsapp}.
 */
public enum GroupSetting {
    /**
     * Who can edit the metadata of a group
     */
    EDIT_GROUP_INFO("locked", "unlocked"),

    /**
     * Who can send messages in a group
     */
    SEND_MESSAGES("announcement", "not_announcement"),

    /**
     * Who can add new members to the community
     */
    APPROVE_NEW_PARTICIPANTS("membership_approval_mode", "membership_approval_mode");

    private final String on;
    private final String off;

    GroupSetting(String on, String off) {
        this.on = on;
        this.off = off;
    }

    public String on() {
        return on;
    }

    public String off() {
        return off;
    }
}
