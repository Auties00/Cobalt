package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import java.util.Optional;

import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessage(name = "Pushname")
public record PushName(@ProtobufProperty(index = 1, type = STRING) String id,
                       @ProtobufProperty(index = 2, type = STRING) Optional<String> name) {
}
