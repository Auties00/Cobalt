package it.auties.whatsapp.model.chat;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a community
 */
public enum CommunitySetting {
    /**
     * Who can edit the metadata of a group
     */
    ADD_GROUPS("allow_non_admin_sub_group_creation", "not_allow_non_admin_sub_group_creation");

    private final String on;
    private final String off;

    CommunitySetting(String on, String off) {
        this.on = on;
        this.off = off;
    }

    public String on() {
        return this.on;
    }

    public String off() {
        return this.off;
    }
}
