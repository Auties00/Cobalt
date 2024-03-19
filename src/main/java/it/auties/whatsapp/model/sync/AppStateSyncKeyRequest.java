package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessageName("Message.AppStateSyncKeyRequest")
public record AppStateSyncKeyRequest(
        @ProtobufProperty(index = 1, type = OBJECT) List<AppStateSyncKeyId> keyIds) implements ProtobufMessage {
}
