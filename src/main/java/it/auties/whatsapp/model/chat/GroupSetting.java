package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.whatsapp.api.Whatsapp;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a
 * group. Said settings can be changed using various methods in {@link Whatsapp}.
 */
@ProtobufEnum
public enum GroupSetting implements ChatSetting {
    /**
     * Who can edit the metadata of a group
     */
    EDIT_GROUP_INFO(0),

    /**
     * Who can send messages in a group
     */
    SEND_MESSAGES(1),

    /**
     * Who can add new members
     */
    ADD_PARTICIPANTS(2),

    /**
     * Who can accept new members
     */
    APPROVE_PARTICIPANTS(3);

    final int index;

    GroupSetting(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    @Override
    public int index() {
        return index;
    }
}
