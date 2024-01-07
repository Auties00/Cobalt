package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.sync.AppStateSyncKey;

import java.util.LinkedList;

public record CompanionSyncKey(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid companion,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        LinkedList<AppStateSyncKey> keys
) implements ProtobufMessage {

}
