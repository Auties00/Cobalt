package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.INT64;
import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessageName("Message.AppStateFatalExceptionNotification")
public record AppStateFatalExceptionNotification(
        @ProtobufProperty(index = 1, type = STRING) List<String> collectionNames,
        @ProtobufProperty(index = 2, type = INT64) Long timestamp) implements ProtobufMessage {
}
