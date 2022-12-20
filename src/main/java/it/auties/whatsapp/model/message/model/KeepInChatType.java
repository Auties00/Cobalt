package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@ProtobufName("KeepType")
public enum KeepInChatType
        implements ProtobufMessage {
    UNKNOWN(0),
    KEEP_FOR_ALL(1),
    UNDO_KEEP_FOR_ALL(2);

    @Getter
    private final int index;
}
