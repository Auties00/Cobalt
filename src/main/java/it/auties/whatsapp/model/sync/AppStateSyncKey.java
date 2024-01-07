package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessageName("Message.AppStateSyncKey")
public record AppStateSyncKey(
        @ProtobufProperty(index = 1, type = OBJECT) AppStateSyncKeyId keyId,
        @ProtobufProperty(index = 2, type = OBJECT) AppStateSyncKeyData keyData
) implements ProtobufMessage {

}
