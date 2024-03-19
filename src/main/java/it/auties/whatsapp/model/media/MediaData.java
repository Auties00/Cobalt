package it.auties.whatsapp.model.media;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;


@ProtobufMessageName("MediaData")
public record MediaData(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String localPath
) implements ProtobufMessage {

}