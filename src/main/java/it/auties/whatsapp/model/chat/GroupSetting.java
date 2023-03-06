package it.auties.whatsapp.model.chat;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.api.Whatsapp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a
 * group. Said settings can be changed using various methods in {@link Whatsapp}.
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum GroupSetting implements ProtobufMessage {
    /**
     * Who can edit the metadata of a group
     */
    EDIT_GROUP_INFO("locked"),

    /**
     * Who can send messages in a group
     */
    SEND_MESSAGES("announcement"),

    /**
     * Who can add new members to the community
     */
    ADD_COMMUNITY_MEMBER("member_add_mode");

    /**
     * The name of the setting linked to this enumerated constant
     */
    @Getter
    private final String data;
}
