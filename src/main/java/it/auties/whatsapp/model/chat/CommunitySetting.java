package it.auties.whatsapp.model.chat;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a community
 */
public enum CommunitySetting implements ChatSetting {
    /**
     * Who can add/remove groups to/from a community
     */
    MODIFY_GROUPS,
    /**
     * Who can add/remove participants to/from a community
     */
    ADD_PARTICIPANTS
}
