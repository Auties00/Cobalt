package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessageName("ServerErrorReceipt")
public record ServerErrorReceipt(
        @ProtobufProperty(index = 1, type = STRING) String stanzaId
) implements ProtobufMessage {

}
