package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

public record PrimaryFeature(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING, repeated = true)
        List<String> flags
) implements ProtobufMessage {

}
