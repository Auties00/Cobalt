package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.MESSAGE;

@ProtobufMessage(name = "Message.AppStateSyncKey")
public record AppStateSyncKey(
        @ProtobufProperty(index = 1, type = MESSAGE) AppStateSyncKeyId keyId,
        @ProtobufProperty(index = 2, type = MESSAGE) AppStateSyncKeyData keyData
) {

}
