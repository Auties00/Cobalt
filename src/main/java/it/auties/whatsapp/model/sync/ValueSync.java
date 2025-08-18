package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "SyncdValue")
public record ValueSync(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] blob
) {

}
