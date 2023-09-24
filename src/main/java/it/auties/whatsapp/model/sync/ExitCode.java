package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.STRING;
import static it.auties.protobuf.model.ProtobufType.UINT64;

@ProtobufMessageName("ExitCode")
public record ExitCode(@ProtobufProperty(index = 1, type = UINT64) long code,
                       @ProtobufProperty(index = 2, type = STRING) String text) implements ProtobufMessage {
}
