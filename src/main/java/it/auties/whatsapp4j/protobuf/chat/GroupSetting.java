package it.auties.whatsapp4j.protobuf.chat;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a group.
 * Said settings can be changed using various methods in {@link WhatsappAPI}.
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum GroupSetting {
    /**
     * Who can edit the metadata of a group
     */
    EDIT_GROUP_INFO("locked"),

    /**
     * Who can send messages in a group
     */
    SEND_MESSAGES("announcement");

    /**
     * The name of the setting linked to this enumerated constant
     */
    private final @Getter String data;
}
