package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessage(name = "Message.AppStateSyncKey")
public record AppStateSyncKey(
        @ProtobufProperty(index = 1, type = OBJECT) AppStateSyncKeyId keyId,
        @ProtobufProperty(index = 2, type = OBJECT) AppStateSyncKeyData keyData
) {

}
