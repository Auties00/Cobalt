package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessage(name = "ServerErrorReceipt")
public record ServerErrorReceipt(
        @ProtobufProperty(index = 1, type = STRING) String stanzaId
) {

}
