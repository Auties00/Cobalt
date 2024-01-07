package it.auties.whatsapp.model.companion;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

public record CompanionPatch(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid companion,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        CompanionHashState state
) implements ProtobufMessage {

}
