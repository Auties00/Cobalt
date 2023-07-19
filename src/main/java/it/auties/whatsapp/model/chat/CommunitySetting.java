package it.auties.whatsapp.model.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a community
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum CommunitySetting {
    /**
     * Who can edit the metadata of a group
     */
    ADD_GROUPS("allow_non_admin_sub_group_creation", "not_allow_non_admin_sub_group_creation");

    /**
     * The name of the setting when enabled
     */
    @Getter
    private final String on;

    /**
     * The name of the setting when disabled
     */
    @Getter
    private final String off;
}
