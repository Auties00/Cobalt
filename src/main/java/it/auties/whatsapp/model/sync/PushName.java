package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import java.util.Optional;

import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessageName("Pushname")
public record PushName(@ProtobufProperty(index = 1, type = STRING) String id,
                       @ProtobufProperty(index = 2, type = STRING) Optional<String> name) implements ProtobufMessage {
}
