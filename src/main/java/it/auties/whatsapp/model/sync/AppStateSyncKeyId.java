package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.BYTES;

@ProtobufMessage(name = "Message.AppStateSyncKeyId")
public record AppStateSyncKeyId(
        @ProtobufProperty(index = 1, type = BYTES) byte[] keyId
) {

}
