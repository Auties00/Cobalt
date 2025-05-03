package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The constants of this enumerated type describe the various settings that can be toggled for a community
 */
@ProtobufEnum
public enum CommunitySetting implements ChatSetting {
    /**
     * Who can add/remove groups to/from a community
     */
    MODIFY_GROUPS(20),
    /**
     * Who can add/remove participants to/from a community
     */
    ADD_PARTICIPANTS(21);

    final int index;

    CommunitySetting(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    @Override
    public int index() {
        return index;
    }
}
