package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessageName("Message.AppStateSyncKeyShare")
public record AppStateSyncKeyShare(
        @ProtobufProperty(index = 1, type = OBJECT) List<AppStateSyncKey> keys) implements ProtobufMessage {
}
